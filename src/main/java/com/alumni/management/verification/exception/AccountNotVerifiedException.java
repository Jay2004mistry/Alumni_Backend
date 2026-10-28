package com.alumni.management.verification.exception;

import com.alumni.management.verification.enums.VerificationPurpose;
import lombok.Getter;

@Getter
public class AccountNotVerifiedException extends RuntimeException {

    private final String email;
    private final boolean otpDispatched;
    private final int cooldownSeconds;
    private final VerificationPurpose purpose;

    public AccountNotVerifiedException(String message) {
        this(message, null, false, 0, VerificationPurpose.ACCOUNT_ACTIVATION);
    }

    public AccountNotVerifiedException(String message, String email, boolean otpDispatched, int cooldownSeconds, VerificationPurpose purpose) {
        super(message);
        this.email = email;
        this.otpDispatched = otpDispatched;
        this.cooldownSeconds = cooldownSeconds;
        this.purpose = purpose != null ? purpose : VerificationPurpose.ACCOUNT_ACTIVATION;
    }
}
