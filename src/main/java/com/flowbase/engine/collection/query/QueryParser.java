package com.flowbase.engine.collection.query;

import com.flowbase.engine.collection.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryParser {
    private static final Pattern BRACKET_PATTERN = Pattern.compile("^(\\w+)\\[(\\w+)]$");
    
    public QueryContext parse(Map<String, String[]> parameterMap) {
        String sortBy = "";
        int limit = 0, offset = 0;
        List<QueryFilter> filters = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if (values == null || values.length == 0 || (values.length == 1 && values[0].isBlank())) continue;
            String rawValue = values[0];
            if (key.equals("_sort")) {
                sortBy = rawValue;
                continue;
            }
            if (key.equals("_limit")) {
                limit = Integer.parseInt(rawValue);
                continue;
            }
            if (key.equals("_offset")) {
                offset = Integer.parseInt(rawValue);
                continue;
            }
            Matcher matcher = BRACKET_PATTERN.matcher(key);
            String fieldName;
            FilterOperator operator;
            if (matcher.matches()) {
                fieldName = matcher.group(1);
                String operatorCode = matcher.group(2);
                try {
                    operator = FilterOperator.valueOf(operatorCode.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Unsupported filter operator: [" + operatorCode + "]");
                }
            } else {
                fieldName = key;
                operator = FilterOperator.EQ;
            }
            filters.add(new QueryFilter(fieldName, operator, rawValue));
        }
        return new QueryContext(filters, sortBy, limit, offset);
    }
}
