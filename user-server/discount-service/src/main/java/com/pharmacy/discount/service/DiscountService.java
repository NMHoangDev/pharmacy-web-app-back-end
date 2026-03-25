package com.pharmacy.discount.service;

import com.pharmacy.discount.dto.*;

import java.util.List;

public interface DiscountService {

    DiscountResponse createDiscount(DiscountCreateRequest request);

    DiscountResponse updateDiscount(Long id, DiscountUpdateRequest request);

    void deleteDiscount(Long id);

    DiscountResponse toggleStatus(ToggleDiscountStatusRequest request);

    List<DiscountResponse> getAllDiscounts();

    List<DiscountResponse> getAvailableDiscountsForUser(String userId);

    ApplyDiscountResponse validateDiscount(String userId, ApplyDiscountRequest request);

    ApplyDiscountResponse validateAndApplyDiscount(String userId, ApplyDiscountRequest request);

    List<CampaignResponse> getActiveCampaigns();
}
