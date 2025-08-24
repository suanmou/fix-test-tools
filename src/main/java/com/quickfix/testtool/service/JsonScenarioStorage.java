package com.quickfix.testtool.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickfix.testtool.core.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON场景存储服务
 */
public class JsonScenarioStorage {
    private static final Logger log = LoggerFactory.getLogger(JsonScenarioStorage.class);
    private static final String SCENARIOS_DIR = "scenarios";
    private static final String JSON_EXTENSION = ".json";
    
    private final ObjectMapper objectMapper;
    private final Path scenariosPath;
    
    public JsonScenarioStorage() {
        this.objectMapper = new ObjectMapper();
        this.scenariosPath = Paths.get(SCENARIOS_DIR);
        initializeStorage();
    }
    
    private void initializeStorage() {
        try {
            if (!Files.exists(scenariosPath)) {
                Files.createDirectories(scenariosPath);
                log.info("Created scenarios directory: {}", scenariosPath);
            }
        } catch (IOException e) {
            log.error("Failed to create scenarios directory", e);
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }
    
    /**
     * 保存场景到JSON文件
     */
    public String saveScenario(TestScenario scenario) {
        try {
            if (scenario.getId() == null || scenario.getId().isEmpty()) {
                scenario.setId(UUID.randomUUID().toString());
            }
            
            File file = scenariosPath.resolve(scenario.getId() + JSON_EXTENSION).toFile();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, scenario);
            
            log.info("Saved scenario: {} to {}", scenario.getName(), file.getAbsolutePath());
            return scenario.getId();
        } catch (IOException e) {
            log.error("Failed to save scenario", e);
            throw new RuntimeException("Failed to save scenario", e);
        }
    }
    
    /**
     * 从JSON文件加载场景
     */
    public Optional<TestScenario> loadScenario(String id) {
        try {
            File file = scenariosPath.resolve(id + JSON_EXTENSION).toFile();
            if (!file.exists()) {
                return Optional.empty();
            }
            
            TestScenario scenario = objectMapper.readValue(file, TestScenario.class);
            return Optional.of(scenario);
        } catch (IOException e) {
            log.error("Failed to load scenario: {}", id, e);
            return Optional.empty();
        }
    }
    
    /**
     * 获取所有场景
     */
    public List<TestScenario> loadAllScenarios() {
        try {
            return Files.list(scenariosPath)
                    .filter(path -> path.toString().endsWith(JSON_EXTENSION))
                    .map(this::loadScenarioFromFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Failed to list scenarios", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 按标签搜索场景
     */
    public List<TestScenario> findByTags(List<String> tags) {
        return loadAllScenarios().stream()
                .filter(scenario -> {
                    Object scenarioTags = scenario.getMetadata().get("tags");
                    if (scenarioTags instanceof List) {
                        List<?> tagList = (List<?>) scenarioTags;
                        return tagList.stream()
                                .map(Object::toString)
                                .anyMatch(tags::contains);
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 按分类搜索场景
     */
    public List<TestScenario> findByCategory(String category) {
        return loadAllScenarios().stream()
                .filter(scenario -> category.equals(scenario.getMetadata().get("category")))
                .collect(Collectors.toList());
    }
    
    /**
     * 删除场景
     */
    public boolean deleteScenario(String id) {
        try {
            return Files.deleteIfExists(scenariosPath.resolve(id + JSON_EXTENSION));
        } catch (IOException e) {
            log.error("Failed to delete scenario: {}", id, e);
            return false;
        }
    }
    
    private Optional<TestScenario> loadScenarioFromFile(Path path) {
        try {
            TestScenario scenario = objectMapper.readValue(path.toFile(), TestScenario.class);
            return Optional.of(scenario);
        } catch (IOException e) {
            log.error("Failed to load scenario from: {}", path, e);
            return Optional.empty();
        }
    }
}