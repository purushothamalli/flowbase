package com.flowbase.engine.collection.exception;

public class MissingRequiredFieldException extends ValidationException {
    public MissingRequiredFieldException(String fieldName) {
        super("Field '" + fieldName + "' is required, but was empty or missing.");
    }
}
