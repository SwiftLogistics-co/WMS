package com.swiftlogistics.wms.controller;

import com.swiftlogistics.wms.mock.MockWmsServer;
import com.swiftlogistics.wms.service.KafkaProducerService;
import com.swiftlogistics.wms.service.WmsTcpClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring system health and connectivity.
 * Provides endpoints for checking the status of various components.
 */
@Slf4j
@RestController
@RequestMapping("/api/wms/monitor")
@RequiredArgsConstructor
public class MonitoringController {

    private final WmsTcpClientService tcpClientService;
    private final KafkaProducerService kafkaProducerService;
    
    @Autowired(required = false)
    private MockWmsServer mockWmsServer;

    /**
     * Health check endpoint for the entire system.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        boolean overallHealth = true;

        // Check WMS TCP connection
        boolean wmsConnected = tcpClientService.testConnection();
        health.put("wms_tcp_connection", wmsConnected ? "UP" : "DOWN");
        overallHealth &= wmsConnected;

        // Check Kafka connectivity
        boolean kafkaConnected = kafkaProducerService.testKafkaConnectivity();
        health.put("kafka_connection", kafkaConnected ? "UP" : "DOWN");
        overallHealth &= kafkaConnected;

        // Check Mock WMS Server (if enabled)
        if (mockWmsServer != null) {
            boolean mockWmsRunning = mockWmsServer.isRunning();
            health.put("mock_wms_server", mockWmsRunning ? "UP" : "DOWN");
            health.put("mock_packages_count", mockWmsServer.getPackageCount());
        }

        health.put("overall_status", overallHealth ? "UP" : "DOWN");
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }

    /**
     * Get system status information.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("service_name", "WMS Adapter Service");
        status.put("version", "1.0.0");
        status.put("uptime", System.currentTimeMillis());

        // Component status
        Map<String, Object> components = new HashMap<>();
        components.put("wms_tcp_client", tcpClientService.testConnection() ? "CONNECTED" : "DISCONNECTED");
        components.put("kafka_producer", kafkaProducerService.testKafkaConnectivity() ? "CONNECTED" : "DISCONNECTED");
        
        if (mockWmsServer != null) {
            components.put("mock_wms_server", mockWmsServer.isRunning() ? "RUNNING" : "STOPPED");
        }
        
        status.put("components", components);

        return ResponseEntity.ok(status);
    }

    /**
     * Test WMS TCP connection.
     */
    @GetMapping("/test-wms")
    public ResponseEntity<Map<String, Object>> testWmsConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean connected = tcpClientService.testConnection();
            result.put("wms_connection", connected ? "SUCCESS" : "FAILED");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("wms_connection", "ERROR");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Test Kafka connectivity.
     */
    @GetMapping("/test-kafka")
    public ResponseEntity<Map<String, Object>> testKafkaConnection() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            boolean connected = kafkaProducerService.testKafkaConnectivity();
            result.put("kafka_connection", connected ? "SUCCESS" : "FAILED");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            result.put("kafka_connection", "ERROR");
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
        }
    }
}
