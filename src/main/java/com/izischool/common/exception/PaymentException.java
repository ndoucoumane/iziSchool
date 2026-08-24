package com.izischool.common.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends ApiException {
    public PaymentException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_ERROR");
    }

    public PaymentException(String message, String code) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, code);
    }

    public PaymentException(String message, HttpStatus status, String code) {
        super(message, status, code);
    }
}
