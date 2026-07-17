package com.flowbase.engine.auth.exception;

public class RevokedTokenException extends AuthenticationException {
    public RevokedTokenException(String message) {
        super(message);
    }
}
