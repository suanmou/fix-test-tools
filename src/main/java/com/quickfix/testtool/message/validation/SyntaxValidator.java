package com.quickfix.testtool.message.validation;

import quickfix.*;
import quickfix.field.MsgType;
import java.util.ArrayList;
import java.util.List;

/**
 * FIX消息语法验证器
 */
public class SyntaxValidator implements MessageValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SyntaxValidator.class);
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            // 验证消息头
            validateHeader(message, details);
            
            // 验证消息体
            validateBody(message, details);
            
            // 验证消息尾
            validateTrailer(message, details);
            
            if (details.isEmpty()) {
                return ValidationResult.success();
            } else {
                return ValidationResult.failure("Syntax validation failed", details);
            }
            
        } catch (Exception e) {
            details.add("Unexpected error: " + e.getMessage());
            return ValidationResult.failure("Syntax validation error", details);
        }
    }
    
    private void validateHeader(Message message, List<String> details) {
        try {
            // 验证必需的头字段
            if (!message.getHeader().isSetField(quickfix.field.BeginString.FIELD)) {
                details.add("Missing BeginString(8) in header");
            }
            
            if (!message.getHeader().isSetField(quickfix.field.BodyLength.FIELD)) {
                details.add("Missing BodyLength(9) in header");
            }
            
            if (!message.getHeader().isSetField(quickfix.field.MsgType.FIELD)) {
                details.add("Missing MsgType(35) in header");
            }
            
            if (!message.getHeader().isSetField(quickfix.field.SenderCompID.FIELD)) {
                details.add("Missing SenderCompID(49) in header");
            }
            
            if (!message.getHeader().isSetField(quickfix.field.TargetCompID.FIELD)) {
                details.add("Missing TargetCompID(56) in header");
            }
            
            if (!message.getHeader().isSetField(quickfix.field.MsgSeqNum.FIELD)) {
                details.add("Missing MsgSeqNum(34) in header");
            }
            
        } catch (FieldNotFound e) {
            details.add("Header field validation error: " + e.getMessage());
        }
    }
    
    private void validateBody(Message message, List<String> details) {
        try {
            // 根据消息类型验证必需字段
            String msgType = message.getHeader().getString(quickfix.field.MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.LOGON:
                    validateLogonMessage(message, details);
                    break;
                case MsgType.HEARTBEAT:
                    validateHeartbeatMessage(message, details);
                    break;
                case MsgType.TEST_REQUEST:
                    validateTestRequestMessage(message, details);
                    break;
                case MsgType.NEW_ORDER_SINGLE:
                    validateNewOrderSingleMessage(message, details);
                    break;
                case MsgType.EXECUTION_REPORT:
                    validateExecutionReportMessage(message, details);
                    break;
                default:
                    log.debug("No specific validation for message type: {}", msgType);
            }
            
        } catch (FieldNotFound e) {
            details.add("Message type validation error: " + e.getMessage());
        }
    }
    
    private void validateTrailer(Message message, List<String> details) {
        try {
            if (!message.getTrailer().isSetField(quickfix.field.CheckSum.FIELD)) {
                details.add("Missing CheckSum(10) in trailer");
            }
        } catch (FieldNotFound e) {
            details.add("Trailer field validation error: " + e.getMessage());
        }
    }
    
    private void validateLogonMessage(Message message, List<String> details) {
        try {
            if (!message.isSetField(quickfix.field.EncryptMethod.FIELD)) {
                details.add("Missing EncryptMethod(98) in Logon message");
            }
            
            if (!message.isSetField(quickfix.field.HeartBtInt.FIELD)) {
                details.add("Missing HeartBtInt(108) in Logon message");
            }
            
        } catch (FieldNotFound e) {
            details.add("Logon message validation error: " + e.getMessage());
        }
    }
    
    private void validateHeartbeatMessage(Message message, List<String> details) {
        // Heartbeat消息没有必需字段
    }
    
    private void validateTestRequestMessage(Message message, List<String> details) {
        try {
            if (!message.isSetField(quickfix.field.TestReqID.FIELD)) {
                details.add("Missing TestReqID(112) in TestRequest message");
            }
        } catch (FieldNotFound e) {
            details.add("TestRequest message validation error: " + e.getMessage());
        }
    }
    
    private void validateNewOrderSingleMessage(Message message, List<String> details) {
        try {
            if (!message.isSetField(quickfix.field.ClOrdID.FIELD)) {
                details.add("Missing ClOrdID(11) in NewOrderSingle message");
            }
            
            if (!message.isSetField(quickfix.field.Side.FIELD)) {
                details.add("Missing Side(54) in NewOrderSingle message");
            }
            
            if (!message.isSetField(quickfix.field.TransactTime.FIELD)) {
                details.add("Missing TransactTime(60) in NewOrderSingle message");
            }
            
            if (!message.isSetField(quickfix.field.OrdType.FIELD)) {
                details.add("Missing OrdType(40) in NewOrderSingle message");
            }
            
        } catch (FieldNotFound e) {
            details.add("NewOrderSingle message validation error: " + e.getMessage());
        }
    }
    
    private void validateExecutionReportMessage(Message message, List<String> details) {
        try {
            if (!message.isSetField(quickfix.field.OrderID.FIELD)) {
                details.add("Missing OrderID(37) in ExecutionReport message");
            }
            
            if (!message.isSetField(quickfix.field.ExecID.FIELD)) {
                details.add("Missing ExecID(17) in ExecutionReport message");
            }
            
            if (!message.isSetField(quickfix.field.ExecType.FIELD)) {
                details.add("Missing ExecType(150) in ExecutionReport message");
            }
            
            if (!message.isSetField(quickfix.field.OrdStatus.FIELD)) {
                details.add("Missing OrdStatus(39) in ExecutionReport message");
            }
            
        } catch (FieldNotFound e) {
            details.add("ExecutionReport message validation error: " + e.getMessage());
        }
    }
    
    @Override
    public String getName() {
        return "SyntaxValidator";
    }
    
    @Override
    public int getPriority() {
        return 1; // 高优先级，最先执行
    }
}