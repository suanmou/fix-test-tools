package com.quickfix.testtool.message.validation;

import quickfix.*;
import quickfix.field.*;

public class ValidationExample {
    public static void main(String[] args) {
        try {
            // 创建验证链
            ValidationChain chain = ValidatorFactory.createDefaultChain();
            
            // 创建测试消息
            Message newOrder = new Message();
            newOrder.getHeader().setField(new MsgType(MsgType.NEW_ORDER_SINGLE));
            newOrder.setField(new ClOrdID("ORDER-123"));
            newOrder.setField(new Side(Side.BUY));
            newOrder.setField(new OrderQty(1000.0));
            newOrder.setField(new OrdType(OrdType.LIMIT));
            newOrder.setField(new Price(150.50));
            newOrder.setField(new TransactTime(new java.util.Date()));
            
            // 创建预期消息
            Message expected = new Message();
            expected.getHeader().setField(new MsgType(MsgType.NEW_ORDER_SINGLE));
            expected.setField(new ClOrdID("ORDER-123"));
            expected.setField(new Side(Side.BUY));
            
            // 执行验证
            ValidationContext context = new ValidationContext(
                new SessionID("FIX.4.4:INITIATOR->ACCEPTOR"),
                expected,
                ValidationLevel.BUSINESS
            );
            
            List<ValidationResult> results = chain.validate(newOrder, context);
            
            // 输出验证结果
            for (ValidationResult result : results) {
                System.out.println("Validator: " + result.getMessage());
                System.out.println("Valid: " + result.isValid());
                if (!result.getDetails().isEmpty()) {
                    result.getDetails().forEach(System.out::println);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}