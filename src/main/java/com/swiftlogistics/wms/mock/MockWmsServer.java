package com.swiftlogistics.wms.mock;

import com.swiftlogistics.wms.dto.WmsTcpMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock WMS TCP Server that simulates warehouse management system behavior.
 * This server responds to TCP messages from the WMS adapter and simulates
 * warehouse operations like order processing, status updates, etc.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "wms.mock.enabled", havingValue = "true", matchIfMissing = false)
public class MockWmsServer {

    private static final int DEFAULT_PORT = 9999;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    
    // Simulate package storage
    private final ConcurrentHashMap<String, MockPackage> packages = new ConcurrentHashMap<>();

    @PostConstruct
    public void startServer() {
        try {
            serverSocket = new ServerSocket(DEFAULT_PORT);
            executorService = Executors.newCachedThreadPool();
            running.set(true);
            
            log.info("Mock WMS Server starting on port {}", DEFAULT_PORT);
            
            // Start accepting connections in a separate thread
            executorService.submit(this::acceptConnections);
            
        } catch (IOException e) {
            log.error("Failed to start Mock WMS Server", e);
        }
    }

    @PreDestroy
    public void stopServer() {
        running.set(false);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
            log.info("Mock WMS Server stopped");
        } catch (IOException e) {
            log.error("Error stopping Mock WMS Server", e);
        }
    }

    private void acceptConnections() {
        while (running.get() && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                log.debug("New client connection accepted: {}", clientSocket.getRemoteSocketAddress());
                executorService.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running.get()) {
                    log.error("Error accepting client connection", e);
                }
            }
        }
    }

    private void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String request;
            while ((request = reader.readLine()) != null) {
                log.debug("Received TCP message: {}", request);
                
                try {
                    WmsTcpMessage requestMessage = WmsTcpMessage.fromTcpString(request);
                    WmsTcpMessage response = processMessage(requestMessage);
                    
                    String responseString = response.toTcpString();
                    log.debug("Sending TCP response: {}", responseString);
                    
                    writer.write(responseString);
                    writer.newLine();
                    writer.flush();
                    
                } catch (Exception e) {
                    log.error("Error processing message: {}", request, e);
                    
                    // Send error response
                    WmsTcpMessage errorResponse = WmsTcpMessage.builder()
                            .messageType("ERROR")
                            .data("Invalid message format: " + e.getMessage())
                            .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                            .build();
                    
                    writer.write(errorResponse.toTcpString());
                    writer.newLine();
                    writer.flush();
                }
            }
            
        } catch (IOException e) {
            log.debug("Client connection closed: {}", e.getMessage());
        }
    }

    private WmsTcpMessage processMessage(WmsTcpMessage request) {
        String messageType = request.getMessageType();
        String operation = request.getOperation();
        
        log.info("Processing {} message with operation: {}", messageType, operation);
        
        switch (messageType.toUpperCase()) {
            case "ORDER":
                return handleOrderMessage(request);
            case "QUERY":
                return handleQueryMessage(request);
            case "PING":
                return handlePingMessage(request);
            default:
                return createErrorResponse(request, "Unknown message type: " + messageType);
        }
    }

    private WmsTcpMessage handleOrderMessage(WmsTcpMessage request) {
        String operation = request.getOperation();
        
        switch (operation.toUpperCase()) {
            case "CREATE":
                return handleOrderCreation(request);
            case "CANCEL":
                return handleOrderCancellation(request);
            default:
                return createErrorResponse(request, "Unknown order operation: " + operation);
        }
    }

    private WmsTcpMessage handleOrderCreation(WmsTcpMessage request) {
        String trackingId = request.getTrackingId();
        String orderId = request.getOrderId();
        
        // Simulate order processing
        MockPackage mockPackage = MockPackage.builder()
                .trackingId(trackingId)
                .orderId(orderId)
                .status("PROCESSING")
                .location(request.getLocation())
                .createdAt(LocalDateTime.now())
                .build();
        
        packages.put(trackingId, mockPackage);
        
        log.info("Created mock package: {} for order: {}", trackingId, orderId);
        
        // Simulate some processing time and status changes
        simulatePackageProcessing(trackingId);
        
        return WmsTcpMessage.builder()
                .messageType("ACK")
                .sequenceNumber(request.getSequenceNumber())
                .trackingId(trackingId)
                .orderId(orderId)
                .status("ACCEPTED")
                .data("Order created successfully")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private WmsTcpMessage handleOrderCancellation(WmsTcpMessage request) {
        String trackingId = request.getTrackingId();
        
        MockPackage mockPackage = packages.get(trackingId);
        if (mockPackage == null) {
            return createErrorResponse(request, "Package not found: " + trackingId);
        }
        
        mockPackage.setStatus("CANCELLED");
        log.info("Cancelled package: {}", trackingId);
        
        return WmsTcpMessage.builder()
                .messageType("ACK")
                .sequenceNumber(request.getSequenceNumber())
                .trackingId(trackingId)
                .orderId(request.getOrderId())
                .status("CANCELLED")
                .data("Order cancelled successfully")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private WmsTcpMessage handleQueryMessage(WmsTcpMessage request) {
        String trackingId = request.getTrackingId();
        
        MockPackage mockPackage = packages.get(trackingId);
        if (mockPackage == null) {
            return createErrorResponse(request, "Package not found: " + trackingId);
        }
        
        return WmsTcpMessage.builder()
                .messageType("STATUS")
                .sequenceNumber(request.getSequenceNumber())
                .trackingId(trackingId)
                .orderId(mockPackage.getOrderId())
                .status(mockPackage.getStatus())
                .location(mockPackage.getLocation())
                .data("Package status query")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private WmsTcpMessage handlePingMessage(WmsTcpMessage request) {
        return WmsTcpMessage.builder()
                .messageType("PONG")
                .sequenceNumber(request.getSequenceNumber())
                .data("Mock WMS Server is running")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    private WmsTcpMessage createErrorResponse(WmsTcpMessage request, String errorMessage) {
        return WmsTcpMessage.builder()
                .messageType("ERROR")
                .sequenceNumber(request.getSequenceNumber())
                .trackingId(request.getTrackingId())
                .orderId(request.getOrderId())
                .data(errorMessage)
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    /**
     * Simulate package processing lifecycle in a separate thread.
     */
    private void simulatePackageProcessing(String trackingId) {
        executorService.submit(() -> {
            try {
                MockPackage mockPackage = packages.get(trackingId);
                if (mockPackage == null) return;
                
                // Simulate processing stages
                Thread.sleep(2000); // Initial processing
                mockPackage.setStatus("PICKED");
                log.info("Package {} status updated to PICKED", trackingId);
                
                Thread.sleep(3000); // Packing
                mockPackage.setStatus("PACKED");
                log.info("Package {} status updated to PACKED", trackingId);
                
                Thread.sleep(5000); // Shipping
                mockPackage.setStatus("SHIPPED");
                log.info("Package {} status updated to SHIPPED", trackingId);
                
                Thread.sleep(10000); // Delivery
                mockPackage.setStatus("DELIVERED");
                log.info("Package {} status updated to DELIVERED", trackingId);
                
            } catch (InterruptedException e) {
                log.debug("Package processing simulation interrupted for {}", trackingId);
                Thread.currentThread().interrupt();
            }
        });
    }

    public boolean isRunning() {
        return running.get() && serverSocket != null && !serverSocket.isClosed();
    }

    public int getPackageCount() {
        return packages.size();
    }
}
