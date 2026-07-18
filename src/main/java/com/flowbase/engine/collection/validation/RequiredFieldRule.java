package com.flowbase.engine.collection.validation;

import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.exception.MissingRequiredFieldException;
import org.springframework.stereotype.Component;

@Component
public class RequiredFieldRule implements ValidationRule {
    @Override
    public void validate(CollectionField field, Object value) throws MissingRequiredFieldException {
        if (field.required() && (value == null || value instanceof String str && str.isBlank()))
            throw new MissingRequiredFieldException(field.name());
    }
}
