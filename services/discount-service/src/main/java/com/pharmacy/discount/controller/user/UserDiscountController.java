package com.pharmacy.discount.controller.user;

import com.pharmacy.discount.dto.ApplyDiscountRequest;
import com.pharmacy.discount.dto.ApplyDiscountResponse;
import com.pharmacy.discount.dto.CampaignResponse;
import com.pharmacy.discount.dto.DiscountResponse;
import com.pharmacy.discount.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/discounts")
public class UserDiscountController {

    private final DiscountService discountService;

    public UserDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping("/available")
    public ResponseEntity<List<DiscountResponse>> available(@AuthenticationPrincipal Jwt jwt) {
        String userId = requireUserId(jwt);
        return ResponseEntity.ok(discountService.getAvailableDiscountsForUser(userId));
    }

    @PostMapping("/validate")
    public ResponseEntity<ApplyDiscountResponse> validate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplyDiscountRequest request) {
        String userId = requireUserId(jwt);
        return ResponseEntity.ok(discountService.validateDiscount(userId, request));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApplyDiscountResponse> apply(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ApplyDiscountRequest request) {
        String userId = requireUserId(jwt);
        return ResponseEntity.ok(discountService.validateAndApplyDiscount(userId, request));
    }

    @GetMapping("/campaigns")
    public ResponseEntity<List<CampaignResponse>> campaigns() {
        return ResponseEntity.ok(discountService.getActiveCampaigns());
    }

    private String requireUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new IllegalStateException("Missing JWT subject");
        }
        return jwt.getSubject();
    }
}
