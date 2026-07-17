package com.flowbase.engine.collection.validation;

import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.exception.ValidationException;
@FunctionalInterface
public interface ValidationRule {
    void validate(CollectionField field, Object value) throws ValidationException;
}
