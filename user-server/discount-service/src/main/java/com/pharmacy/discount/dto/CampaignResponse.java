package com.pharmacy.discount.dto;

import com.pharmacy.discount.entity.DiscountType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight DTO for showing active discount campaigns on the frontend.
 */
public class CampaignResponse {

    private Long id;
    private String name;
    private DiscountType type;
    private BigDecimal value;
    private LocalDateTime endDate;
    private String displayText;

    public CampaignResponse() {
    }

    public CampaignResponse(Long id, String name, DiscountType type, BigDecimal value, LocalDateTime endDate,
            String displayText) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
        this.endDate = endDate;
        this.displayText = displayText;
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

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }
}
