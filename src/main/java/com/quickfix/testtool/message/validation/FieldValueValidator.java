package com.quickfix.testtool.message.validation;

import quickfix.*;
import quickfix.field.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 字段值验证器
 */
public class FieldValueValidator implements MessageValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FieldValueValidator.class);
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            Message expected = context.getExpectedMessage();
            if (expected == null) {
                return ValidationResult.success();
            }
            
            // 验证消息类型
            validateMessageType(message, expected, details);
            
            // 验证头字段
            validateHeaderFields(message.getHeader(), expected.getHeader(), details);
            
            // 验证消息体字段
            validateBodyFields(message, expected, details);
            
            // 验证尾字段
            validateTrailerFields(message.getTrailer(), expected.getTrailer(), details);
            
            if (details.isEmpty()) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure("Field value validation failed", details);
            }
            
        } catch (Exception e) {
            details.add("Unexpected error: " + e.getMessage());
            return ValidationResult.failure("Field value validation error", details);
        }
    }
    
    private void validateMessageType(Message message, Message expected, List<String> details) {
        try {
            String actualType = message.getHeader().getString(MsgType.FIELD);
            String expectedType = expected.getHeader().getString(MsgType.FIELD);
            
            if (!actualType.equals(expectedType)) {
                details.add(String.format("Message type mismatch: expected=%s, actual=%s", 
                        expectedType, actualType));
            }
        } catch (FieldNotFound e) {
            details.add("Message type field not found");
        }
    }
    
    private void validateHeaderFields(Message.Header actual, Message.Header expected, List<String> details) {
        validateFields(actual, expected, "header", details);
    }
    
    private void validateBodyFields(Message actual, Message expected, List<String> details) {
        validateFields(actual, expected, "body", details);
    }
    
    private void validateTrailerFields(Message.Trailer actual, Message.Trailer expected, List<String> details) {
        validateFields(actual, expected, "trailer", details);
    }
    
    private void validateFields(FieldMap actual, FieldMap expected, String section, List<String> details) {
        try {
            for (int fieldTag : expected.getFieldOrder()) {
                if (actual.isSetField(fieldTag)) {
                    String actualValue = actual.getString(fieldTag);
                    String expectedValue = expected.getString(fieldTag);
                    
                    if (!actualValue.equals(expectedValue)) {
                        details.add(String.format("%s field mismatch [tag=%d]: expected=%s, actual=%s",
                                section, fieldTag, expectedValue, actualValue));
                    }
                } else {
                    details.add(String.format("Missing %s field [tag=%d]", section, fieldTag));
                }
            }
        } catch (FieldNotFound e) {
            details.add("Field validation error: " + e.getMessage());
        }
    }
    
    @Override
    public String getName() {
        return "FieldValueValidator";
    }
    
    @Override
    public int getPriority() {
        return 2;
    }
}