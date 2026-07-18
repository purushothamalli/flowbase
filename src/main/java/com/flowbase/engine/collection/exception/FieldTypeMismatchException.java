package com.flowbase.engine.collection.exception;

public class FieldTypeMismatchException extends ValidationException {
    public FieldTypeMismatchException(String fieldName, String expectedType, String actualType) {
        super("Type mismatch for field '" + fieldName + "'. Expected: " + expectedType + ", but received: " + actualType);
    }
}
