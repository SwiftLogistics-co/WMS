package com.swiftlogistics.wms.service;

import com.swiftlogistics.wms.config.WmsProperties;
import com.swiftlogistics.wms.dto.PackageStatusDto;
import com.swiftlogistics.wms.dto.WarehouseEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for publishing messages to Kafka topics.
 * Handles package status updates and warehouse events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WmsProperties wmsProperties;

    /**
     * Publish a package status update to Kafka.
     */
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Void> publishPackageStatus(PackageStatusDto packageStatus) {
        String topic = wmsProperties.getKafka().getTopics().getPackageStatus();
        String key = packageStatus.getTrackingId();
        
        log.info("Publishing package status update for tracking ID: {} to topic: {}", 
                key, topic);
        
        return kafkaTemplate.send(topic, key, packageStatus)
                .thenAccept(result -> {
                    log.debug("Package status published successfully: {}", result.getRecordMetadata());
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish package status for tracking ID: {}", key, ex);
                    return null;
                });
    }

    /**
     * Publish a warehouse event to Kafka.
     */
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Void> publishWarehouseEvent(WarehouseEventDto event) {
        String topic = wmsProperties.getKafka().getTopics().getWarehouseEvents();
        String key = event.getTrackingId() != null ? event.getTrackingId() : event.getOrderId();
        
        log.info("Publishing warehouse event: {} for tracking ID: {} to topic: {}", 
                event.getEventType(), key, topic);
        
        return kafkaTemplate.send(topic, key, event)
                .thenAccept(result -> {
                    log.debug("Warehouse event published successfully: {}", result.getRecordMetadata());
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish warehouse event: {} for tracking ID: {}", 
                            event.getEventType(), key, ex);
                    return null;
                });
    }

    /**
     * Publish a dispatch event to Kafka.
     */
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Void> publishDispatchEvent(Object dispatchEvent, String trackingId) {
        String topic = wmsProperties.getKafka().getTopics().getDispatchEvents();
        String key = trackingId;
        
        log.info("Publishing dispatch event for tracking ID: {} to topic: {}", key, topic);
        
        return kafkaTemplate.send(topic, key, dispatchEvent)
                .thenAccept(result -> {
                    log.debug("Dispatch event published successfully: {}", result.getRecordMetadata());
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish dispatch event for tracking ID: {}", key, ex);
                    return null;
                });
    }

    /**
     * Publish a generic message to a specified topic.
     */
    public CompletableFuture<SendResult<String, Object>> publishMessage(String topic, String key, Object message) {
        log.info("Publishing message to topic: {} with key: {}", topic, key);
        
        return kafkaTemplate.send(topic, key, message)
                .thenApply(result -> {
                    log.debug("Message published successfully to topic: {}, partition: {}, offset: {}", 
                            topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    return result;
                })
                .exceptionally(ex -> {
                    log.error("Failed to publish message to topic: {} with key: {}", topic, key, ex);
                    throw new RuntimeException("Failed to publish message", ex);
                });
    }

    /**
     * Send a test message to verify Kafka connectivity.
     */
    public boolean testKafkaConnectivity() {
        try {
            String testTopic = "test-connectivity";
            String testMessage = "connectivity-test";
            
            CompletableFuture<SendResult<String, Object>> future = 
                    kafkaTemplate.send(testTopic, "test-key", testMessage);
            
            // Wait for completion with timeout
            future.get();
            log.info("Kafka connectivity test successful");
            return true;
        } catch (Exception e) {
            log.warn("Kafka connectivity test failed: {}", e.getMessage());
            return false;
        }
    }
}
