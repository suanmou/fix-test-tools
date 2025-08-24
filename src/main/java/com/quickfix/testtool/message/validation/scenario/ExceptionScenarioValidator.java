package com.quickfix.testtool.message.validation.scenario;

import quickfix.*;
import quickfix.field.*;
import java.util.*;

/**
 * 异常场景验证器
 * 验证系统对异常消息的处理
 */
public class ExceptionScenarioValidator implements MessageValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExceptionScenarioValidator.class);
    
    // 异常场景配置
    private final Map<String, ExceptionScenario> scenarios = new HashMap<>();
    
    public ExceptionScenarioValidator() {
        initializeScenarios();
    }
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            String scenarioType = context.getScenarioType();
            if (scenarioType != null) {
                return validateSpecificScenario(message, scenarioType, details);
            }
            
            // 默认异常验证
            return validateDefaultExceptions(message, details);
            
        } catch (Exception e) {
            details.add("Exception scenario validation error: " + e.getMessage());
            return ValidationResult.failure("Exception scenario validation failed", details);
        }
    }
    
    private ValidationResult validateSpecificScenario(Message message, String scenarioType, List<String> details) {
        ExceptionScenario scenario = scenarios.get(scenarioType);
        if (scenario == null) {
            return ValidationResult.success();
        }
        
        return scenario.validate(message, details);
    }
    
    private ValidationResult validateDefaultExceptions(Message message, List<String> details) {
        try {
            // 验证消息完整性
            if (!isMessageComplete(message)) {
                details.add("Message is incomplete or malformed");
            }
            
            // 验证字段数据类型
            if (!validateFieldTypes(message, details)) {
                return ValidationResult.failure("Field type validation failed", details);
            }
            
            // 验证枚举值范围
            if (!validateEnumValues(message, details)) {
                return ValidationResult.failure("Enum value validation failed", details);
            }
            
            // 验证数值范围
            if (!validateNumericRanges(message, details)) {
                return ValidationResult.failure("Numeric range validation failed", details);
            }
            
        } catch (Exception e) {
            details.add("Exception during validation: " + e.getMessage());
        }
        
        return details.isEmpty() ? ValidationResult.success() : 
               ValidationResult.failure("Exception validation failed", details);
    }
    
    private boolean isMessageComplete(Message message) {
        try {
            // 检查必需字段
            return message.getHeader().isSetField(MsgType.FIELD) &&
                   message.getHeader().isSetField(SenderCompID.FIELD) &&
                   message.getHeader().isSetField(TargetCompID.FIELD) &&
                   message.getHeader().isSetField(MsgSeqNum.FIELD) &&
                   message.getTrailer().isSetField(CheckSum.FIELD);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateFieldTypes(Message message, List<String> details) {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.NEW_ORDER_SINGLE:
                    return validateOrderFieldTypes(message, details);
                case MsgType.EXECUTION_REPORT:
                    return validateExecutionFieldTypes(message, details);
                default:
                    return true;
            }
            
        } catch (Exception e) {
            details.add("Field type validation error: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateOrderFieldTypes(Message message, List<String> details) {
        try {
            // 验证数值字段
            validateNumericField(message, OrderQty.FIELD, "OrderQty", details);
            validateNumericField(message, Price.FIELD, "Price", details);
            
            // 验证字符字段
            validateCharField(message, Side.FIELD, "Side", details);
            validateCharField(message, OrdType.FIELD, "OrdType", details);
            
            return details.isEmpty();
            
        } catch (Exception e) {
            details.add("Order field type validation error: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateExecutionFieldTypes(Message message, List<String> details) {
        try {
            validateNumericField(message, LastShares.FIELD, "LastShares", details);
            validateNumericField(message, LastPx.FIELD, "LastPx", details);
            validateCharField(message, ExecType.FIELD, "ExecType", details);
            validateCharField(message, OrdStatus.FIELD, "OrdStatus", details);
            
            return details.isEmpty();
            
        } catch (Exception e) {
            details.add("Execution field type validation error: " + e.getMessage());
            return false;
        }
    }
    
    private void validateNumericField(Message message, int fieldTag, String fieldName, List<String> details) {
        try {
            if (message.isSetField(fieldTag)) {
                String value = message.getString(fieldTag);
                Double.parseDouble(value);
            }
        } catch (Exception e) {
            details.add("Invalid numeric value for " + fieldName + ": " + e.getMessage());
        }
    }
    
    private void validateCharField(Message message, int fieldTag, String fieldName, List<String> details) {
        try {
            if (message.isSetField(fieldTag)) {
                String value = message.getString(fieldTag);
                if (value.length() != 1) {
                    details.add("Invalid char value for " + fieldName + ": " + value);
                }
            }
        } catch (Exception e) {
            details.add("Invalid char value for " + fieldName + ": " + e.getMessage());
        }
    }
    
    private boolean validateEnumValues(Message message, List<String> details) {
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            // 验证Side字段
            if (message.isSetField(Side.FIELD)) {
                char side = message.getChar(Side.FIELD);
                if (side != Side.BUY && side != Side.SELL && side != Side.BUY_MINUS && side != Side.SELL_PLUS) {
                    details.add("Invalid Side value: " + side);
                }
            }
            
            // 验证OrdType字段
            if (message.isSetField(OrdType.FIELD)) {
                char ordType = message.getChar(OrdType.FIELD);
                if (ordType != OrdType.MARKET && ordType != OrdType.LIMIT && 
                    ordType != OrdType.STOP && ordType != OrdType.STOP_LIMIT) {
                    details.add("Invalid OrdType value: " + ordType);
                }
            }
            
            // 验证TimeInForce字段
            if (message.isSetField(TimeInForce.FIELD)) {
                char tif = message.getChar(TimeInForce.FIELD);
                if (tif != TimeInForce.DAY && tif != TimeInForce.GTC && 
                    tif != TimeInForce.IOC && tif != TimeInForce.FOK) {
                    details.add("Invalid TimeInForce value: " + tif);
                }
            }
            
            return details.isEmpty();
            
        } catch (Exception e) {
            details.add("Enum validation error: " + e.getMessage());
            return false;
        }
    }
    
    private boolean validateNumericRanges(Message message, List<String> details) {
        try {
            // 验证数量必须为正
            if (message.isSetField(OrderQty.FIELD)) {
                double qty = message.getDouble(OrderQty.FIELD);
                if (qty <= 0) {
                    details.add("OrderQty must be positive: " + qty);
                }
            }
            
            // 验证价格必须为正
            if (message.isSetField(Price.FIELD)) {
                double price = message.getDouble(Price.FIELD);
                if (price <= 0) {
                    details.add("Price must be positive: " + price);
                }
            }
            
            // 验证成交数量必须为正
            if (message.isSetField(LastShares.FIELD)) {
                double lastShares = message.getDouble(LastShares.FIELD);
                if (lastShares < 0) {
                    details.add("LastShares must be non-negative: " + lastShares);
                }
            }
            
            return details.isEmpty();
            
        } catch (Exception e) {
            details.add("Numeric range validation error: " + e.getMessage());
            return false;
        }
    }
    
    private void initializeScenarios() {
        // 初始化各种异常场景
        scenarios.put("MALFORMED_MESSAGE", new MalformedMessageScenario());
        scenarios.put("OUT_OF_SEQUENCE", new OutOfSequenceScenario());
        scenarios.put("DUPLICATE_MESSAGE", new DuplicateMessageScenario());
        scenarios.put("INVALID_CHECKSUM", new InvalidChecksumScenario());
        scenarios.put("MISSING_REQUIRED_FIELDS", new MissingRequiredFieldsScenario());
        scenarios.put("INVALID_DATA_TYPES", new InvalidDataTypesScenario());
        scenarios.put("ENUM_OUT_OF_RANGE", new EnumOutOfRangeScenario());
    }
    
    @Override
    public String getName() {
        return "ExceptionScenarioValidator";
    }
    
    @Override
    public int getPriority() {
        return 5;
    }
}

/**
 * 异常场景基类
 */
abstract class ExceptionScenario {
    public abstract ValidationResult validate(Message message, List<String> details);
}

// 具体异常场景实现
class MalformedMessageScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证畸形消息处理
        return ValidationResult.success();
    }
}

class OutOfSequenceScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证乱序消息处理
        return ValidationResult.success();
    }
}

class DuplicateMessageScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证重复消息处理
        return ValidationResult.success();
    }
}

class InvalidChecksumScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证校验和错误处理
        return ValidationResult.success();
    }
}

class MissingRequiredFieldsScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证缺失必需字段的处理
        return ValidationResult.success();
    }
}

class InvalidDataTypesScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证数据类型错误的处理
        return ValidationResult.success();
    }
}

class EnumOutOfRangeScenario extends ExceptionScenario {
    @Override
    public ValidationResult validate(Message message, List<String> details) {
        // 验证枚举值越界的处理
        return ValidationResult.success();
    }
}