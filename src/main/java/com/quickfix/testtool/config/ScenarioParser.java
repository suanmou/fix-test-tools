package com.quickfix.testtool.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.quickfix.testtool.core.model.TestScenario;
import java.io.File;
import java.io.IOException;

public class ScenarioParser {
    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    
    public static TestScenario parseFromYaml(String filePath) throws IOException {
        return mapper.readValue(new File(filePath), TestScenario.class);
    }
    
    public static TestScenario parseFromJson(String filePath) throws IOException {
        return mapper.readValue(new File(filePath), TestScenario.class);
    }
}