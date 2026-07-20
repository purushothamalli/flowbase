package com.flowbase.engine.collection.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AclEvaluatorService {
    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();
    
    public boolean evaluate(String rule, Map<String, Object> documentData, AclAuthContext aclAuthContext) {
        if (rule == null || rule.isEmpty()) return true; try {
            Expression expression = this.expressionCache.computeIfAbsent(rule, this.parser::parseExpression);
            EvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
            context.setVariable("data", documentData);
            context.setVariable("auth", aclAuthContext != null ? aclAuthContext : new AclAuthContext("anonymous", "anonymous"));
            Boolean result = expression.getValue(context, Boolean.class); return result != null && result;
        } catch (Exception e) {
            log.error("SpEL ACL evaluation crashed for rule: '{}'", rule, e); return false;
        }
    }
}
