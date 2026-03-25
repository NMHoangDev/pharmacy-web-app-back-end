package com.pharmacy.discount.dto;

import com.pharmacy.discount.entity.DiscountStatus;
import jakarta.validation.constraints.NotNull;

public class ToggleDiscountStatusRequest {

    @NotNull
    private Long id;

    @NotNull
    private DiscountStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DiscountStatus getStatus() {
        return status;
    }

    public void setStatus(DiscountStatus status) {
        this.status = status;
    }
}
