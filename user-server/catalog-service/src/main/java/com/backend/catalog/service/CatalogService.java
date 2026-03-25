package com.backend.catalog.service;

import com.backend.catalog.api.dto.BranchSettingRequest;
import com.backend.catalog.api.dto.CategoryRequest;
import com.backend.catalog.api.dto.DrugAdminDto;
import com.backend.catalog.api.dto.DrugPublicDto;
import com.backend.catalog.api.dto.DrugRequest;
import com.backend.catalog.cache.CacheConstants;
import com.backend.catalog.cache.CacheHelper;
import com.backend.catalog.cache.CacheInvalidationEvent;
import com.backend.catalog.cache.CacheKeyBuilder;
import com.backend.common.messaging.EventTypes;
import com.backend.common.model.EventEnvelope;
import com.backend.catalog.model.Category;
import com.backend.catalog.model.Drug;
import com.backend.catalog.model.DrugBranchSetting;
import com.backend.catalog.repo.CategoryRepository;
import com.backend.catalog.repo.DrugBranchSettingRepository;
import com.backend.catalog.repo.DrugRepository;
import com.backend.catalog.repo.DrugWithBranchView;
import jakarta.transaction.Transactional;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {
    private final CategoryRepository categoryRepository;
    private final DrugRepository drugRepository;
    private final DrugBranchSettingRepository drugBranchSettingRepository;
    private final CacheHelper cacheHelper;
    private final CacheKeyBuilder cacheKeyBuilder;
    private final KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate;

    private static final UUID DEFAULT_BRANCH_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public CatalogService(CategoryRepository categoryRepository, DrugRepository drugRepository,
            DrugBranchSettingRepository drugBranchSettingRepository, CacheHelper cacheHelper,
            CacheKeyBuilder cacheKeyBuilder,
            KafkaTemplate<String, EventEnvelope<?>> kafkaTemplate) {
        this.categoryRepository = categoryRepository;
        this.drugRepository = drugRepository;
        this.drugBranchSettingRepository = drugBranchSettingRepository;
        this.cacheHelper = cacheHelper;
        this.cacheKeyBuilder = cacheKeyBuilder;
        this.kafkaTemplate = kafkaTemplate;
    }

    // Public queries
    public List<Category> listPublicCategories() {
        return cacheHelper.getOrSetCacheByTtlKey(cacheKeyBuilder.build("category", "list", "active"),
                CacheConstants.TTL_CATEGORY_LIST,
                categoryRepository::findByActiveTrueOrderBySortOrderAscNameAsc);
    }

    public Page<DrugPublicDto> searchPublicProducts(String q, UUID categoryId, UUID branchId, Pageable pageable) {
        String keyword = normalizeKeyword(q);
        UUID resolvedBranchId = resolveBranchId(branchId);
        String cacheKey = buildPublicListCacheKey(keyword, categoryId, resolvedBranchId, pageable);
        return cacheHelper.getOrSetCacheByTtlKey(cacheKey, CacheConstants.TTL_PRODUCT_LIST,
                () -> drugRepository.searchPublicByBranch(keyword, categoryId, resolvedBranchId, pageable)
                        .map(view -> toPublicDto(view, resolvedBranchId)));
    }

    public Page<DrugAdminDto> searchProducts(String q, UUID categoryId, String status, UUID branchId,
            Pageable pageable) {
        String keyword = normalizeKeyword(q);
        String normalizedStatus = normalizeStatus(status);
        UUID resolvedBranchId = resolveBranchId(branchId);
        return drugRepository.searchAdminByBranch(keyword, categoryId, normalizedStatus, resolvedBranchId, pageable)
                .map(view -> toAdminDto(view, resolvedBranchId));
    }

    public DrugPublicDto getPublicProduct(String idOrSlug, UUID branchId) {
        UUID resolvedBranchId = resolveBranchId(branchId);
        String cacheKey = cacheKeyBuilder.build("product", "detail", idOrSlug, "branch", resolvedBranchId);
        return cacheHelper.getOrSetCacheByTtlKey(cacheKey, CacheConstants.TTL_PRODUCT_DETAIL, () -> {
            DrugWithBranchView view = findWithBranchByIdOrSlug(idOrSlug, resolvedBranchId);
            String effectiveStatus = resolveEffectiveStatus(view);
            if (!"ACTIVE".equalsIgnoreCase(effectiveStatus)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại");
            }
            return toPublicDto(view, resolvedBranchId);
        });
    }

    public DrugAdminDto getDrug(UUID id, UUID branchId) {
        UUID resolvedBranchId = resolveBranchId(branchId);
        DrugWithBranchView view = drugRepository
                .findWithBranchById(Objects.requireNonNull(id, "id"), resolvedBranchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"));
        return toAdminDto(view, resolvedBranchId);
    }

    public DrugAdminDto getDrugByIdOrSlug(String idOrSlug, UUID branchId) {
        UUID resolvedBranchId = resolveBranchId(branchId);
        DrugWithBranchView view = findWithBranchByIdOrSlug(idOrSlug, resolvedBranchId);
        return toAdminDto(view, resolvedBranchId);
    }

    private DrugWithBranchView findWithBranchByIdOrSlug(String idOrSlug, UUID branchId) {
        try {
            UUID id = UUID.fromString(idOrSlug);
            return drugRepository.findWithBranchById(id, branchId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"));
        } catch (IllegalArgumentException ex) {
            return drugRepository.findWithBranchBySlug(idOrSlug, branchId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"));
        }
    }

    // Category
    public Category createCategory(CategoryRequest req) {
        String name = req.name() == null ? "" : req.name().trim();
        String slug = req.slug() == null ? "" : req.slug().trim().toLowerCase();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên danh mục không hợp lệ");
        }
        if (slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug không hợp lệ");
        }
        if (categoryRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug danh mục đã tồn tại");
        }
        UUID parentId = req.parentId();
        if (parentId != null && !categoryRepository.existsById(parentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục cha không tồn tại");
        }
        Category c = new Category();
        c.setId(UUID.randomUUID());
        c.setName(name);
        c.setSlug(slug);
        c.setParentId(parentId);
        c.setDescription(req.description() == null ? "" : req.description().trim());
        c.setActive(req.active() == null || req.active());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        c.setCreatedAt(Instant.now());
        Category saved = categoryRepository.save(c);
        invalidateCatalogCaches();
        return saved;
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategory(UUID id) {
        return categoryRepository.findById(Objects.requireNonNull(id, "id")).orElseThrow();
    }

    public Category updateCategory(UUID id, CategoryRequest req) {
        Category c = categoryRepository.findById(Objects.requireNonNull(id, "id")).orElseThrow();
        String name = req.name() == null ? "" : req.name().trim();
        String slug = req.slug() == null ? "" : req.slug().trim().toLowerCase();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên danh mục không hợp lệ");
        }
        if (slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug không hợp lệ");
        }
        categoryRepository.findBySlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug danh mục đã tồn tại");
            }
        });
        UUID parentId = req.parentId();
        if (parentId != null && parentId.equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục cha không hợp lệ");
        }
        if (parentId != null
                && !categoryRepository.existsById(Objects.requireNonNull(parentId, "parentId"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục cha không tồn tại");
        }
        c.setName(name);
        c.setSlug(slug);
        c.setParentId(parentId);
        c.setDescription(req.description() == null ? "" : req.description().trim());
        c.setActive(req.active() == null || req.active());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        Category saved = categoryRepository.save(c);
        invalidateCatalogCaches();
        return saved;
    }

    public void deleteCategory(UUID id) {
        UUID safeId = Objects.requireNonNull(id, "id");
        if (categoryRepository.existsByParentId(safeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Danh mục có danh mục con, không thể xóa");
        }
        categoryRepository.deleteById(safeId);
        invalidateCatalogCaches();
    }

    // Drug
    public DrugAdminDto createDrug(DrugRequest req) {
        if (req.categoryId() == null
                || !categoryRepository.existsById(Objects.requireNonNull(req.categoryId(), "categoryId"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại");
        }
        if (req.costPrice() == null || req.salePrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá không hợp lệ");
        }
        if (req.costPrice().signum() < 0 || req.salePrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá không hợp lệ");
        }
        String normalizedStatus = normalizeStatus(req.status());
        if (normalizedStatus == null) {
            normalizedStatus = "INACTIVE";
        }
        validateStatus(normalizedStatus);
        String slug = req.slug() == null ? "" : req.slug().trim().toLowerCase();
        if (slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug thuốc không hợp lệ");
        }
        if (drugRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug thuốc đã tồn tại");
        }
        Drug d = new Drug();
        d.setId(UUID.randomUUID());
        d.setSku(req.sku());
        d.setName(req.name());
        d.setSlug(slug);
        d.setCategoryId(req.categoryId());
        d.setCostPrice(req.costPrice());
        d.setSalePrice(req.salePrice());
        d.setStatus(normalizedStatus);
        d.setPrescriptionRequired(req.prescriptionRequired() != null && req.prescriptionRequired());
        d.setDescription(req.description());
        d.setImageUrl(req.imageUrl());
        d.setAttributes(req.attributes());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        Drug saved = drugRepository.save(d);
        invalidateProductCaches(saved.getId(), saved.getSlug());
        publishProductCacheInvalidation(EventTypes.PRODUCT_CREATED, saved.getId());
        return toAdminDto(saved, DEFAULT_BRANCH_ID);
    }

    public List<Drug> listDrugs() {
        return drugRepository.findAll();
    }

    public DrugAdminDto updateDrug(UUID id, DrugRequest req) {
        Drug d = drugRepository.findById(Objects.requireNonNull(id, "id"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại"));
        if (req.categoryId() == null
                || !categoryRepository.existsById(Objects.requireNonNull(req.categoryId(), "categoryId"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại");
        }
        if (req.costPrice() == null || req.salePrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá không hợp lệ");
        }
        if (req.costPrice().signum() < 0 || req.salePrice().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá không hợp lệ");
        }
        String normalizedStatus = normalizeStatus(req.status());
        if (normalizedStatus == null) {
            normalizedStatus = "INACTIVE";
        }
        validateStatus(normalizedStatus);
        String slug = req.slug() == null ? "" : req.slug().trim().toLowerCase();
        if (slug.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slug thuốc không hợp lệ");
        }
        drugRepository.findBySlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug thuốc đã tồn tại");
            }
        });
        d.setSku(req.sku());
        d.setName(req.name());
        d.setSlug(slug);
        d.setCategoryId(req.categoryId());
        d.setCostPrice(req.costPrice());
        d.setSalePrice(req.salePrice());
        d.setStatus(normalizedStatus);
        d.setPrescriptionRequired(req.prescriptionRequired() != null && req.prescriptionRequired());
        d.setDescription(req.description());
        d.setImageUrl(req.imageUrl());
        d.setAttributes(req.attributes());
        d.setUpdatedAt(Instant.now());
        Drug saved = drugRepository.save(d);
        invalidateProductCaches(saved.getId(), saved.getSlug());
        publishProductCacheInvalidation(EventTypes.PRODUCT_UPDATED, saved.getId());
        return toAdminDto(saved, DEFAULT_BRANCH_ID);
    }

    public void deleteDrug(UUID id) {
        UUID safeId = Objects.requireNonNull(id, "id");
        drugRepository.findById(safeId).ifPresent(drug -> invalidateProductCaches(drug.getId(), drug.getSlug()));
        drugRepository.deleteById(safeId);
        invalidateCatalogCaches();
        publishProductCacheInvalidation(EventTypes.PRODUCT_DELETED, safeId);
    }

    public DrugAdminDto upsertBranchSetting(UUID drugId, BranchSettingRequest req) {
        if (req == null || req.branchId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh không hợp lệ");
        }
        if (!drugRepository.existsById(Objects.requireNonNull(drugId, "drugId"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sản phẩm không tồn tại");
        }
        String nextStatus = normalizeStatus(req.status());
        if (nextStatus == null) {
            nextStatus = "ACTIVE";
        }
        validateStatus(nextStatus);
        DrugBranchSetting setting = drugBranchSettingRepository.findByDrugIdAndBranchId(drugId, req.branchId())
                .orElseGet(() -> {
                    DrugBranchSetting s = new DrugBranchSetting();
                    s.setId(UUID.randomUUID());
                    s.setDrugId(drugId);
                    s.setBranchId(req.branchId());
                    s.setCreatedAt(Instant.now());
                    return s;
                });
        setting.setPriceOverride(req.priceOverride());
        setting.setStatus(nextStatus);
        setting.setNote(req.note());
        setting.setUpdatedAt(Instant.now());
        drugBranchSettingRepository.save(setting);
        invalidateProductCaches(drugId, null);
        return getDrug(drugId, req.branchId());
    }

    public void deleteBranchSetting(UUID drugId, UUID branchId) {
        if (branchId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chi nhánh không hợp lệ");
        }
        drugBranchSettingRepository.findByDrugIdAndBranchId(drugId, branchId)
                .ifPresent(drugBranchSettingRepository::delete);
        invalidateProductCaches(drugId, null);
    }

    private String buildPublicListCacheKey(String keyword, UUID categoryId, UUID branchId, Pageable pageable) {
        String sort = pageable.getSort() == null ? "unsorted" : pageable.getSort().toString();
        return cacheKeyBuilder.build("product", "list",
                "page", pageable.getPageNumber(),
                "size", pageable.getPageSize(),
                "q", (keyword == null ? "_" : keyword),
                "category", (categoryId == null ? "_" : categoryId),
                "branch", branchId,
                "sort", sort);
    }

    private void invalidateCatalogCaches() {
        cacheHelper.evict(cacheKeyBuilder.build("category", "list", "active"));
        cacheHelper.evictByPattern(cacheKeyBuilder.pattern("product", "list"));
    }

    private void invalidateProductCaches(UUID drugId, String slug) {
        cacheHelper.evictByPattern(cacheKeyBuilder.pattern("product", "list"));
        if (drugId != null) {
            cacheHelper.evictByPattern(cacheKeyBuilder.pattern("product", "detail", drugId));
        }
        if (slug != null && !slug.isBlank()) {
            cacheHelper.evictByPattern(cacheKeyBuilder.pattern("product", "detail", slug));
        }
    }

    private void publishProductCacheInvalidation(String eventType, UUID productId) {
        try {
            CacheInvalidationEvent event = new CacheInvalidationEvent(
                    "product",
                    eventType,
                    productId == null ? null : productId.toString());
            kafkaTemplate.send(CacheConstants.CACHE_INVALIDATION_TOPIC, EventEnvelope.of(eventType, "1", event));
        } catch (Exception ex) {
            // Do not fail write transactions when Kafka is unavailable.
        }
    }

    private String normalizeKeyword(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.trim().toUpperCase();
    }

    private UUID resolveBranchId(UUID branchId) {
        return branchId == null ? DEFAULT_BRANCH_ID : branchId;
    }

    private String resolveEffectiveStatus(DrugWithBranchView view) {
        if (view.getBranchStatus() != null && !view.getBranchStatus().isBlank()) {
            return view.getBranchStatus();
        }
        return view.getGlobalStatus();
    }

    private BigDecimal resolveEffectivePrice(DrugWithBranchView view) {
        return view.getPriceOverride() != null ? view.getPriceOverride() : view.getBaseSalePrice();
    }

    private void validateStatus(String status) {
        if (!"ACTIVE".equalsIgnoreCase(status) && !"INACTIVE".equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ");
        }
    }

    private DrugPublicDto toPublicDto(DrugWithBranchView view, UUID branchId) {
        BigDecimal effectivePrice = resolveEffectivePrice(view);
        String effectiveStatus = resolveEffectiveStatus(view);
        return new DrugPublicDto(view.getId(), view.getSku(), view.getName(), view.getSlug(), view.getCategoryId(),
                effectivePrice, effectiveStatus, view.isPrescriptionRequired(), view.getDescription(),
                view.getImageUrl(), view.getAttributes());
    }

    private DrugAdminDto toAdminDto(DrugWithBranchView view, UUID branchId) {
        BigDecimal effectivePrice = resolveEffectivePrice(view);
        String effectiveStatus = resolveEffectiveStatus(view);
        return new DrugAdminDto(view.getId(), view.getSku(), view.getName(), view.getSlug(), view.getCategoryId(),
                view.getCostPrice(), view.getBaseSalePrice(), view.getPriceOverride(), effectivePrice,
                view.getGlobalStatus(),
                view.getBranchStatus(), effectiveStatus, view.isPrescriptionRequired(), view.getDescription(),
                view.getImageUrl(), view.getAttributes(), branchId, view.getNote());
    }

    private DrugAdminDto toAdminDto(Drug drug, UUID branchId) {
        BigDecimal baseSalePrice = drug.getSalePrice();
        return new DrugAdminDto(drug.getId(), drug.getSku(), drug.getName(), drug.getSlug(), drug.getCategoryId(),
                drug.getCostPrice(), baseSalePrice, null, baseSalePrice, drug.getStatus(), null, drug.getStatus(),
                drug.isPrescriptionRequired(), drug.getDescription(), drug.getImageUrl(), drug.getAttributes(),
                branchId, null);
    }
}
