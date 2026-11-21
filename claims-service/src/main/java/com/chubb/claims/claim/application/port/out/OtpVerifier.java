package com.chubb.claims.claim.application.port.out;

/**
 * Checks a one-time password for phone-claim verification (TDD 7.4). The first release uses a stub.
 */
public interface OtpVerifier {

    boolean verify(String policyNumber, String otp);
}
