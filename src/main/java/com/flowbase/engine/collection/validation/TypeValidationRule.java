package com.flowbase.engine.collection.validation;

import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class TypeValidationRule implements ValidationRule {
    @Override
    public void validate(CollectionField field, Object value) throws ValidationException {
        if (value == null) return;
        boolean match = switch (field.type()) {
            case STRING -> value instanceof String;
            case NUMBER -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case DATETIME -> {
                if (value instanceof String) {
                    try {
                        Instant.parse((CharSequence) value);
                        yield true;
                    } catch (Exception e) {
                        yield false;
                    }
                }
                yield value instanceof Date || value instanceof Instant;
            }
        }; if (!match)
            throw new ValidationException("Type mismatch for: " + field.name() + " Expected Type: " + field.type() +
                    " but received: " + value
                                                                                                                                                .getClass());
    }
}
