package com.quickfix.testtool.message.validation;

import quickfix.*;
import quickfix.field.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 业务逻辑验证器
 */
public class BusinessLogicValidator implements MessageValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BusinessLogicValidator.class);
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.NEW_ORDER_SINGLE:
                    return validateNewOrderSingle(message, context);
                case MsgType.EXECUTION_REPORT:
                    return validateExecutionReport(message, context);
                case MsgType.ORDER_CANCEL_REQUEST:
                    return validateOrderCancelRequest(message, context);
                default:
                    return ValidationResult.success();
            }
            
        } catch (Exception e) {
            details.add("Business logic validation error: " + e.getMessage());
            return ValidationResult.failure("Business logic validation failed", details);
        }
    }
    
    private ValidationResult validateNewOrderSingle(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            // 验证订单数量必须为正数
            double orderQty = message.getDouble(OrderQty.FIELD);
            if (orderQty <= 0) {
                details.add("Order quantity must be positive: " + orderQty);
            }
            
            // 验证价格必须为正数（对于市价订单除外）
            char ordType = message.getChar(OrdType.FIELD);
            if (ordType == OrdType.LIMIT || ordType == OrdType.STOP) {
                if (message.isSetField(Price.FIELD)) {
                    double price = message.getDouble(Price.FIELD);
                    if (price <= 0) {
                        details.add("Price must be positive for limit/stop orders: " + price);
                    }
                } else {
                    details.add("Price is required for limit/stop orders");
                }
            }
            
            // 验证交易方向
            char side = message.getChar(Side.FIELD);
            if (side != Side.BUY && side != Side.SELL) {
                details.add("Invalid side value: " + side);
            }
            
        } catch (FieldNotFound e) {
            details.add("Required field missing: " + e.getMessage());
        }
        
        if (details.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure("NewOrderSingle business validation failed", details);
        }
    }
    
    private ValidationResult validateExecutionReport(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            // 验证执行类型和订单状态的一致性
            char execType = message.getChar(ExecType.FIELD);
            char ordStatus = message.getChar(OrdStatus.FIELD);
            
            if (!isValidExecTypeOrdStatusCombination(execType, ordStatus)) {
                details.add(String.format("Invalid ExecType/OrdStatus combination: %c/%c", execType, ordStatus));
            }
            
            // 验证成交数量不超过订单数量
            if (message.isSetField(LastShares.FIELD) && message.isSetField(OrderQty.FIELD)) {
                double lastShares = message.getDouble(LastShares.FIELD);
                double orderQty = message.getDouble(OrderQty.FIELD);
                
                if (lastShares > orderQty) {
                    details.add(String.format("Last shares (%f) exceeds order quantity (%f)", lastShares, orderQty));
                }
            }
            
            // 验证成交金额为正
            if (message.isSetField(LastPx.FIELD)) {
                double lastPx = message.getDouble(LastPx.FIELD);
                if (lastPx <= 0) {
                    details.add("Last price must be positive: " + lastPx);
                }
            }
            
        } catch (FieldNotFound e) {
            details.add("Required field missing: " + e.getMessage());
        }
        
        if (details.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure("ExecutionReport business validation failed", details);
        }
    }
    
    private ValidationResult validateOrderCancelRequest(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            // 验证取消数量不超过原始订单数量
            if (message.isSetField(OrderQty.FIELD)) {
                double orderQty = message.getDouble(OrderQty.FIELD);
                if (orderQty <= 0) {
                    details.add("Cancel quantity must be positive: " + orderQty);
                }
            }
            
            // 验证原始订单ID存在
            if (!message.isSetField(OrigClOrdID.FIELD)) {
                details.add("Missing OrigClOrdID(41) in OrderCancelRequest");
            }
            
        } catch (FieldNotFound e) {
            details.add("Required field missing: " + e.getMessage());
        }
        
        if (details.isEmpty()) {
            return ValidationResult.success();
        } else {
            return ValidationResult.failure("OrderCancelRequest business validation failed", details);
        }
    }
    
    private boolean isValidExecTypeOrdStatusCombination(char execType, char ordStatus) {
        // 根据FIX协议规范验证ExecType和OrdStatus的合法组合
        switch (execType) {
            case ExecType.NEW:
                return ordStatus == OrdStatus.NEW;
            case ExecType.PARTIAL_FILL:
                return ordStatus == OrdStatus.PARTIALLY_FILLED;
            case ExecType.FILL:
                return ordStatus == OrdStatus.FILLED;
            case ExecType.CANCELED:
                return ordStatus == OrdStatus.CANCELED;
            case ExecType.REPLACED:
                return ordStatus == OrdStatus.REPLACED;
            case ExecType.REJECTED:
                return ordStatus == OrdStatus.REJECTED;
            default:
                return true; // 其他组合默认允许
        }
    }
    
    @Override
    public String getName() {
        return "BusinessLogicValidator";
    }
    
    @Override
    public int getPriority() {
        return 3;
    }
}