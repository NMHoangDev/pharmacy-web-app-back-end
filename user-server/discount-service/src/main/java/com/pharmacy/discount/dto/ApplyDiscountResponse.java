package com.pharmacy.discount.dto;

import java.math.BigDecimal;

public class ApplyDiscountResponse {

    private boolean valid;
    private String reason;
    private BigDecimal discountAmount;
    private BigDecimal shippingDiscount;
    private BigDecimal finalTotal;
    private DiscountResponse discount;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getShippingDiscount() {
        return shippingDiscount;
    }

    public void setShippingDiscount(BigDecimal shippingDiscount) {
        this.shippingDiscount = shippingDiscount;
    }

    public BigDecimal getFinalTotal() {
        return finalTotal;
    }

    public void setFinalTotal(BigDecimal finalTotal) {
        this.finalTotal = finalTotal;
    }

    public DiscountResponse getDiscount() {
        return discount;
    }

    public void setDiscount(DiscountResponse discount) {
        this.discount = discount;
    }
}
