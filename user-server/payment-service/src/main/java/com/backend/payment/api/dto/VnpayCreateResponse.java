package com.backend.payment.api.dto;

public record VnpayCreateResponse(String code, String message, String data, String txnRef) {
}
