package com.chubb.claims.claim.adapter.out.otp;

import com.chubb.claims.claim.application.port.out.OtpVerifier;
import com.chubb.claims.claim.config.ClaimsProperties;
import org.springframework.stereotype.Component;

/**
 * Accepts one fixed code so the phone-claim flow can be exercised end to end.
 * This must be replaced with a real OTP channel before production.
 */
@Component
class StubOtpVerifier implements OtpVerifier {

    private final String stubCode;

    StubOtpVerifier(ClaimsProperties properties) {
        this.stubCode = properties.otp().stubCode();
    }

    @Override
    public boolean verify(String policyNumber, String otp) {
        return stubCode != null && stubCode.equals(otp);
    }
}
