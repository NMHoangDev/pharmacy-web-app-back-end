package com.pharmacy.discount.service;

import com.pharmacy.discount.dto.*;
import com.pharmacy.discount.entity.*;
import com.pharmacy.discount.exception.BadRequestException;
import com.pharmacy.discount.exception.ConflictException;
import com.pharmacy.discount.exception.NotFoundException;
import com.pharmacy.discount.kafka.DiscountEventPublisher;
import com.pharmacy.discount.kafka.DiscountEventTypes;
import com.pharmacy.discount.mapper.DiscountMapper;
import com.pharmacy.discount.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscountServiceImpl implements DiscountService {

    private static final Logger log = LoggerFactory.getLogger(DiscountServiceImpl.class);

    private final DiscountRepository discountRepository;
    private final DiscountScopeRepository discountScopeRepository;
    private final DiscountUsageRepository discountUsageRepository;
    private final DiscountUserTargetRepository discountUserTargetRepository;
    private final DiscountEventPublisher eventPublisher;

    private static final DateTimeFormatter CAMPAIGN_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM");

    public DiscountServiceImpl(
            DiscountRepository discountRepository,
            DiscountScopeRepository discountScopeRepository,
            DiscountUsageRepository discountUsageRepository,
            DiscountUserTargetRepository discountUserTargetRepository,
            DiscountEventPublisher eventPublisher) {
        this.discountRepository = discountRepository;
        this.discountScopeRepository = discountScopeRepository;
        this.discountUsageRepository = discountUsageRepository;
        this.discountUserTargetRepository = discountUserTargetRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DiscountResponse createDiscount(DiscountCreateRequest request) {
        discountRepository.findByCodeIgnoreCase(request.getCode())
                .ifPresent(existing -> {
                    throw new ConflictException("Mã khuyến mãi đã tồn tại");
                });

        Discount discount = new Discount();
        applyUpsertFields(discount, request.getName(), request.getCode(), request.getType(), request.getValue(),
                request.getMaxDiscount(), request.getMinOrderValue(), request.getUsageLimit(),
                request.getUsagePerUser(),
                request.getStartDate(), request.getEndDate(), request.getStatus());

        discount = discountRepository.save(discount);

        upsertScopesAndTargets(discount, request.getScopes(), request.getTargetUserIds());

        DiscountResponse res = toResponse(discount, null);
        eventPublisher.publishSafe(DiscountEventTypes.DISCOUNT_CREATED, String.valueOf(discount.getId()),
                Map.of("discountId", discount.getId(), "code", discount.getCode()));
        return res;
    }

    @Override
    @Transactional
    public DiscountResponse updateDiscount(Long id, DiscountUpdateRequest request) {
        Discount discount = discountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Discount not found: " + id));

        discountRepository.findByCodeIgnoreCase(request.getCode())
                .filter(existing -> !Objects.equals(existing.getId(), id))
                .ifPresent(existing -> {
                    throw new ConflictException("Mã khuyến mãi đã tồn tại");
                });

        applyUpsertFields(discount, request.getName(), request.getCode(), request.getType(), request.getValue(),
                request.getMaxDiscount(), request.getMinOrderValue(), request.getUsageLimit(),
                request.getUsagePerUser(),
                request.getStartDate(), request.getEndDate(), request.getStatus());

        discount = discountRepository.save(discount);

        upsertScopesAndTargets(discount, request.getScopes(), request.getTargetUserIds());

        DiscountResponse res = toResponse(discount, null);
        eventPublisher.publishSafe(DiscountEventTypes.DISCOUNT_UPDATED, String.valueOf(discount.getId()),
                Map.of("discountId", discount.getId(), "code", discount.getCode()));
        return res;
    }

    @Override
    @Transactional
    public void deleteDiscount(Long id) {
        if (!discountRepository.existsById(id)) {
            throw new NotFoundException("Discount not found: " + id);
        }
        discountRepository.deleteById(id);
    }

    @Override
    @Transactional
    public DiscountResponse toggleStatus(ToggleDiscountStatusRequest request) {
        Discount discount = discountRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException("Discount not found: " + request.getId()));

        DiscountStatus desired = request.getStatus();
        if (desired == DiscountStatus.DISABLED) {
            discount.setStatus(DiscountStatus.DISABLED);
        } else {
            discount.setStatus(computeStatus(null, discount.getStartDate(), discount.getEndDate()));
        }
        discount = discountRepository.save(discount);
        eventPublisher.publishSafe(DiscountEventTypes.DISCOUNT_UPDATED, String.valueOf(discount.getId()),
                Map.of("discountId", discount.getId(), "status", discount.getStatus().name()));
        return toResponse(discount, null);
    }

    private DiscountStatus computeStatus(DiscountStatus requested, LocalDateTime startDate, LocalDateTime endDate) {
        if (requested == DiscountStatus.DISABLED) {
            return DiscountStatus.DISABLED;
        }

        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) {
            return DiscountStatus.SCHEDULED;
        }
        if (endDate != null && now.isAfter(endDate)) {
            return DiscountStatus.EXPIRED;
        }
        return DiscountStatus.ACTIVE;
    }

    @Override
    public List<DiscountResponse> getAllDiscounts() {
        List<Discount> discounts = discountRepository.findAll();
        return discounts.stream()
                .map(d -> toResponse(d, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<DiscountResponse> getAvailableDiscountsForUser(String userId) {
        LocalDateTime now = LocalDateTime.now();
        List<Discount> active = discountRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(DiscountStatus.ACTIVE, now, now);

        return active.stream()
                .filter(d -> isUserEligible(d.getId(), userId))
                .map(d -> toResponse(d, userId))
                .collect(Collectors.toList());
    }

    @Override
    public ApplyDiscountResponse validateDiscount(String userId, ApplyDiscountRequest request) {
        return validateInternal(userId, request, false);
    }

    @Override
    @Transactional
    public ApplyDiscountResponse validateAndApplyDiscount(String userId, ApplyDiscountRequest request) {
        return validateInternal(userId, request, true);
    }

    @Override
    @Cacheable(cacheNames = "active_discounts", key = "'active_discounts'", unless = "#result == null || #result.isEmpty()")
    public List<CampaignResponse> getActiveCampaigns() {
        LocalDateTime now = LocalDateTime.now();
        List<Discount> active = discountRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByEndDateAsc(
                        DiscountStatus.ACTIVE,
                        now,
                        now,
                        PageRequest.of(0, 5));

        return active.stream()
                .map(this::toCampaignResponse)
                .collect(Collectors.toList());
    }

    private CampaignResponse toCampaignResponse(Discount discount) {
        String displayText = buildDisplayText(discount);
        return new CampaignResponse(
                discount.getId(),
                discount.getName(),
                discount.getType(),
                discount.getValue(),
                discount.getEndDate(),
                displayText);
    }

    private String buildDisplayText(Discount discount) {
        if (discount == null)
            return "";

        StringBuilder sb = new StringBuilder();
        DiscountType type = discount.getType();
        BigDecimal value = discount.getValue();

        if (type == DiscountType.PERCENT) {
            sb.append("Giảm ").append(formatPercent(value));
        } else if (type == DiscountType.FIXED) {
            sb.append("Giảm ").append(formatMoney(value));
        } else if (type == DiscountType.FREESHIP) {
            sb.append("Freeship");
        } else {
            sb.append("Ưu đãi");
        }

        if (type == DiscountType.PERCENT && discount.getMaxDiscount() != null
                && discount.getMaxDiscount().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" tối đa ").append(formatMoney(discount.getMaxDiscount()));
        }

        if (discount.getMinOrderValue() != null && discount.getMinOrderValue().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(" • Đơn từ ").append(formatMoney(discount.getMinOrderValue()));
        }

        if (discount.getEndDate() != null) {
            sb.append(" đến ").append(discount.getEndDate().format(CAMPAIGN_DATE_FMT));
        }

        return sb.toString();
    }

    private String formatPercent(BigDecimal value) {
        if (value == null)
            return "0%";
        String raw = value.stripTrailingZeros().toPlainString();
        return raw + "%";
    }

    private String formatMoney(BigDecimal value) {
        if (value == null)
            return "0đ";
        try {
            NumberFormat nf = NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            nf.setMaximumFractionDigits(0);
            nf.setMinimumFractionDigits(0);
            return nf.format(value) + "đ";
        } catch (Exception e) {
            return value.stripTrailingZeros().toPlainString() + "đ";
        }
    }

    private ApplyDiscountResponse validateInternal(String userId, ApplyDiscountRequest request, boolean applyUsage) {
        Discount discount = discountRepository.findByCodeIgnoreCase(request.getCode())
                .orElseThrow(() -> new NotFoundException("Discount code not found"));

        ValidationOutcome outcome = validateDiscountRules(discount, userId, request);
        if (!outcome.valid) {
            log.info("Discount validation failed code={} user={} reason={}", request.getCode(), userId, outcome.reason);
            return invalid(outcome.reason);
        }

        Calculation calc = calculate(discount, request.getOrder());

        if (applyUsage) {
            DiscountUsage usage = new DiscountUsage();
            usage.setDiscount(discount);
            usage.setUserId(userId);
            usage.setOrderId(request.getOrderId());
            discountUsageRepository.save(usage);
            discount.setUsedCount(discount.getUsedCount() + 1);
            discountRepository.save(discount);

            eventPublisher.publishSafe(DiscountEventTypes.DISCOUNT_USED, request.getOrderId(),
                    Map.of("discountId", discount.getId(), "code", discount.getCode(), "userId", userId, "orderId",
                            request.getOrderId(),
                            "discountAmount", calc.discountAmount));

            log.info("Discount used code={} user={} order={} amount={}", discount.getCode(), userId,
                    request.getOrderId(), calc.discountAmount);
        }

        ApplyDiscountResponse res = new ApplyDiscountResponse();
        res.setValid(true);
        res.setDiscountAmount(calc.discountAmount);
        res.setShippingDiscount(calc.shippingDiscount);
        res.setFinalTotal(calc.finalTotal);
        res.setDiscount(toResponse(discount, userId));
        return res;
    }

    private DiscountResponse toResponse(Discount discount, String userId) {
        List<DiscountScope> scopes = discountScopeRepository.findByDiscount_Id(discount.getId());
        boolean targeted = discountUserTargetRepository.existsByDiscount_Id(discount.getId());
        if (userId != null && targeted) {
            targeted = discountUserTargetRepository.existsByDiscount_IdAndUserId(discount.getId(), userId);
        }
        return DiscountMapper.toResponse(discount, scopes, targeted);
    }

    private boolean isUserEligible(Long discountId, String userId) {
        boolean targeted = discountUserTargetRepository.existsByDiscount_Id(discountId);
        return !targeted || discountUserTargetRepository.existsByDiscount_IdAndUserId(discountId, userId);
    }

    private record ValidationOutcome(boolean valid, String reason) {
    }

    private ValidationOutcome validateDiscountRules(Discount discount, String userId, ApplyDiscountRequest request) {
        LocalDateTime now = LocalDateTime.now();

        if (discount.getStatus() != DiscountStatus.ACTIVE) {
            return new ValidationOutcome(false, "Discount is not active");
        }
        if (now.isBefore(discount.getStartDate()) || now.isAfter(discount.getEndDate())) {
            return new ValidationOutcome(false, "Discount is out of time range");
        }
        if (discount.getMinOrderValue() != null
                && request.getOrder().getTotal().compareTo(discount.getMinOrderValue()) < 0) {
            return new ValidationOutcome(false, "Order total below minimum");
        }
        if (discount.getUsageLimit() != null && discount.getUsedCount() >= discount.getUsageLimit()) {
            return new ValidationOutcome(false, "Discount usage limit reached");
        }
        if (discount.getUsagePerUser() != null) {
            long usedByUser = discountUsageRepository.countByDiscount_IdAndUserId(discount.getId(), userId);
            if (usedByUser >= discount.getUsagePerUser()) {
                return new ValidationOutcome(false, "Discount usage per user reached");
            }
        }

        if (!isUserEligible(discount.getId(), userId)) {
            return new ValidationOutcome(false, "User not eligible for this discount");
        }

        List<DiscountScope> scopes = discountScopeRepository.findByDiscount_Id(discount.getId());
        if (!scopes.isEmpty() && !scopeMatches(scopes, request.getOrder().getItems())) {
            return new ValidationOutcome(false, "Discount scope not applicable");
        }

        if (request.getOrder().getTotal().compareTo(BigDecimal.ZERO) < 0) {
            return new ValidationOutcome(false, "Invalid order total");
        }

        return new ValidationOutcome(true, null);
    }

    private boolean scopeMatches(List<DiscountScope> scopes, List<ApplyDiscountRequest.Order.Item> items) {
        if (scopes.stream().anyMatch(s -> s.getScopeType() == ScopeType.ALL)) {
            return true;
        }
        if (items == null || items.isEmpty()) {
            return false;
        }
        Set<Long> productIds = items.stream().map(ApplyDiscountRequest.Order.Item::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> categoryIds = items.stream().map(ApplyDiscountRequest.Order.Item::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (DiscountScope s : scopes) {
            if (s.getScopeType() == ScopeType.PRODUCT && s.getScopeId() != null
                    && productIds.contains(s.getScopeId())) {
                return true;
            }
            if (s.getScopeType() == ScopeType.CATEGORY && s.getScopeId() != null
                    && categoryIds.contains(s.getScopeId())) {
                return true;
            }
        }
        return false;
    }

    private record Calculation(BigDecimal discountAmount, BigDecimal shippingDiscount, BigDecimal finalTotal) {
    }

    private Calculation calculate(Discount discount, ApplyDiscountRequest.Order order) {
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;

        if (discount.getType() == DiscountType.PERCENT) {
            BigDecimal pct = discount.getValue().divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            discountAmount = order.getSubtotal().multiply(pct);
        } else if (discount.getType() == DiscountType.FIXED) {
            discountAmount = discount.getValue();
        } else if (discount.getType() == DiscountType.FREESHIP) {
            shippingDiscount = order.getShippingFee().min(discount.getValue());
        }

        if (discount.getMaxDiscount() != null && discountAmount.compareTo(discount.getMaxDiscount()) > 0) {
            discountAmount = discount.getMaxDiscount();
        }

        BigDecimal finalTotal = order.getTotal()
                .subtract(discountAmount)
                .subtract(shippingDiscount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        discountAmount = discountAmount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        shippingDiscount = shippingDiscount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        finalTotal = finalTotal.setScale(2, RoundingMode.HALF_UP);

        return new Calculation(discountAmount, shippingDiscount, finalTotal);
    }

    private ApplyDiscountResponse invalid(String reason) {
        ApplyDiscountResponse res = new ApplyDiscountResponse();
        res.setValid(false);
        res.setReason(reason);
        res.setDiscountAmount(BigDecimal.ZERO);
        res.setShippingDiscount(BigDecimal.ZERO);
        res.setFinalTotal(null);
        return res;
    }

    private void applyUpsertFields(
            Discount discount,
            String name,
            String code,
            DiscountType type,
            BigDecimal value,
            BigDecimal maxDiscount,
            BigDecimal minOrderValue,
            Integer usageLimit,
            Integer usagePerUser,
            LocalDateTime startDate,
            LocalDateTime endDate,
            DiscountStatus status) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("endDate must be after startDate");
        }
        discount.setName(name);
        discount.setCode(code.trim());
        discount.setType(type);
        discount.setValue(value);
        discount.setMaxDiscount(maxDiscount);
        discount.setMinOrderValue(minOrderValue);
        discount.setUsageLimit(usageLimit);
        discount.setUsagePerUser(usagePerUser);
        discount.setStartDate(startDate);
        discount.setEndDate(endDate);
        discount.setStatus(computeStatus(status, startDate, endDate));
    }

    private void upsertScopesAndTargets(Discount discount, List<DiscountCreateRequest.ScopeRule> scopes,
            List<String> targetUserIds) {
        discountScopeRepository.deleteByDiscount_Id(discount.getId());

        if (scopes != null) {
            for (DiscountCreateRequest.ScopeRule s : scopes) {
                if (s.getScopeType() != ScopeType.ALL && s.getScopeId() == null) {
                    throw new BadRequestException("scopeId is required for " + s.getScopeType());
                }
                DiscountScope scope = new DiscountScope();
                scope.setDiscount(discount);
                scope.setScopeType(s.getScopeType());
                scope.setScopeId(s.getScopeId());
                discountScopeRepository.save(scope);
            }
        }

        // targets: clear and recreate
        discountUserTargetRepository.deleteByDiscount_Id(discount.getId());
        if (targetUserIds != null) {
            for (String userId : targetUserIds) {
                if (userId == null || userId.isBlank()) {
                    continue;
                }
                DiscountUserTarget target = new DiscountUserTarget();
                target.setDiscount(discount);
                target.setUserId(userId);
                discountUserTargetRepository.save(target);
            }
        }
    }
}
