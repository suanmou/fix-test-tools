package com.quickfix.testtool.core.engine;

import com.quickfix.testtool.core.model.TestScenario;
import com.quickfix.testtool.core.model.TestStep;
import com.quickfix.testtool.core.model.TestStep.ExpectedResult;
import com.quickfix.testtool.simulator.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.*;
import quickfix.field.MsgType;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestEngine {
    private static final Logger log = LoggerFactory.getLogger(TestEngine.class);
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    
    public TestResult executeScenario(TestScenario scenario) {
        log.info("Starting test scenario: {}", scenario.getName());
        TestResult result = new TestResult(scenario.getId());
        
        try {
            // 初始化模拟器
            FIXSimulator simulator = createSimulator(scenario);
            simulator.start();
            
            // 执行测试序列
            for (TestStep step : scenario.getSequence()) {
                executeStep(step, simulator, result);
                if (result.getStatus() == TestStatus.FAILED) {
                    break;
                }
            }
            
            simulator.stop();
            if (result.getStatus() != TestStatus.FAILED) {
                result.setStatus(TestStatus.PASSED);
            }
            
        } catch (Exception e) {
            log.error("Test execution failed", e);
            result.setStatus(TestStatus.FAILED);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    private FIXSimulator createSimulator(TestScenario scenario) {
        // 根据配置创建合适的模拟器
        if ("initiator".equalsIgnoreCase(scenario.getConfiguration().getRole())) {
            return new InitiatorSimulator();
        } else {
            return new AcceptorSimulator();
        }
    }
    
    private void executeStep(TestStep step, FIXSimulator simulator, TestResult result) {
        log.debug("Executing step: {}", step.getAction());
        
        try {
            switch (step.getAction()) {
                case "WAIT_LOGIN":
                    waitForLogin(step, simulator, result);
                    break;
                case "SEND_HEARTBEAT":
                    sendHeartbeat(step, simulator, result);
                    break;
                case "SEND_TEST_REQUEST":
                    sendTestRequest(step, simulator, result);
                    break;
                case "WAIT_MESSAGE":
                    waitForMessage(step, simulator, result);
                    break;
                case "VERIFY_SESSION_STATE":
                    verifySessionState(step, simulator, result);
                    break;
                // 订单相关操作
                case "SEND_NEW_ORDER":
                    sendNewOrder(step, simulator, result);
                    break;
                case "CANCEL_ORDER":
                    cancelOrder(step, simulator, result);
                    break;
                case "MODIFY_ORDER":
                    modifyOrder(step, simulator, result);
                    break;
                case "QUERY_ORDER_STATUS":
                    queryOrderStatus(step, simulator, result);
                    break;
                case "WAIT_EXECUTION_REPORT":
                    waitForExecutionReport(step, simulator, result);
                    break;
                case "WAIT_ORDER_CANCEL_RESPONSE":
                    waitForOrderCancelResponse(step, simulator, result);
                    break;
                case "WAIT_ORDER_MODIFY_RESPONSE":
                    waitForOrderModifyResponse(step, simulator, result);
                    break;
                default:
                    log.warn("Unknown action: {}", step.getAction());
                    result.addFailure("Unknown action: " + step.getAction());
            }
        } catch (Exception e) {
            log.error("Step execution failed: {}", step.getAction(), e);
            result.addFailure("Step " + step.getAction() + " failed: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void waitForLogin(TestStep step, FIXSimulator simulator, TestResult result) {
        log.info("Waiting for login completion...");
        long timeout = step.getTimeout() > 0 ? step.getTimeout() : 10000; // 默认10秒
        AtomicBoolean loggedIn = new AtomicBoolean(false);
        
        try {
            CompletableFuture<Void> loginFuture = CompletableFuture.runAsync(() -> {
                while (!loggedIn.get() && !Thread.currentThread().isInterrupted()) {
                    try {
                        SessionID sessionId = simulator.getSessionId();
                        if (sessionId != null) {
                            Session session = Session.lookupSession(sessionId);
                            if (session != null && session.isLoggedOn()) {
                                loggedIn.set(true);
                                break;
                            }
                        }
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            loginFuture.get(timeout, TimeUnit.MILLISECONDS);
            
            if (!loggedIn.get()) {
                result.addFailure("Login timeout after " + timeout + "ms");
                result.setStatus(TestStatus.FAILED);
            } else {
                log.info("Login completed successfully");
            }
            
        } catch (TimeoutException e) {
            result.addFailure("Login timeout after " + timeout + "ms");
            result.setStatus(TestStatus.FAILED);
        } catch (Exception e) {
            result.addFailure("Login wait failed: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void sendHeartbeat(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            log.info("Sending heartbeat message...");
            Message heartbeat = new Message();
            
            // 设置消息头
            heartbeat.getHeader().setField(new quickfix.field.MsgType(MsgType.HEARTBEAT));
            
            // 添加TestReqID（如果提供）
            Map<String, String> params = step.getParameters();
            if (params != null && params.containsKey("testReqID")) {
                heartbeat.setField(new quickfix.field.TestReqID(params.get("testReqID")));
            }
            
            simulator.sendMessage(heartbeat);
            log.debug("Heartbeat sent successfully");
            
            // 验证响应
            if (step.getExpected() != null) {
                verifyResponse(step.getExpected(), simulator, result);
            }
            
        } catch (Exception e) {
            log.error("Failed to send heartbeat", e);
            result.addFailure("Failed to send heartbeat: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void sendTestRequest(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            log.info("Sending test request...");
            Message testRequest = new Message();
            
            testRequest.getHeader().setField(new quickfix.field.MsgType(MsgType.TEST_REQUEST));
            
            Map<String, String> params = step.getParameters();
            if (params != null && params.containsKey("testReqID")) {
                testRequest.setField(new quickfix.field.TestReqID(params.get("testReqID")));
            } else {
                testRequest.setField(new quickfix.field.TestReqID("TEST_" + System.currentTimeMillis()));
            }
            
            simulator.sendMessage(testRequest);
            log.debug("Test request sent successfully");
            
            if (step.getExpected() != null) {
                verifyResponse(step.getExpected(), simulator, result);
            }
            
        } catch (Exception e) {
            log.error("Failed to send test request", e);
            result.addFailure("Failed to send test request: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
     private void sendNewOrder(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            log.info("Sending new order...");
            Map<String, String> params = step.getParameters();
            if (params == null) {
                result.addFailure("Missing parameters for new order");
                result.setStatus(TestStatus.FAILED);
                return;
            }
            
            NewOrderSingle order = new NewOrderSingle();
            
            // 设置订单字段
            order.set(new ClOrdID(params.getOrDefault("clOrdID", "ORDER_" + System.currentTimeMillis())));
            order.set(new Symbol(params.getOrDefault("symbol", "AAPL")));
            order.set(new Side(params.getOrDefault("side", "1").charAt(0)));
            order.set(new TransactTime(new Date()));
            order.set(new OrdType(params.getOrDefault("ordType", "1").charAt(0)));
            order.set(new OrderQty(Double.parseDouble(params.getOrDefault("quantity", "100"))));
            
            if (params.containsKey("price")) {
                order.set(new Price(Double.parseDouble(params.get("price"))));
            }
            if (params.containsKey("timeInForce")) {
                order.set(new TimeInForce(params.get("timeInForce").charAt(0)));
            }
            
            simulator.sendMessage(order);
            log.debug("New order sent successfully");
            
            if (step.getExpected() != null) {
                verifyResponse(step.getExpected(), simulator, result);
            }
            
        } catch (Exception e) {
            log.error("Failed to send new order", e);
            result.addFailure("Failed to send new order: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void cancelOrder(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            log.info("Canceling order...");
            Map<String, String> params = step.getParameters();
            if (params == null || !params.containsKey("origClOrdID")) {
                result.addFailure("Missing origClOrdID parameter for cancel order");
                result.setStatus(TestStatus.FAILED);
                return;
            }
            
            OrderCancelRequest cancelRequest = new OrderCancelRequest();
            
            cancelRequest.set(new OrigClOrdID(params.get("origClOrdID")));
            cancelRequest.set(new ClOrdID(params.getOrDefault("clOrdID", "CANCEL_" + System.currentTimeMillis())));
            cancelRequest.set(new Symbol(params.getOrDefault("symbol", "AAPL")));
            cancelRequest.set(new Side(params.getOrDefault("side", "1").charAt(0)));
            cancelRequest.set(new TransactTime(new Date()));
            cancelRequest.set(new OrderQty(Double.parseDouble(params.getOrDefault("quantity", "100"))));
            
            simulator.sendMessage(cancelRequest);
            log.debug("Order cancel request sent successfully");
            
            if (step.getExpected() != null) {
                verifyResponse(step.getExpected(), simulator, result);
            }
            
        } catch (Exception e) {
            log.error("Failed to cancel order", e);
            result.addFailure("Failed to cancel order: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void modifyOrder(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            log.info("Modifying order...");
            Map<String, String> params = step.getParameters();
            if (params == null || !params.containsKey("origClOrdID")) {
                result.addFailure("Missing origClOrdID parameter for modify order");
                result.setStatus(TestStatus.FAILED);
                return;
            }
            
            OrderCancelReplaceRequest modifyRequest = new OrderCancelReplaceRequest();
            
            modifyRequest.set(new OrigClOrdID(params.get("origClOrdID")));
            modifyRequest.set(new ClOrdID(params.getOrDefault("clOrdID", "MODIFY_" + System.currentTimeMillis())));
            modifyRequest.set(new Symbol(params.getOrDefault("symbol", "AAPL")));
            modifyRequest.set(new Side(params.getOrDefault("side", "1").charAt(0)));
            modifyRequest.set(new TransactTime(new Date()));
            modifyRequest.set(new OrderQty(Double.parseDouble(params.getOrDefault("quantity", "100"))));
            
            if (params.containsKey("price")) {
                modifyRequest.set(new Price(Double.parseDouble(params.get("price"))));
            }
            
            simulator.sendMessage(modifyRequest);
            log.debug("Order modify request sent successfully");
            
            if (step.getExpected() != null) {
                verifyResponse(step.getExpected(), simulator, result);
            }
            
        } catch (Exception e) {
            log.error("Failed to modify order", e);
            result.addFailure("Failed to modify order: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void queryOrderStatus(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            log.info("Querying order status...");
            Map<String, String> params = step.getParameters();
            if (params == null || !params.containsKey("orderID")) {
                result.addFailure("Missing orderID parameter for order status query");
                result.setStatus(TestStatus.FAILED);
                return;
            }
            
            OrderStatusRequest statusRequest = new OrderStatusRequest();
            
            statusRequest.set(new OrderID(params.get("orderID")));
            statusRequest.set(new Symbol(params.getOrDefault("symbol", "AAPL")));
            statusRequest.set(new Side(params.getOrDefault("side", "1").charAt(0)));
            
            simulator.sendMessage(statusRequest);
            log.debug("Order status request sent successfully");
            
            if (step.getExpected() != null) {
                verifyResponse(step.getExpected(), simulator, result);
            }
            
        } catch (Exception e) {
            log.error("Failed to query order status", e);
            result.addFailure("Failed to query order status: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void waitForExecutionReport(TestStep step, FIXSimulator simulator, TestResult result) {
        long timeout = step.getTimeout() > 0 ? step.getTimeout() : 5000;
        Map<String, String> params = step.getParameters();
        
        try {
            log.info("Waiting for execution report...");
            CompletableFuture<Message> messageFuture = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    Message message = simulator.getLastReceivedMessage();
                    if (message != null && message instanceof ExecutionReport) {
                        if (matchesExpected(message, params)) {
                            return message;
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            });
            
            Message receivedMessage = messageFuture.get(timeout, TimeUnit.MILLISECONDS);
            
            if (receivedMessage == null) {
                result.addFailure("No execution report received within timeout");
                result.setStatus(TestStatus.FAILED);
            } else {
                log.info("Execution report received: {}", receivedMessage);
            }
            
        } catch (Exception e) {
            result.addFailure("Failed to wait for execution report: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void waitForOrderCancelResponse(TestStep step, FIXSimulator simulator, TestResult result) {
        long timeout = step.getTimeout() > 0 ? step.getTimeout() : 5000;
        Map<String, String> params = step.getParameters();
        
        try {
            log.info("Waiting for order cancel response...");
            CompletableFuture<Message> messageFuture = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    Message message = simulator.getLastReceivedMessage();
                    if (message != null && (message instanceof OrderCancelReject || message instanceof ExecutionReport)) {
                        if (matchesExpected(message, params)) {
                            return message;
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            });
            
            Message receivedMessage = messageFuture.get(timeout, TimeUnit.MILLISECONDS);
            
            if (receivedMessage == null) {
                result.addFailure("No order cancel response received within timeout");
                result.setStatus(TestStatus.FAILED);
            } else {
                log.info("Order cancel response received: {}", receivedMessage);
            }
            
        } catch (Exception e) {
            result.addFailure("Failed to wait for order cancel response: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void waitForOrderModifyResponse(TestStep step, FIXSimulator simulator, TestResult result) {
        long timeout = step.getTimeout() > 0 ? step.getTimeout() : 5000;
        Map<String, String> params = step.getParameters();
        
        try {
            log.info("Waiting for order modify response...");
            CompletableFuture<Message> messageFuture = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    Message message = simulator.getLastReceivedMessage();
                    if (message != null && (message instanceof OrderCancelReject || message instanceof ExecutionReport)) {
                        if (matchesExpected(message, params)) {
                            return message;
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            });
            
            Message receivedMessage = messageFuture.get(timeout, TimeUnit.MILLISECONDS);
            
            if (receivedMessage == null) {
                result.addFailure("No order modify response received within timeout");
                result.setStatus(TestStatus.FAILED);
            } else {
                log.info("Order modify response received: {}", receivedMessage);
            }
            
        } catch (Exception e) {
            result.addFailure("Failed to wait for order modify response: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    private void waitForMessage(TestStep step, FIXSimulator simulator, TestResult result) {
        long timeout = step.getTimeout() > 0 ? step.getTimeout() : 5000;
        Map<String, String> params = step.getParameters();
        
        try {
            log.info("Waiting for message...");
            CompletableFuture<Message> messageFuture = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    Message message = simulator.getLastReceivedMessage();
                    if (message != null && matchesExpected(message, params)) {
                        return message;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return null;
            });
            
            Message receivedMessage = messageFuture.get(timeout, TimeUnit.MILLISECONDS);
            
            if (receivedMessage == null) {
                result.addFailure("No matching message received within timeout");
                result.setStatus(TestStatus.FAILED);
            } else {
                log.info("Expected message received: {}", receivedMessage);
            }
            
        } catch (Exception e) {
            result.addFailure("Failed to wait for message: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void verifySessionState(TestStep step, FIXSimulator simulator, TestResult result) {
        try {
            Map<String, String> params = step.getParameters();
            if (params == null || !params.containsKey("expectedState")) {
                result.addFailure("Missing expectedState parameter");
                result.setStatus(TestStatus.FAILED);
                return;
            }
            
            String expectedState = params.get("expectedState");
            SessionID sessionId = simulator.getSessionId();
            Session session = sessionId != null ? Session.lookupSession(sessionId) : null;
            
            boolean isLoggedOn = session != null && session.isLoggedOn();
            
            switch (expectedState.toUpperCase()) {
                case "LOGGED_ON":
                    if (!isLoggedOn) {
                        result.addFailure("Expected session to be logged on, but it's not");
                        result.setStatus(TestStatus.FAILED);
                    }
                    break;
                case "DISCONNECTED":
                    if (isLoggedOn) {
                        result.addFailure("Expected session to be disconnected, but it's logged on");
                        result.setStatus(TestStatus.FAILED);
                    }
                    break;
                default:
                    result.addFailure("Unknown session state: " + expectedState);
                    result.setStatus(TestStatus.FAILED);
            }
            
        } catch (Exception e) {
            result.addFailure("Failed to verify session state: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private void verifyResponse(ExpectedResult expected, FIXSimulator simulator, TestResult result) {
        long timeout = expected.getTimeout() > 0 ? expected.getTimeout() : 5000;
        
        try {
            CompletableFuture<Boolean> verificationFuture = CompletableFuture.supplyAsync(() -> {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < timeout) {
                    Message response = simulator.getLastReceivedMessage();
                    if (response != null && validateMessage(response, expected)) {
                        return true;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                return false;
            });
            
            boolean success = verificationFuture.get(timeout, TimeUnit.MILLISECONDS);
            
            if (!success) {
                result.addFailure("Response validation failed or timeout");
                result.setStatus(TestStatus.FAILED);
            } else {
                log.debug("Response validation passed");
            }
            
        } catch (Exception e) {
            result.addFailure("Response verification failed: " + e.getMessage());
            result.setStatus(TestStatus.FAILED);
        }
    }
    
    private boolean validateMessage(Message message, ExpectedResult expected) {
        if (expected == null) return true;
        
        try {
            // 验证消息类型
            if (expected.getMessageType() != null) {
                String actualMsgType = message.getHeader().getString(MsgType.FIELD);
                if (!expected.getMessageType().equals(actualMsgType)) {
                    log.debug("Message type mismatch: expected={}, actual={}", 
                             expected.getMessageType(), actualMsgType);
                    return false;
                }
            }
            
            // 验证字段值
            if (expected.getFields() != null) {
                for (Map.Entry<String, String> entry : expected.getFields().entrySet()) {
                    int fieldTag = Integer.parseInt(entry.getKey());
                    String expectedValue = entry.getValue();
                    
                    if (!message.isSetField(fieldTag)) {
                        log.debug("Missing expected field: {}", fieldTag);
                        return false;
                    }
                    
                    String actualValue = message.getString(fieldTag);
                    if (!expectedValue.equals(actualValue)) {
                        log.debug("Field value mismatch: tag={}, expected={}, actual={}", 
                                 fieldTag, expectedValue, actualValue);
                        return false;
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating message", e);
            return false;
        }
    }
    
    private boolean matchesExpected(Message message, Map<String, String> params) {
        if (params == null || params.isEmpty()) return true;
        
        try {
            // 检查消息类型
            if (params.containsKey("msgType")) {
                String expectedType = params.get("msgType");
                String actualType = message.getHeader().getString(MsgType.FIELD);
                if (!expectedType.equals(actualType)) {
                    return false;
                }
            }
            
            // 检查其他字段
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if ("msgType".equals(entry.getKey())) continue;
                
                try {
                    int fieldTag = Integer.parseInt(entry.getKey());
                    String expectedValue = entry.getValue();
                    String actualValue = message.getString(fieldTag);
                    
                    if (!expectedValue.equals(actualValue)) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid field tag: {}", entry.getKey());
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error matching message", e);
            return false;
        }
    }
    
    public void shutdown() {
        executor.shutdown();
        scheduler.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// 测试结果类
class TestResult {
    private final String scenarioId;
    private TestStatus status;
    private String errorMessage;
    private final List<String> failures = new ArrayList<>();
    private final long startTime = System.currentTimeMillis();
    private long endTime;
    
    public TestResult(String scenarioId) {
        this.scenarioId = scenarioId;
        this.status = TestStatus.RUNNING;
    }
    
    public void addFailure(String failure) {
        failures.add(failure);
    }
    
    public void complete() {
        this.endTime = System.currentTimeMillis();
    }
    
    public long getDuration() {
        return (endTime > 0 ? endTime : System.currentTimeMillis()) - startTime;
    }
    
    // Getters and setters
    public String getScenarioId() { return scenarioId; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public List<String> getFailures() { return new ArrayList<>(failures); }
    public boolean hasFailures() { return !failures.isEmpty(); }
}

enum TestStatus {
    RUNNING, PASSED, FAILED
}