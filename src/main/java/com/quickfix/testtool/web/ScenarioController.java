package com.quickfix.testtool.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/scenarios")
@CrossOrigin(origins = "*")
public class ScenarioController {
    
    private static final String SCENARIOS_DIR = "scenarios";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public ScenarioController() {
        // 确保scenarios目录存在
        File scenariosDir = new File(SCENARIOS_DIR);
        if (!scenariosDir.exists()) {
            scenariosDir.mkdirs();
        }
    }
    
    @GetMapping
    public List<Map<String, Object>> getAllScenarios() throws IOException {
        File scenariosDir = new File(SCENARIOS_DIR);
        File[] files = scenariosDir.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (files == null) {
            return Collections.emptyList();
        }
        
        List<Map<String, Object>> scenarios = new ArrayList<>();
        for (File file : files) {
            try {
                Map<String, Object> scenario = objectMapper.readValue(file, Map.class);
                scenarios.add(scenario);
            } catch (Exception e) {
                // 跳过无效的文件
                System.err.println("跳过无效文件: " + file.getName() + " - " + e.getMessage());
            }
        }
        
        return scenarios;
    }
    
    @GetMapping("/{id}")
    public Map<String, Object> getScenario(@PathVariable String id) throws IOException {
        File file = new File(SCENARIOS_DIR, id + ".json");
        if (!file.exists()) {
            throw new RuntimeException("场景文件不存在: " + id);
        }
        
        return objectMapper.readValue(file, Map.class);
    }
    
    @PostMapping
    public Map<String, Object> createScenario(@RequestBody Map<String, Object> scenario) throws IOException {
        String id = (String) scenario.get("id");
        if (id == null || id.trim().isEmpty()) {
            id = UUID.randomUUID().toString();
            scenario.put("id", id);
        }
        
        File file = new File(SCENARIOS_DIR, id + ".json");
        objectMapper.writeValue(file, scenario);
        
        return scenario;
    }
    
    @PutMapping("/{id}")
    public Map<String, Object> updateScenario(
            @PathVariable String id, 
            @RequestBody Map<String, Object> scenario) throws IOException {
        
        scenario.put("id", id);
        File file = new File(SCENARIOS_DIR, id + ".json");
        objectMapper.writeValue(file, scenario);
        
        return scenario;
    }
    
    @DeleteMapping("/{id}")
    public void deleteScenario(@PathVariable String id) {
        File file = new File(SCENARIOS_DIR, id + ".json");
        if (file.exists()) {
            file.delete();
        }
    }
    
    @PostMapping("/upload")
    public Map<String, Object> uploadScenario(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件为空");
        }
        
        String filename = file.getOriginalFilename();
        if (!filename.endsWith(".json")) {
            throw new RuntimeException("只能上传JSON文件");
        }
        
        // 读取文件内容
        Map<String, Object> scenario = objectMapper.readValue(file.getInputStream(), Map.class);
        
        // 确保有ID
        String id = (String) scenario.get("id");
        if (id == null || id.trim().isEmpty()) {
            id = filename.replace(".json", "");
            scenario.put("id", id);
        }
        
        // 保存文件
        File targetFile = new File(SCENARIOS_DIR, id + ".json");
        objectMapper.writeValue(targetFile, scenario);
        
        return scenario;
    }
    
    @PostMapping("/{id}/duplicate")
    public Map<String, Object> duplicateScenario(@PathVariable String id) throws IOException {
        Map<String, Object> original = getScenario(id);
        
        // 生成新的ID
        String newId = id + "_copy_" + System.currentTimeMillis();
        original.put("id", newId);
        original.put("name", original.get("name") + " (副本)");
        
        File newFile = new File(SCENARIOS_DIR, newId + ".json");
        objectMapper.writeValue(newFile, original);
        
        return original;
    }
}