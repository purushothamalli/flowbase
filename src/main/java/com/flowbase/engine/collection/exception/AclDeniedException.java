package com.flowbase.engine.collection.exception;

public class AclDeniedException extends RuntimeException {
    public AclDeniedException(String message) {
        super(message);
    }
}
