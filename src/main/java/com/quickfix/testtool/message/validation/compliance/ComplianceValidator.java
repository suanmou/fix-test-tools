package com.quickfix.testtool.message.validation.compliance;

import quickfix.*;
import quickfix.field.*;
import java.util.*;

/**
 * 合规性验证器
 * 验证交易合规性要求
 */
public class ComplianceValidator implements MessageValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ComplianceValidator.class);
    
    // 合规规则配置
    private final Map<String, ComplianceRule> rules = new HashMap<>();
    
    public ComplianceValidator() {
        initializeRules();
    }
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            // 根据消息类型应用相应的合规规则
            List<ComplianceRule> applicableRules = getApplicableRules(msgType);
            
            for (ComplianceRule rule : applicableRules) {
                ValidationResult result = rule.validate(message, context);
                if (!result.isValid()) {
                    details.addAll(result.getDetails());
                }
            }
            
            return details.isEmpty() ? ValidationResult.success() : 
                   ValidationResult.failure("Compliance validation failed", details);
            
        } catch (Exception e) {
            details.add("Compliance validation error: " + e.getMessage());
            return ValidationResult.failure("Compliance validation failed", details);
        }
    }
    
    private List<ComplianceRule> getApplicableRules(String msgType) {
        List<ComplianceRule> applicableRules = new ArrayList<>();
        
        // 所有消息都需要基础合规检查
        applicableRules.add(rules.get("BASIC_AUTHENTICATION"));
        
        // 根据消息类型添加特定规则
        switch (msgType) {
            case MsgType.NEW_ORDER_SINGLE:
                applicableRules.add(rules.get("ORDER_SIZE_LIMIT"));
                applicableRules.add(rules.get("PRICE_LIMIT"));
                applicableRules.add(rules.get("TRADING_HOURS"));
                applicableRules.add(rules.get("SYMBOL_VALIDATION"));
                break;
            case MsgType.ORDER_CANCEL_REQUEST:
                applicableRules.add(rules.get("CANCEL_PERMISSION"));
                break;
            case MsgType.ORDER_CANCEL_REPLACE_REQUEST:
                applicableRules.add(rules.get("MODIFY_PERMISSION"));
                applicableRules.add(rules.get("ORDER_SIZE_LIMIT"));
                break;
        }
        
        return applicableRules;
    }
    
    private void initializeRules() {
        rules.put("BASIC_AUTHENTICATION", new BasicAuthenticationRule());
        rules.put("ORDER_SIZE_LIMIT", new OrderSizeLimitRule());
        rules.put("PRICE_LIMIT", new PriceLimitRule());
        rules.put("TRADING_HOURS", new TradingHoursRule());
        rules.put("SYMBOL_VALIDATION", new SymbolValidationRule());
        rules.put("CANCEL_PERMISSION", new CancelPermissionRule());
        rules.put("MODIFY_PERMISSION", new ModifyPermissionRule());
    }
    
    @Override
    public String getName() {
        return "ComplianceValidator";
    }
    
    @Override
    public int getPriority() {
        return 6;
    }
}

/**
 * 合规规则接口
 */
interface ComplianceRule {
    ValidationResult validate(Message message, ValidationContext context);
    String getRuleName();
}

/**
 * 基础认证规则
 */
class BasicAuthenticationRule implements ComplianceRule {
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            // 验证发送方身份
            String senderCompId = message.getHeader().getString(SenderCompID.FIELD);
            if (!isValidSender(senderCompId)) {
                details.add("Invalid sender: " + senderCompId);
            }
            
            // 验证目标方身份
            String targetCompId = message.getHeader().getString(TargetCompID.FIELD);
            if (!isValidTarget(targetCompId)) {
                details.add("Invalid target: " + targetCompId);
            }
            
        } catch (FieldNotFound e) {
            details.add("Authentication fields missing: " + e.getMessage());
        }
        
        return details.isEmpty() ? ValidationResult.success() : 
               ValidationResult.failure("Authentication failed", details);
    }
    
    private boolean isValidSender(String senderCompId) {
        // 实现具体的身份验证逻辑
        return senderCompId != null && !senderCompId.trim().isEmpty();
    }
    
    private boolean isValidTarget(String targetCompId) {
        // 实现具体的身份验证逻辑
        return targetCompId != null && !targetCompId.trim().isEmpty();
    }
    
    @Override
    public String getRuleName() {
        return "BasicAuthenticationRule";
    }
}

