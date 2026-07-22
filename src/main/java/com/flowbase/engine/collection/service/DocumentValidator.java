package com.flowbase.engine.collection.service;

import com.flowbase.engine.collection.domain.Collection;
import com.flowbase.engine.collection.domain.CollectionField;
import com.flowbase.engine.collection.validation.ValidationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentValidator {
    private final List<ValidationRule> validationRules;

    public void validate(Collection collection, Map<String, Object> payload) {
        for (CollectionField field : collection.fields()) {
            Object value = payload.get(field.name());
            for (ValidationRule rule : this.validationRules) {
                rule.validate(field, value);
            }
        }
    }
}
