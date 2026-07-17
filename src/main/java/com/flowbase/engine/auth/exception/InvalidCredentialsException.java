package com.flowbase.engine.auth.exception;

public class InvalidCredentialsException extends AuthenticationException{
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
