package com.alumni.management.verification.exception;

public class ResendCooldownException extends RuntimeException {
    private final long secondsRemaining;

    public ResendCooldownException(String message, long secondsRemaining) {
        super(message);
        this.secondsRemaining = secondsRemaining;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
