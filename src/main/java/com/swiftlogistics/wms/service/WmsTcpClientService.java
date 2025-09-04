package com.swiftlogistics.wms.service;

import com.swiftlogistics.wms.config.WmsProperties;
import com.swiftlogistics.wms.dto.WmsTcpMessage;
import com.swiftlogistics.wms.exception.WmsCommunicationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for communicating with the legacy WMS system via TCP protocol.
 * Handles connection management, message serialization, and error recovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WmsTcpClientService {

    private final WmsProperties wmsProperties;
    private final AtomicLong sequenceNumber = new AtomicLong(1);

    /**
     * Send a message to the legacy WMS system synchronously.
     */
    @Retryable(value = {WmsCommunicationException.class}, 
               maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public WmsTcpMessage sendMessage(WmsTcpMessage message) throws WmsCommunicationException {
        log.info("Sending TCP message to WMS: {}", message.getMessageType());
        
        // Set sequence number and timestamp if not provided
        if (message.getSequenceNumber() == null) {
            message.setSequenceNumber(String.valueOf(sequenceNumber.getAndIncrement()));
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        try (Socket socket = createSocket()) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send message
            String tcpMessage = message.toTcpString();
            log.debug("Sending TCP data: {}", tcpMessage);
            writer.write(tcpMessage);
            writer.newLine();
            writer.flush();

            // Read response
            String response = reader.readLine();
            if (response == null) {
                throw new WmsCommunicationException("No response received from WMS");
            }
            
            log.debug("Received TCP response: {}", response);
            WmsTcpMessage responseMessage = WmsTcpMessage.fromTcpString(response);
            log.info("Received response from WMS: {}", responseMessage.getMessageType());
            
            return responseMessage;

        } catch (IOException e) {
            log.error("TCP communication error: {}", e.getMessage(), e);
            throw new WmsCommunicationException("Failed to communicate with WMS: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during TCP communication: {}", e.getMessage(), e);
            throw new WmsCommunicationException("Unexpected error: " + e.getMessage(), e);
        }
    }

    /**
     * Send a message to the legacy WMS system asynchronously.
     */
    @Async("tcpTaskExecutor")
    public CompletableFuture<WmsTcpMessage> sendMessageAsync(WmsTcpMessage message) {
        try {
            WmsTcpMessage response = sendMessage(message);
            return CompletableFuture.completedFuture(response);
        } catch (WmsCommunicationException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Send an order creation message to the WMS.
     */
    public WmsTcpMessage sendOrderCreation(String trackingId, String orderId, String location, String data) 
            throws WmsCommunicationException {
        WmsTcpMessage message = WmsTcpMessage.builder()
                .messageType("ORDER")
                .trackingId(trackingId)
                .orderId(orderId)
                .operation("CREATE")
                .location(location)
                .data(data)
                .build();
        
        return sendMessage(message);
    }

    /**
     * Send a status query message to the WMS.
     */
    public WmsTcpMessage queryPackageStatus(String trackingId) throws WmsCommunicationException {
        WmsTcpMessage message = WmsTcpMessage.builder()
                .messageType("QUERY")
                .trackingId(trackingId)
                .operation("STATUS")
                .build();
        
        return sendMessage(message);
    }

    /**
     * Send an order cancellation message to the WMS.
     */
    public WmsTcpMessage cancelOrder(String trackingId, String orderId) throws WmsCommunicationException {
        WmsTcpMessage message = WmsTcpMessage.builder()
                .messageType("ORDER")
                .trackingId(trackingId)
                .orderId(orderId)
                .operation("CANCEL")
                .build();
        
        return sendMessage(message);
    }

    /**
     * Create a TCP socket connection to the WMS.
     */
    private Socket createSocket() throws IOException {
        WmsProperties.Legacy legacy = wmsProperties.getLegacy();
        Socket socket = new Socket(legacy.getHost(), legacy.getPort());
        socket.setSoTimeout(legacy.getReadTimeout());
        socket.setTcpNoDelay(true);
        return socket;
    }

    /**
     * Test the connection to the WMS system.
     */
    public boolean testConnection() {
        try {
            WmsTcpMessage pingMessage = WmsTcpMessage.builder()
                    .messageType("PING")
                    .operation("TEST")
                    .build();
            
            WmsTcpMessage response = sendMessage(pingMessage);
            return "PONG".equals(response.getMessageType()) || "ACK".equals(response.getMessageType());
        } catch (Exception e) {
            log.warn("WMS connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
