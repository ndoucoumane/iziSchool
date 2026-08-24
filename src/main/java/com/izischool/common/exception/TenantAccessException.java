package com.izischool.common.exception;

import org.springframework.http.HttpStatus;

public class TenantAccessException extends ApiException {
    public TenantAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED");
    }

    public TenantAccessException(String message, String code) {
        super(message, HttpStatus.FORBIDDEN, code);
    }
}
