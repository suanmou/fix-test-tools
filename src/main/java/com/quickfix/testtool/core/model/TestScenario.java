package com.quickfix.testtool.core.model;

import java.util.List;
import java.util.Map;

public class TestScenario {
    private String id;
    private String name;
    private String description;
    private Map<String, Object> metadata;
    private Configuration configuration;
    private List<TestStep> sequence;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public Configuration getConfiguration() { return configuration; }
    public void setConfiguration(Configuration configuration) { this.configuration = configuration; }
    
    public List<TestStep> getSequence() { return sequence; }
    public void setSequence(List<TestStep> sequence) { this.sequence = sequence; }
}

class Configuration {
    private String quickfixConfig;
    private int initialSeqNum;
    private String loginState;
    
    // Getters and setters
    public String getQuickfixConfig() { return quickfixConfig; }
    public void setQuickfixConfig(String quickfixConfig) { this.quickfixConfig = quickfixConfig; }
    
    public int getInitialSeqNum() { return initialSeqNum; }
    public void setInitialSeqNum(int initialSeqNum) { this.initialSeqNum = initialSeqNum; }
    
    public String getLoginState() { return loginState; }
    public void setLoginState(String loginState) { this.loginState = loginState; }
}

class TestStep {
    private String action;
    private Map<String, Object> parameters;
    private long timeout;
    private ExpectedResult expected;
    
    // Getters and setters
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
    
    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }
    
    public ExpectedResult getExpected() { return expected; }
    public void setExpected(ExpectedResult expected) { this.expected = expected; }
}

class ExpectedResult {
    private String sessionState;
    private String messageType;
    private Map<String, Object> fields;
    
    // Getters and setters
    public String getSessionState() { return sessionState; }
    public void setSessionState(String sessionState) { this.sessionState = sessionState; }
    
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    
    public Map<String, Object> getFields() { return fields; }
    public void setFields(Map<String, Object> fields) { this.fields = fields; }
}