package com.quickfix.testtool;

import com.quickfix.testtool.api.JsonScenarioApiServer;
import com.quickfix.testtool.core.engine.TestEngine;

import java.util.Scanner;

/**
 * JSON场景测试启动器
 */
public class JsonTestRunner {
    public static void main(String[] args) {
        try {
            int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
            
            TestEngine testEngine = new TestEngine();
            JsonScenarioApiServer server = new JsonScenarioApiServer(port, testEngine);
            
            server.start();
            
            System.out.println("\n=== QuickFIX/J JSON Scenario API ===");
            System.out.println("Server started on http://localhost:" + port);
            System.out.println("\nAvailable endpoints:");
            System.out.println("GET  /api/scenarios     - List all scenarios");
            System.out.println("POST /api/scenarios     - Create new scenario");
            System.out.println("POST /api/execute       - Execute scenario");
            System.out.println("GET  /api/status        - Get execution status");
            System.out.println("GET  /api/health        - Health check");
            System.out.println("\nPress Enter to stop server...");
            
            new Scanner(System.in).nextLine();
            server.stop();
            System.out.println("Server stopped");
            
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}