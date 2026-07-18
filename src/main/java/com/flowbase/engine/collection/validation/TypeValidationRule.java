package com.flowbase.engine.collection.validation;

import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.exception.FieldTypeMismatchException;
import com.flowbase.engine.collection.exception.MissingRequiredFieldException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;

@Component
public class TypeValidationRule implements ValidationRule {
    @Override
    public void validate(CollectionField field, Object value) throws MissingRequiredFieldException {
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
            throw new FieldTypeMismatchException(field.name(), field.type().toString(), value.getClass().getTypeName());
    }
}
