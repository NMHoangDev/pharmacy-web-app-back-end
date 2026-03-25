package com.pharmacy.discount.controller.admin;

import com.pharmacy.discount.dto.*;
import com.pharmacy.discount.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/discounts")
public class AdminDiscountController {

    private final DiscountService discountService;

    public AdminDiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/create")
    public ResponseEntity<DiscountResponse> create(@Valid @RequestBody DiscountCreateRequest request) {
        return ResponseEntity.ok(discountService.createDiscount(request));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<DiscountResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DiscountUpdateRequest request) {
        return ResponseEntity.ok(discountService.updateDiscount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        discountService.deleteDiscount(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/list")
    public ResponseEntity<List<DiscountResponse>> list() {
        return ResponseEntity.ok(discountService.getAllDiscounts());
    }

    @PatchMapping("/toggle-status")
    public ResponseEntity<DiscountResponse> toggleStatus(@Valid @RequestBody ToggleDiscountStatusRequest request) {
        return ResponseEntity.ok(discountService.toggleStatus(request));
    }
}
