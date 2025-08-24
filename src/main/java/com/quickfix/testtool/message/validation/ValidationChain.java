package com.quickfix.testtool.message.validation;

import quickfix.Message;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 验证器链
 */
public class ValidationChain {
    private final List<MessageValidator> validators = new ArrayList<>();
    
    public ValidationChain() {
        // 按优先级排序添加验证器
        registerValidator(new SyntaxValidator());
        registerValidator(new FieldValueValidator());
        registerValidator(new BusinessLogicValidator());
        registerValidator(new TimingValidator());
    }
    
    public void registerValidator(MessageValidator validator) {
        validators.add(validator);
        validators.sort(Comparator.comparingInt(MessageValidator::getPriority));
    }
    
    public List<ValidationResult> validate(Message message, ValidationContext context) {
        List<ValidationResult> results = new ArrayList<>();
        
        for (MessageValidator validator : validators) {
            ValidationResult result = validator.validate(message, context);
            results.add(result);
            
            // 如果语法验证失败，跳过后续验证
            if (!result.isValid() && result.getLevel() == ValidationLevel.SYNTAX) {
                break;
            }
        }
        
        return results;
    }
    
    public boolean validateAll(Message message, ValidationContext context) {
        return validate(message, context).stream()
                .allMatch(ValidationResult::isValid);
    }
}

/**
 * 验证器工厂
 */
public class ValidatorFactory {
    private static final Map<String, MessageValidator> validators = new ConcurrentHashMap<>();
    
    static {
        validators.put("syntax", new SyntaxValidator());
        validators.put("field", new FieldValueValidator());
        validators.put("business", new BusinessLogicValidator());
        validators.put("timing", new TimingValidator());
    }
    
    public static MessageValidator getValidator(String type) {
        return validators.get(type.toLowerCase());
    }
    
    public static ValidationChain createDefaultChain() {
        return new ValidationChain();
    }
    
    public static ValidationChain createCustomChain(String... validatorTypes) {
        ValidationChain chain = new ValidationChain();
        for (String type : validatorTypes) {
            MessageValidator validator = getValidator(type);
            if (validator != null) {
                chain.registerValidator(validator);
            }
        }
        return chain;
    }
}