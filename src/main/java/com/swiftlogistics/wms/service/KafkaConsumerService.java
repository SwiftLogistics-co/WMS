package com.swiftlogistics.wms.service;

import com.swiftlogistics.wms.dto.OrderDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

/**
 * Service for consuming messages from Kafka topics.
 * Handles incoming orders and other events from the ESB.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final PackageTrackingService packageTrackingService;

    /**
     * Consume new order messages from the orders topic.
     */
    @KafkaListener(topics = "${wms.kafka.topics.orders:orders}", 
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrder(@Payload OrderDto orderDto,
                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                            @Header(KafkaHeaders.OFFSET) long offset,
                            Acknowledgment acknowledgment) {
        
        log.info("Received order from Kafka - Topic: {}, Partition: {}, Offset: {}, Order ID: {}, Tracking ID: {}",
                topic, partition, offset, orderDto.getOrderId(), orderDto.getTrackingId());
        
        try {
            // Validate order data
            if (orderDto.getOrderId() == null || orderDto.getTrackingId() == null) {
                log.error("Invalid order received - missing order ID or tracking ID: {}", orderDto);
                acknowledgment.acknowledge();
                return;
            }
            
            // Process the order
            packageTrackingService.processNewOrder(orderDto)
                    .thenRun(() -> {
                        log.info("Successfully processed order: {}", orderDto.getOrderId());
                        acknowledgment.acknowledge();
                    })
                    .exceptionally(ex -> {
                        log.error("Failed to process order: {}", orderDto.getOrderId(), ex);
                        // In a production system, you might want to send to a dead letter queue
                        // For now, we'll acknowledge to prevent infinite retries
                        acknowledgment.acknowledge();
                        return null;
                    });
                    
        } catch (Exception e) {
            log.error("Error processing order message: {}", orderDto.getOrderId(), e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consume package update messages.
     */
    @KafkaListener(topics = "${wms.kafka.topics.package-updates:package-updates}", 
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consumePackageUpdate(@Payload String updateMessage,
                                    @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                    @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                    @Header(KafkaHeaders.OFFSET) long offset,
                                    Acknowledgment acknowledgment) {
        
        log.info("Received package update from Kafka - Topic: {}, Partition: {}, Offset: {}, Message: {}",
                topic, partition, offset, updateMessage);
        
        try {
            // Process package update
            // This is a placeholder for package update logic
            // In a real system, you would parse the message and update accordingly
            
            log.info("Successfully processed package update: {}", updateMessage);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing package update message: {}", updateMessage, e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consume order cancellation messages.
     */
    @KafkaListener(topics = "${wms.kafka.topics.order-cancellations:order-cancellations}", 
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderCancellation(@Payload String cancellationMessage,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {
        
        log.info("Received order cancellation from Kafka - Topic: {}, Partition: {}, Offset: {}, Message: {}",
                topic, partition, offset, cancellationMessage);
        
        try {
            // Parse cancellation message
            // Expected format: "trackingId:reason" or JSON
            String[] parts = cancellationMessage.split(":", 2);
            if (parts.length >= 1) {
                String trackingId = parts[0];
                String reason = parts.length > 1 ? parts[1] : "Order cancelled";
                
                packageTrackingService.cancelOrder(trackingId, reason);
                log.info("Successfully processed order cancellation for tracking ID: {}", trackingId);
            } else {
                log.warn("Invalid cancellation message format: {}", cancellationMessage);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing order cancellation message: {}", cancellationMessage, e);
            acknowledgment.acknowledge();
        }
    }

    /**
     * Consume external warehouse status updates.
     * This could be used for receiving status updates from external systems.
     */
    @KafkaListener(topics = "${wms.kafka.topics.external-status:external-status}", 
                   groupId = "${spring.kafka.consumer.group-id}")
    public void consumeExternalStatus(@Payload String statusMessage,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {
        
        log.info("Received external status update from Kafka - Topic: {}, Partition: {}, Offset: {}, Message: {}",
                topic, partition, offset, statusMessage);
        
        try {
            // Process external status update
            // This is a placeholder for external status update logic
            // You would implement specific logic based on your external system requirements
            
            log.info("Successfully processed external status update: {}", statusMessage);
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing external status message: {}", statusMessage, e);
            acknowledgment.acknowledge();
        }
    }
}
