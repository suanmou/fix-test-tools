package com.quickfix.testtool.simulator;

import quickfix.*;
import quickfix.field.MsgType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public interface FIXSimulator {
    void start() throws ConfigError;
    void stop();
    void sendMessage(Message message) throws SessionNotFound;
    void setBehavior(BehaviorConfiguration config);
}

// Acceptor模拟器实现
class AcceptorSimulator implements quickfix.Application, FIXSimulator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AcceptorSimulator.class);
    
    private SocketAcceptor acceptor;
    private SessionSettings settings;
    private BehaviorConfiguration behavior;
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();
    
    @Override
    public void start() throws ConfigError {
        settings = new SessionSettings("config/acceptor.cfg");
        MessageStoreFactory storeFactory = new FileStoreFactory(settings);
        LogFactory logFactory = new FileLogFactory(settings);
        MessageFactory messageFactory = new DefaultMessageFactory();
        
        acceptor = new SocketAcceptor(this, storeFactory, settings, logFactory, messageFactory);
        acceptor.start();
        log.info("Acceptor simulator started");
    }
    
    @Override
    public void stop() {
        if (acceptor != null) {
            acceptor.stop();
            log.info("Acceptor simulator stopped");
        }
    }
    
    @Override
    public void sendMessage(Message message) throws SessionNotFound {
        // 实现消息发送逻辑
    }
    
    @Override
    public void setBehavior(BehaviorConfiguration config) {
        this.behavior = config;
    }
    
    // Application接口实现
    @Override
    public void onCreate(SessionID sessionId) {
        log.info("Session created: {}", sessionId);
    }
    
    @Override
    public void onLogon(SessionID sessionId) {
        log.info("Session logged on: {}", sessionId);
    }
    
    @Override
    public void onLogout(SessionID sessionId) {
        log.info("Session logged out: {}", sessionId);
    }
    
    @Override
    public void toAdmin(Message message, SessionID sessionId) {
        log.debug("Sending admin message: {}", message);
    }
    
    @Override
    public void fromAdmin(Message message, SessionID sessionId) {
        log.debug("Received admin message: {}", message);
    }
    
    @Override
    public void toApp(Message message, SessionID sessionId) throws DoNotSend {
        log.debug("Sending app message: {}", message);
    }
    
    @Override
    public void fromApp(Message message, SessionID sessionId) throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
        log.info("Received app message: {}", message);
        if (behavior != null) {
            ResponseAction action = behavior.determineResponse(message);
            processResponseAction(action, sessionId);
        }
    }
    
    private void processResponseAction(ResponseAction action, SessionID sessionId) {
        if (action != null && action.getResponseMessage() != null) {
            try {
                Session.sendToTarget(action.getResponseMessage(), sessionId);
            } catch (SessionNotFound e) {
                log.error("Failed to send response", e);
            }
        }
    }
}

// 行为配置接口
interface BehaviorConfiguration {
    ResponseAction determineResponse(Message receivedMessage);
    boolean shouldInitiateAction();
    Message createInitiationMessage();
}

// 响应动作
class ResponseAction {
    private Message responseMessage;
    private int delay;
    
    public ResponseAction(Message responseMessage, int delay) {
        this.responseMessage = responseMessage;
        this.delay = delay;
    }
    
    public Message getResponseMessage() { return responseMessage; }
    public int getDelay() { return delay; }
}