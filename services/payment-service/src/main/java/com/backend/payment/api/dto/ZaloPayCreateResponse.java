package com.backend.payment.api.dto;

public record ZaloPayCreateResponse(String paymentUrl, String txnRef) {
}
