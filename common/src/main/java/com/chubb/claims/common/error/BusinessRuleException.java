package com.chubb.claims.common.error;

/**
 * Thrown when a request breaks a business rule, such as an invalid status transition
 * or a claim filed after the 30-day deadline. Mapped to HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
