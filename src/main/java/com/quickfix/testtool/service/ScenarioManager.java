package com.quickfix.testtool.service;

import com.quickfix.testtool.core.engine.TestEngine;
import com.quickfix.testtool.core.engine.TestResult;
import com.quickfix.testtool.core.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场景管理服务
 */
public class ScenarioManager {
    private static final Logger log = LoggerFactory.getLogger(ScenarioManager.class);
    
    private final JsonScenarioStorage storage;
    private final TestEngine testEngine;
    private final Map<String, ExecutionRecord> executions = new ConcurrentHashMap<>();
    
    public ScenarioManager(TestEngine testEngine) {
        this.storage = new JsonScenarioStorage();
        this.testEngine = testEngine;
    }
    
    /**
     * 创建新场景
     */
    public TestScenario createScenario(String name, String description, List<Map<String, Object>> steps) {
        TestScenario scenario = new TestScenario();
        scenario.setId(UUID.randomUUID().toString());
        scenario.setName(name);
        scenario.setDescription(description);
        
        // 设置默认配置
        Configuration config = new Configuration();
        config.setQuickfixConfig("config/quickfix.cfg");
        config.setInitialSeqNum(1);
        config.setLoginState("LOGGED_IN");
        scenario.setConfiguration(config);
        
        // 转换步骤
        List<TestStep> testSteps = new ArrayList<>();
        for (Map<String, Object> stepData : steps) {
            TestStep step = new TestStep();
            step.setAction((String) stepData.get("action"));
            step.setParameters((Map<String, Object>) stepData.get("parameters"));
            step.setTimeout(Long.parseLong(stepData.getOrDefault("timeout", 5000).toString()));
            
            ExpectedResult expected = new ExpectedResult();
            Map<String, Object> expectedData = (Map<String, Object>) stepData.get("expected");
            if (expectedData != null) {
                expected.setSessionState((String) expectedData.get("sessionState"));
                expected.setMessageType((String) expectedData.get("messageType"));
                expected.setFields((Map<String, Object>) expectedData.get("fields"));
            }
            step.setExpected(expected);
            
            testSteps.add(step);
        }
        scenario.setSequence(testSteps);
        
        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createdAt", LocalDateTime.now().toString());
        metadata.put("createdBy", "user");
        metadata.put("category", "regression");
        metadata.put("tags", Arrays.asList("heartbeat", "basic"));
        scenario.setMetadata(metadata);
        
        storage.saveScenario(scenario);
        log.info("Created scenario: {} ({})", name, scenario.getId());
        return scenario;
    }
    
    /**
     * 执行场景
     */
    public String executeScenario(String scenarioId, boolean async) {
        Optional<TestScenario> scenarioOpt = storage.loadScenario(scenarioId);
        if (!scenarioOpt.isPresent()) {
            throw new IllegalArgumentException("Scenario not found: " + scenarioId);
        }
        
        TestScenario scenario = scenarioOpt.get();
        String executionId = UUID.randomUUID().toString();
        
        ExecutionRecord record = new ExecutionRecord();
        record.executionId = executionId;
        record.scenarioId = scenarioId;
        record.status = "RUNNING";
        record.startTime = LocalDateTime.now();
        executions.put(executionId, record);
        
        if (async) {
            CompletableFuture.runAsync(() -> {
                executeAndRecord(record, scenario);
            });
        } else {
            executeAndRecord(record, scenario);
        }
        
        return executionId;
    }
    
    /**
     * 获取执行状态
     */
    public ExecutionRecord getExecutionStatus(String executionId) {
        return executions.get(executionId);
    }
    
    /**
     * 获取所有场景
     */
    public List<TestScenario> getAllScenarios() {
        return storage.loadAllScenarios();
    }
    
    /**
     * 按ID获取场景
     */
    public Optional<TestScenario> getScenario(String id) {
        return storage.loadScenario(id);
    }
    
    /**
     * 删除场景
     */
    public boolean deleteScenario(String id) {
        return storage.deleteScenario(id);
    }
    
    private void executeAndRecord(ExecutionRecord record, TestScenario scenario) {
        try {
            TestResult result = testEngine.executeScenario(scenario);
            record.result = result;
            record.status = result.getStatus().name();
            record.endTime = LocalDateTime.now();
            log.info("Scenario execution completed: {} - {}", record.executionId, record.status);
        } catch (Exception e) {
            record.status = "FAILED";
            record.error = e.getMessage();
            record.endTime = LocalDateTime.now();
            log.error("Scenario execution failed: " + record.executionId, e);
        }
    }
    
    /**
     * 执行记录类
     */
    public static class ExecutionRecord {
        public String executionId;
        public String scenarioId;
        public String status;
        public TestResult result;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String error;
    }
}