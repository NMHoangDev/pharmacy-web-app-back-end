package com.pharmacy.discount.exception;

public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
