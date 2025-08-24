package com.quickfix.testtool.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickfix.testtool.core.engine.TestEngine;
import com.quickfix.testtool.core.model.TestScenario;
import com.quickfix.testtool.service.ScenarioManager;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JSON场景管理REST API
 */
public class JsonScenarioApiServer {
    private final HttpServer server;
    private final ScenarioManager scenarioManager;
    private final ObjectMapper objectMapper;
    
    public JsonScenarioApiServer(int port, TestEngine testEngine) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.scenarioManager = new ScenarioManager(testEngine);
        this.objectMapper = new ObjectMapper();
        
        // 设置路由
        server.createContext("/api/scenarios", new ScenariosHandler());
        server.createContext("/api/execute", new ExecuteHandler());
        server.createContext("/api/status", new StatusHandler());
        server.createContext("/api/health", new HealthHandler());
        
        server.setExecutor(null); // 使用默认执行器
    }
    
    public void start() {
        server.start();
        System.out.println("JSON Scenario API Server started on port " + server.getAddress().getPort());
    }
    
    public void stop() {
        server.stop(0);
    }
    
    // 处理器类
    class ScenariosHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                
                if ("GET".equals(method)) {
                    handleGetScenarios(exchange);
                } else if ("POST".equals(method)) {
                    handleCreateScenario(exchange);
                } else {
                    sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
        
        private void handleGetScenarios(HttpExchange exchange) throws IOException {
            List<TestScenario> scenarios = scenarioManager.getAllScenarios();
            sendResponse(exchange, 200, scenarios);
        }
        
        private void handleCreateScenario(HttpExchange exchange) throws IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, Object> request = objectMapper.readValue(body, Map.class);
            
            TestScenario scenario = scenarioManager.createScenario(
                    (String) request.get("name"),
                    (String) request.get("description"),
                    (List<Map<String, Object>>) request.get("steps")
            );
            
            sendResponse(exchange, 201, Map.of(
                    "id", scenario.getId(),
                    "name", scenario.getName(),
                    "status", "created"
            ));
        }
    }
    
    class ExecuteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, Map.of("error", "Method not allowed"));
                    return;
                }
                
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> request = objectMapper.readValue(body, Map.class);
                
                String scenarioId = (String) request.get("scenarioId");
                boolean async = Boolean.TRUE.equals(request.get("async"));
                
                String executionId = scenarioManager.executeScenario(scenarioId, async);
                
                sendResponse(exchange, 202, Map.of(
                        "executionId", executionId,
                        "status", async ? "queued" : "running"
                ));
            } catch (Exception e) {
                sendResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }
    
    class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                String executionId = params.get("executionId");
                if (executionId == null) {
                    sendResponse(exchange, 400, Map.of("error", "executionId parameter required"));
                    return;
                }
                
                ScenarioManager.ExecutionRecord record = scenarioManager.getExecutionStatus(executionId);
                if (record == null) {
                    sendResponse(exchange, 404, Map.of("error", "Execution not found"));
                    return;
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("executionId", record.executionId);
                response.put("scenarioId", record.scenarioId);
                response.put("status", record.status);
                response.put("startTime", record.startTime.toString());
                if (record.endTime != null) {
                    response.put("endTime", record.endTime.toString());
                }
                if (record.error != null) {
                    response.put("error", record.error);
                }
                
                sendResponse(exchange, 200, response);
            } catch (Exception e) {
                sendResponse(exchange, 500, Map.of("error", e.getMessage()));
            }
        }
    }
    
    class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, Map.of("status", "healthy"));
        }
    }
    
    private void sendResponse(HttpExchange exchange, int status, Object body) throws IOException {
        String json = objectMapper.writeValueAsString(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.getBytes(StandardCharsets.UTF_8).length);
        
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }
    
    private Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    result.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return result;
    }
}