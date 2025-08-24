package com.quickfix.testtool.message.validation;

import com.quickfix.testtool.message.validation.scenario.*;
import com.quickfix.testtool.message.validation.compliance.*;

/**
 * 增强的验证器工厂
 */
public class EnhancedValidatorFactory extends ValidatorFactory {
    
    static {
        // 注册所有验证器
        registerValidator("orderLifecycle", new OrderLifecycleValidator());
        registerValidator("exceptionScenario", new ExceptionScenarioValidator());
        registerValidator("compliance", new ComplianceValidator());
    }
    
    public static ValidationChain createBusinessScenarioChain() {
        ValidationChain chain = new ValidationChain();
        chain.registerValidator(new OrderLifecycleValidator());
        chain.registerValidator(new BusinessLogicValidator());
        chain.registerValidator(new ComplianceValidator());
        return chain;
    }
    
    public static ValidationChain createExceptionScenarioChain() {
        ValidationChain chain = new ValidationChain();
        chain.registerValidator(new SyntaxValidator());
        chain.registerValidator(new ExceptionScenarioValidator());
        chain.registerValidator(new FieldValueValidator());
        return chain;
    }
    
    public static ValidationChain createFullValidationChain() {
        ValidationChain chain = new ValidationChain();
        chain.registerValidator(new SyntaxValidator());
        chain.registerValidator(new FieldValueValidator());
        chain.registerValidator(new BusinessLogicValidator());
        chain.registerValidator(new OrderLifecycleValidator());
        chain.registerValidator(new ExceptionScenarioValidator());
        chain.registerValidator(new ComplianceValidator());
        return chain;
    }
}