/**
 * 订单数量限制规则
 */
class OrderSizeLimitRule implements ComplianceRule {
    private static final double MIN_ORDER_SIZE = 100.0;
    private static final double MAX_ORDER_SIZE = 1000000.0;
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            if (message.isSetField(OrderQty.FIELD)) {
                double orderQty = message.getDouble(OrderQty.FIELD);
                
                if (orderQty < MIN_ORDER_SIZE) {
                    details.add("Order size below minimum: " + orderQty + " < " + MIN_ORDER_SIZE);
                }
                
                if (orderQty > MAX_ORDER_SIZE) {
                    details.add("Order size exceeds maximum: " + orderQty + " > " + MAX_ORDER_SIZE);
                }
            }
            
        } catch (FieldNotFound e) {
            details.add("OrderQty field missing: " + e.getMessage());
        }
        
        return details.isEmpty() ? ValidationResult.success() : 
               ValidationResult.failure("Order size limit violated", details);
    }
    
    @Override
    public String getRuleName() {
        return "OrderSizeLimitRule";
    }
}

/**
 * 价格限制规则
 */
class PriceLimitRule implements ComplianceRule {
    private static final double MIN_PRICE = 0.01;
    private static final double MAX_PRICE = 10000.0;
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            if (message.isSetField(Price.FIELD)) {
                double price = message.getDouble(Price.FIELD);
                
                if (price < MIN_PRICE) {
                    details.add("Price below minimum: " + price + " < " + MIN_PRICE);
                }
                
                if (price > MAX_PRICE) {
                    details.add("Price exceeds maximum: " + price + " > " + MAX_PRICE);
                }
            }
            
        } catch (FieldNotFound e) {
            details.add("Price field missing: " + e.getMessage());
        }
        
        return details.isEmpty() ? ValidationResult.success() : 
               ValidationResult.failure("Price limit violated", details);
    }
    
    @Override
    public String getRuleName() {
        return "PriceLimitRule";
    }
}

/**
 * 交易时间规则
 */
class TradingHoursRule implements ComplianceRule {
    private static final int OPEN_HOUR = 9;
    private static final int CLOSE_HOUR = 17;
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            if (message.isSetField(TransactTime.FIELD)) {
                java.util.Date transactTime = message.getUtcTimeStamp(TransactTime.FIELD);
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.setTime(transactTime);
                
                int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
                
                if (hour < OPEN_HOUR || hour >= CLOSE_HOUR) {
                    details.add("Order placed outside trading hours: " + hour);
                }
            }
            
        } catch (FieldNotFound e) {
            details.add("TransactTime field missing: " + e.getMessage());
        }
        
        return details.isEmpty() ? ValidationResult.success() : 
               ValidationResult.failure("Trading hours violation", details);
    }
    
    @Override
    public String getRuleName() {
        return "TradingHoursRule";
    }
}

/**
 * 交易品种验证规则
 */
class SymbolValidationRule implements ComplianceRule {
    private static final Set<String> VALID_SYMBOLS = Set.of("AAPL", "GOOGL", "MSFT", "TSLA", "AMZN");
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            if (message.isSetField(Symbol.FIELD)) {
                String symbol = message.getString(Symbol.FIELD);
                
                if (!VALID_SYMBOLS.contains(symbol)) {
                    details.add("Invalid symbol: " + symbol);
                }
            }
            
        } catch (FieldNotFound e) {
            details.add("Symbol field missing: " + e.getMessage());
        }
        
        return details.isEmpty() ? ValidationResult.success() : 
               ValidationResult.failure("Symbol validation failed", details);
    }
    
    @Override
    public String getRuleName() {
        return "SymbolValidationRule";
    }
}

/**
 * 取消权限规则
 */
class CancelPermissionRule implements ComplianceRule {
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        // 实现取消权限检查
        return ValidationResult.success();
    }
    
    @Override
    public String getRuleName() {
        return "CancelPermissionRule";
    }
}

/**
 * 修改权限规则
 */
class ModifyPermissionRule implements ComplianceRule {
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        // 实现修改权限检查
        return ValidationResult.success();
    }
    
    @Override
    public String getRuleName() {
        return "ModifyPermissionRule";
    }
}