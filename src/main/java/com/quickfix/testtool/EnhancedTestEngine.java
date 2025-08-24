package com.quickfix.testtool.core.engine;

import com.quickfix.testtool.message.validation.*;
import quickfix.Message;

public class EnhancedTestEngine extends TestEngine {
    private final ValidationChain validationChain = ValidatorFactory.createDefaultChain();
    
    @Override
    protected void validateMessage(Message message, ValidationContext context) {
        List<ValidationResult> results = validationChain.validate(message, context);
        
        for (ValidationResult result : results) {
            if (!result.isValid()) {
                getCurrentResult().addValidationFailure(result);
            }
        }
    }
}