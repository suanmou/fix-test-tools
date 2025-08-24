package com.quickfix.testtool.message.validation.scenario;

import quickfix.*;
import quickfix.field.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单生命周期验证器
 * 验证订单从创建到完成/取消的完整状态转换
 */
public class OrderLifecycleValidator implements MessageValidator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderLifecycleValidator.class);
    
    // 订单状态跟踪
    private final Map<String, OrderState> orderStates = new ConcurrentHashMap<>();
    
    @Override
    public ValidationResult validate(Message message, ValidationContext context) {
        List<String> details = new ArrayList<>();
        
        try {
            String msgType = message.getHeader().getString(MsgType.FIELD);
            
            switch (msgType) {
                case MsgType.NEW_ORDER_SINGLE:
                    return validateNewOrder(message, details);
                case MsgType.ORDER_CANCEL_REQUEST:
                    return validateCancelRequest(message, details);
                case MsgType.ORDER_CANCEL_REPLACE_REQUEST:
                    return validateModifyRequest(message, details);
                case MsgType.EXECUTION_REPORT:
                    return validateExecutionReport(message, details);
                default:
                    return ValidationResult.success();
            }
            
        } catch (Exception e) {
            details.add("Order lifecycle validation error: " + e.getMessage());
            return ValidationResult.failure("Order lifecycle validation failed", details);
        }
    }
    
    private ValidationResult validateNewOrder(Message message, List<String> details) {
        try {
            String clOrdId = message.getString(ClOrdID.FIELD);
            
            // 检查订单ID是否已存在
            if (orderStates.containsKey(clOrdId)) {
                details.add("Duplicate order ID: " + clOrdId);
                return ValidationResult.failure("New order validation failed", details);
            }
            
            // 创建新的订单状态
            OrderState state = new OrderState(clOrdId);
            state.setOriginalQty(message.getDouble(OrderQty.FIELD));
            state.setOriginalPrice(message.getDouble(Price.FIELD));
            state.setSide(message.getChar(Side.FIELD));
            state.setStatus(OrdStatus.NEW);
            state.setTimestamp(System.currentTimeMillis());
            
            orderStates.put(clOrdId, state);
            
            log.info("Created new order: {}", clOrdId);
            return ValidationResult.success();
            
        } catch (FieldNotFound e) {
            details.add("Required field missing in new order: " + e.getMessage());
            return ValidationResult.failure("New order validation failed", details);
        }
    }
    
    private ValidationResult validateCancelRequest(Message message, List<String> details) {
        try {
            String clOrdId = message.getString(ClOrdID.FIELD);
            String origClOrdId = message.getString(OrigClOrdID.FIELD);
            
            // 检查原始订单是否存在
            OrderState originalState = orderStates.get(origClOrdId);
            if (originalState == null) {
                details.add("Original order not found: " + origClOrdId);
                return ValidationResult.failure("Cancel request validation failed", details);
            }
            
            // 检查订单状态是否允许取消
            if (!isCancellable(originalState.getStatus())) {
                details.add("Order cannot be cancelled in status: " + originalState.getStatus());
                return ValidationResult.failure("Cancel request validation failed", details);
            }
            
            // 创建取消订单状态
            OrderState cancelState = new OrderState(clOrdId);
            cancelState.setOriginalOrderId(origClOrdId);
            cancelState.setStatus(OrdStatus.PENDING_CANCEL);
            cancelState.setTimestamp(System.currentTimeMillis());
            
            orderStates.put(clOrdId, cancelState);
            
            log.info("Created cancel request: {} for order: {}", clOrdId, origClOrdId);
            return ValidationResult.success();
            
        } catch (FieldNotFound e) {
            details.add("Required field missing in cancel request: " + e.getMessage());
            return ValidationResult.failure("Cancel request validation failed", details);
        }
    }
    
    private ValidationResult validateModifyRequest(Message message, List<String> details) {
        try {
            String clOrdId = message.getString(ClOrdID.FIELD);
            String origClOrdId = message.getString(OrigClOrdID.FIELD);
            
            // 检查原始订单是否存在
            OrderState originalState = orderStates.get(origClOrdId);
            if (originalState == null) {
                details.add("Original order not found: " + origClOrdId);
                return ValidationResult.failure("Modify request validation failed", details);
            }
            
            // 检查订单状态是否允许修改
            if (!isModifiable(originalState.getStatus())) {
                details.add("Order cannot be modified in status: " + originalState.getStatus());
                return ValidationResult.failure("Modify request validation failed", details);
            }
            
            // 验证修改参数的合理性
            double newQty = message.getDouble(OrderQty.FIELD);
            double originalQty = originalState.getOriginalQty();
            
            if (newQty <= 0) {
                details.add("New quantity must be positive: " + newQty);
                return ValidationResult.failure("Modify request validation failed", details);
            }
            
            if (newQty > originalQty) {
                details.add("Cannot increase order quantity: " + newQty + " > " + originalQty);
                return ValidationResult.failure("Modify request validation failed", details);
            }
            
            // 创建修改订单状态
            OrderState modifyState = new OrderState(clOrdId);
            modifyState.setOriginalOrderId(origClOrdId);
            modifyState.setOriginalQty(originalQty);
            modifyState.setModifiedQty(newQty);
            modifyState.setStatus(OrdStatus.PENDING_REPLACE);
            modifyState.setTimestamp(System.currentTimeMillis());
            
            orderStates.put(clOrdId, modifyState);
            
            log.info("Created modify request: {} for order: {}", clOrdId, origClOrdId);
            return ValidationResult.success();
            
        } catch (FieldNotFound e) {
            details.add("Required field missing in modify request: " + e.getMessage());
            return ValidationResult.failure("Modify request validation failed", details);
        }
    }
    
    private ValidationResult validateExecutionReport(Message message, List<String> details) {
        try {
            String clOrdId = message.getString(ClOrdID.FIELD);
            String orderId = message.getString(OrderID.FIELD);
            char execType = message.getChar(ExecType.FIELD);
            char ordStatus = message.getChar(OrdStatus.FIELD);
            
            // 查找对应的订单状态
            OrderState orderState = findOrderState(clOrdId);
            if (orderState == null) {
                details.add("Order not found for execution report: " + clOrdId);
                return ValidationResult.failure("Execution report validation failed", details);
            }
            
            // 验证状态转换的合理性
            if (!isValidStatusTransition(orderState.getStatus(), ordStatus)) {
                details.add(String.format("Invalid status transition: %c -> %c", 
                        orderState.getStatus(), ordStatus));
                return ValidationResult.failure("Execution report validation failed", details);
            }
            
            // 验证成交数量的合理性
            if (message.isSetField(LastShares.FIELD)) {
                double lastShares = message.getDouble(LastShares.FIELD);
                double cumulativeQty = message.getDouble(CumQty.FIELD);
                double originalQty = orderState.getOriginalQty();
                
                if (lastShares < 0) {
                    details.add("Last shares must be non-negative: " + lastShares);
                }
                
                if (cumulativeQty > originalQty) {
                    details.add("Cumulative quantity exceeds original quantity: " + 
                            cumulativeQty + " > " + originalQty);
                }
                
                if (Math.abs(cumulativeQty - (orderState.getCumulativeQty() + lastShares)) > 0.001) {
                    details.add("Cumulative quantity calculation mismatch");
                }
            }
            
            // 验证价格的合理性
            if (message.isSetField(LastPx.FIELD)) {
                double lastPx = message.getDouble(LastPx.FIELD);
                if (lastPx <= 0) {
                    details.add("Last price must be positive: " + lastPx);
                }
            }
            
            // 更新订单状态
            orderState.setStatus(ordStatus);
            orderState.setOrderId(orderId);
            if (message.isSetField(CumQty.FIELD)) {
                orderState.setCumulativeQty(message.getDouble(CumQty.FIELD));
            }
            orderState.setLastUpdateTime(System.currentTimeMillis());
            
            log.info("Updated order state: {} -> {}", clOrdId, ordStatus);
            return ValidationResult.success();
            
        } catch (FieldNotFound e) {
            details.add("Required field missing in execution report: " + e.getMessage());
            return ValidationResult.failure("Execution report validation failed", details);
        }
    }
    
    private OrderState findOrderState(String clOrdId) {
        // 优先查找直接订单
        OrderState state = orderStates.get(clOrdId);
        if (state != null) {
            return state;
        }
        
        // 查找关联的原始订单
        for (OrderState order : orderStates.values()) {
            if (clOrdId.equals(order.getOriginalOrderId())) {
                return order;
            }
        }
        
        return null;
    }
    
    private boolean isCancellable(char status) {
        return status == OrdStatus.NEW || 
               status == OrdStatus.PARTIALLY_FILLED || 
               status == OrdStatus.PENDING_CANCEL;
    }
    
    private boolean isModifiable(char status) {
        return status == OrdStatus.NEW || 
               status == OrdStatus.PARTIALLY_FILLED || 
               status == OrdStatus.PENDING_REPLACE;
    }
    
    private boolean isValidStatusTransition(char fromStatus, char toStatus) {
        // 定义合法的状态转换
        Map<Character, Set<Character>> validTransitions = Map.of(
            OrdStatus.NEW, Set.of(OrdStatus.NEW, OrdStatus.PARTIALLY_FILLED, OrdStatus.FILLED, 
                                 OrdStatus.CANCELED, OrdStatus.PENDING_CANCEL, OrdStatus.PENDING_REPLACE),
            OrdStatus.PARTIALLY_FILLED, Set.of(OrdStatus.PARTIALLY_FILLED, OrdStatus.FILLED, 
                                              OrdStatus.CANCELED, OrdStatus.PENDING_CANCEL, OrdStatus.PENDING_REPLACE),
            OrdStatus.PENDING_CANCEL, Set.of(OrdStatus.CANCELED, OrdStatus.PARTIALLY_FILLED),
            OrdStatus.PENDING_REPLACE, Set.of(OrdStatus.REPLACED, OrdStatus.PARTIALLY_FILLED)
        );
        
        Set<Character> validTargets = validTransitions.getOrDefault(fromStatus, Set.of());
        return validTargets.contains(toStatus);
    }
    
    @Override
    public String getName() {
        return "OrderLifecycleValidator";
    }
    
    @Override
    public int getPriority() {
        return 4;
    }
    
    /**
     * 订单状态类
     */
    public static class OrderState {
        private final String clOrdId;
        private String orderId;
        private String originalOrderId;
        private double originalQty;
        private double modifiedQty;
        private double cumulativeQty = 0.0;
        private double originalPrice;
        private double modifiedPrice;
        private char side;
        private char status;
        private long timestamp;
        private long lastUpdateTime;
        
        public OrderState(String clOrdId) {
            this.clOrdId = clOrdId;
        }
        
        // Getters and setters
        public String getClOrdId() { return clOrdId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getOriginalOrderId() { return originalOrderId; }
        public void setOriginalOrderId(String originalOrderId) { this.originalOrderId = originalOrderId; }
        public double getOriginalQty() { return originalQty; }
        public void setOriginalQty(double originalQty) { this.originalQty = originalQty; }
        public double getModifiedQty() { return modifiedQty; }
        public void setModifiedQty(double modifiedQty) { this.modifiedQty = modifiedQty; }
        public double getCumulativeQty() { return cumulativeQty; }
        public void setCumulativeQty(double cumulativeQty) { this.cumulativeQty = cumulativeQty; }
        public double getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }
        public double getModifiedPrice() { return modifiedPrice; }
        public void setModifiedPrice(double modifiedPrice) { this.modifiedPrice = modifiedPrice; }
        public char getSide() { return side; }
        public void setSide(char side) { this.side = side; }
        public char getStatus() { return status; }
        public void setStatus(char status) { this.status = status; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(long lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }
}