package com.quickfix.testtool;

import com.quickfix.testtool.config.ScenarioParser;
import com.quickfix.testtool.core.engine.TestEngine;
import com.quickfix.testtool.core.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuickFixTestTool {
    private static final Logger log = LoggerFactory.getLogger(QuickFixTestTool.class);
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java QuickFixTestTool <scenario-file.yml>");
            System.exit(1);
        }
        
        try {
            // 解析测试场景
            TestScenario scenario = ScenarioParser.parseFromYaml(args[0]);
            log.info("Loaded test scenario: {}", scenario.getName());
            
            // 创建测试引擎并执行
            TestEngine engine = new TestEngine();
            TestResult result = engine.executeScenario(scenario);
            
            // 输出结果
            log.info("Test completed: {} - {}", 
                    scenario.getName(), 
                    result.getStatus());
            
            System.exit(result.getStatus() == TestStatus.PASSED ? 0 : 1);
            
        } catch (Exception e) {
            log.error("Test execution failed", e);
            System.exit(1);
        }
    }
}