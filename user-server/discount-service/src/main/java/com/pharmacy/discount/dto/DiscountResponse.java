package com.pharmacy.discount.dto;

import com.pharmacy.discount.entity.DiscountStatus;
import com.pharmacy.discount.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class DiscountResponse {

    private Long id;
    private String name;
    private String code;
    private DiscountType type;
    private BigDecimal value;
    private BigDecimal maxDiscount;
    private BigDecimal minOrderValue;
    private Integer usageLimit;
    private Integer usagePerUser;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private DiscountStatus status;
    private LocalDateTime createdAt;
    private List<Scope> scopes;
    private boolean targeted;

    public static class Scope {
        private String scopeType;
        private Long scopeId;

        public String getScopeType() {
            return scopeType;
        }

        public void setScopeType(String scopeType) {
            this.scopeType = scopeType;
        }

        public Long getScopeId() {
            return scopeId;
        }

        public void setScopeId(Long scopeId) {
            this.scopeId = scopeId;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public DiscountType getType() {
        return type;
    }

    public void setType(DiscountType type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(BigDecimal minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsagePerUser() {
        return usagePerUser;
    }

    public void setUsagePerUser(Integer usagePerUser) {
        this.usagePerUser = usagePerUser;
    }

    public Integer getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(Integer usedCount) {
        this.usedCount = usedCount;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public DiscountStatus getStatus() {
        return status;
    }

    public void setStatus(DiscountStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Scope> getScopes() {
        return scopes;
    }

    public void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    public boolean isTargeted() {
        return targeted;
    }

    public void setTargeted(boolean targeted) {
        this.targeted = targeted;
    }
}
