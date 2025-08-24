package com.quickfix.testtool.message.validation;

import quickfix.Message;
import quickfix.SessionID;
import java.util.List;

/**
 * 消息验证器接口
 */
public interface MessageValidator {
    ValidationResult validate(Message message, ValidationContext context);
    String getName();
    int getPriority();
}

/**
 * 验证上下文，包含验证所需的环境信息
 */
public class ValidationContext {
    private final SessionID sessionId;
    private final Message expectedMessage;
    private final long timestamp;
    private final ValidationLevel level;
    
    public ValidationContext(SessionID sessionId, Message expectedMessage, ValidationLevel level) {
        this.sessionId = sessionId;
        this.expectedMessage = expectedMessage;
        this.level = level;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public SessionID getSessionId() { return sessionId; }
    public Message getExpectedMessage() { return expectedMessage; }
    public long getTimestamp() { return timestamp; }
    public ValidationLevel getLevel() { return level; }
}

/**
 * 验证结果
 */
public class ValidationResult {
    private final boolean valid;
    private final String message;
    private final ValidationLevel level;
    private final List<String> details;
    
    public ValidationResult(boolean valid, String message, ValidationLevel level, List<String> details) {
        this.valid = valid;
        this.message = message;
        this.level = level;
        this.details = details;
    }
    
    public static ValidationResult success() {
        return new ValidationResult(true, "Validation passed", ValidationLevel.INFO, List.of());
    }
    
    public static ValidationResult failure(String message, List<String> details) {
        return new ValidationResult(false, message, ValidationLevel.ERROR, details);
    }
    
    // Getters
    public boolean isValid() { return valid; }
    public String getMessage() { return message; }
    public ValidationLevel getLevel() { return level; }
    public List<String> getDetails() { return details; }
}

/**
 * 验证级别
 */
public enum ValidationLevel {
    SYNTAX, SEMANTIC, BUSINESS, TIMING
}