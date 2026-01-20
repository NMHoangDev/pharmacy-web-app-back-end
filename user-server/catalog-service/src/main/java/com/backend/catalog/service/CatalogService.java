package com.backend.catalog.service;

import com.backend.catalog.api.dto.CategoryRequest;
import com.backend.catalog.api.dto.DrugRequest;
import com.backend.catalog.model.Category;
import com.backend.catalog.model.Drug;
import com.backend.catalog.repo.CategoryRepository;
import com.backend.catalog.repo.DrugRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CatalogService {
    private final CategoryRepository categoryRepository;
    private final DrugRepository drugRepository;

    public CatalogService(CategoryRepository categoryRepository, DrugRepository drugRepository) {
        this.categoryRepository = categoryRepository;
        this.drugRepository = drugRepository;
    }

    // Public queries
    public List<Category> listPublicCategories() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    public Page<Drug> searchPublicProducts(String q, UUID categoryId, Pageable pageable) {
        return drugRepository.searchActive(q == null || q.isBlank() ? null : q.trim(), categoryId, pageable);
    }

    public Page<Drug> searchProducts(String q, UUID categoryId, String status, Pageable pageable) {
        String keyword = q == null || q.isBlank() ? null : q.trim();
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        return drugRepository.searchAll(keyword, categoryId, normalizedStatus, pageable);
    }

    public Drug getPublicProduct(String idOrSlug) {
        try {
            UUID id = UUID.fromString(idOrSlug);
            return drugRepository.findById(id).orElseThrow();
        } catch (IllegalArgumentException ex) {
            return drugRepository.findBySlug(idOrSlug).orElseThrow();
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
        return categoryRepository.save(c);
    }

    public List<Category> listCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategory(UUID id) {
        return categoryRepository.findById(id).orElseThrow();
    }

    public Category updateCategory(UUID id, CategoryRequest req) {
        Category c = categoryRepository.findById(id).orElseThrow();
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
        if (parentId != null && !categoryRepository.existsById(parentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục cha không tồn tại");
        }
        c.setName(name);
        c.setSlug(slug);
        c.setParentId(parentId);
        c.setDescription(req.description() == null ? "" : req.description().trim());
        c.setActive(req.active() == null || req.active());
        c.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
        return categoryRepository.save(c);
    }

    public void deleteCategory(UUID id) {
        if (categoryRepository.existsByParentId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Danh mục có danh mục con, không thể xóa");
        }
        categoryRepository.deleteById(id);
    }

    // Drug
    public Drug createDrug(DrugRequest req) {
        if (req.categoryId() == null || !categoryRepository.existsById(req.categoryId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại");
        }
        if (req.price() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá không hợp lệ");
        }
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
        d.setPrice(req.price());
        d.setStatus(req.status() == null ? "INACTIVE" : req.status().trim().toUpperCase());
        d.setPrescriptionRequired(req.prescriptionRequired() != null && req.prescriptionRequired());
        d.setDescription(req.description());
        d.setImageUrl(req.imageUrl());
        d.setAttributes(req.attributes());
        d.setCreatedAt(Instant.now());
        d.setUpdatedAt(Instant.now());
        return drugRepository.save(d);
    }

    public List<Drug> listDrugs() {
        return drugRepository.findAll();
    }

    public Drug getDrug(UUID id) {
        return drugRepository.findById(id).orElseThrow();
    }

    public Drug updateDrug(UUID id, DrugRequest req) {
        Drug d = drugRepository.findById(id).orElseThrow();
        if (req.categoryId() == null || !categoryRepository.existsById(req.categoryId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Danh mục không tồn tại");
        }
        if (req.price() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Giá không hợp lệ");
        }
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
        d.setPrice(req.price());
        d.setStatus(req.status() == null ? "INACTIVE" : req.status().trim().toUpperCase());
        d.setPrescriptionRequired(req.prescriptionRequired() != null && req.prescriptionRequired());
        d.setDescription(req.description());
        d.setImageUrl(req.imageUrl());
        d.setAttributes(req.attributes());
        d.setUpdatedAt(Instant.now());
        return drugRepository.save(d);
    }

    public void deleteDrug(UUID id) {
        drugRepository.deleteById(id);
    }
}
