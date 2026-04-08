package com.pharmacy.discount.mapper;

import com.pharmacy.discount.dto.DiscountResponse;
import com.pharmacy.discount.entity.Discount;
import com.pharmacy.discount.entity.DiscountScope;

import java.util.List;
import java.util.stream.Collectors;

public final class DiscountMapper {
    private DiscountMapper() {
    }

    public static DiscountResponse toResponse(Discount discount, List<DiscountScope> scopes, boolean targeted) {
        DiscountResponse res = new DiscountResponse();
        res.setId(discount.getId());
        res.setName(discount.getName());
        res.setCode(discount.getCode());
        res.setType(discount.getType());
        res.setValue(discount.getValue());
        res.setMaxDiscount(discount.getMaxDiscount());
        res.setMinOrderValue(discount.getMinOrderValue());
        res.setUsageLimit(discount.getUsageLimit());
        res.setUsagePerUser(discount.getUsagePerUser());
        res.setUsedCount(discount.getUsedCount());
        res.setStartDate(discount.getStartDate());
        res.setEndDate(discount.getEndDate());
        res.setStatus(discount.getStatus());
        res.setCreatedAt(discount.getCreatedAt());
        res.setTargeted(targeted);

        if (scopes != null) {
            res.setScopes(scopes.stream().map(s -> {
                DiscountResponse.Scope scope = new DiscountResponse.Scope();
                scope.setScopeType(String.valueOf(s.getScopeType()));
                scope.setScopeId(s.getScopeId());
                return scope;
            }).collect(Collectors.toList()));
        }
        return res;
    }
}
