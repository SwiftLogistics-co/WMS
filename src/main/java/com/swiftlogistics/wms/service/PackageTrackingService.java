package com.swiftlogistics.wms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftlogistics.wms.dto.OrderDto;
import com.swiftlogistics.wms.dto.PackageStatusDto;
import com.swiftlogistics.wms.dto.WarehouseEventDto;
import com.swiftlogistics.wms.dto.WmsTcpMessage;
import com.swiftlogistics.wms.exception.PackageNotFoundException;
import com.swiftlogistics.wms.exception.PackageProcessingException;
import com.swiftlogistics.wms.exception.WmsCommunicationException;
import com.swiftlogistics.wms.model.EventType;
import com.swiftlogistics.wms.model.Package;
import com.swiftlogistics.wms.model.PackageStatus;
import com.swiftlogistics.wms.model.WarehouseEvent;
import com.swiftlogistics.wms.repository.PackageRepository;
import com.swiftlogistics.wms.repository.WarehouseEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for tracking package lifecycle and managing package-related operations.
 * This service orchestrates the flow between Kafka, TCP communication, and database persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageTrackingService {

    private final PackageRepository packageRepository;
    private final WarehouseEventRepository eventRepository;
    private final WmsTcpClientService tcpClientService;
    private final KafkaProducerService kafkaProducerService;
    private final ObjectMapper objectMapper;

    /**
     * Process a new order received from Kafka.
     */
    @Transactional
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Void> processNewOrder(OrderDto orderDto) {
        log.info("Processing new order: {} with tracking ID: {}", orderDto.getOrderId(), orderDto.getTrackingId());
        
        try {
            // Create package record
            Package packageEntity = createPackageFromOrder(orderDto);
            packageEntity = packageRepository.save(packageEntity);
            
            // Create warehouse event
            createWarehouseEvent(EventType.ORDER_CREATED, packageEntity.getTrackingId(), 
                    packageEntity.getOrderId(), null, PackageStatus.RECEIVED, 
                    "Order created and received for processing");
            
            // Send to legacy WMS
            String orderData = serializeOrderData(orderDto);
            WmsTcpMessage response = tcpClientService.sendOrderCreation(
                    packageEntity.getTrackingId(),
                    packageEntity.getOrderId(),
                    packageEntity.getOrigin(),
                    orderData
            );
            
            // Process WMS response
            processWmsResponse(response, packageEntity);
            
            log.info("Successfully processed new order: {}", orderDto.getOrderId());
            return CompletableFuture.completedFuture(null);
            
        } catch (Exception e) {
            log.error("Failed to process new order: {}", orderDto.getOrderId(), e);
            handleOrderProcessingError(orderDto, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Update package status and publish to Kafka.
     */
    @Transactional
    public void updatePackageStatus(String trackingId, PackageStatus newStatus, String location, String notes) 
            throws PackageNotFoundException {
        
        Package packageEntity = packageRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new PackageNotFoundException(trackingId));
        
        PackageStatus previousStatus = packageEntity.getStatus();
        packageEntity.setStatus(newStatus);
        packageEntity.setCurrentLocation(location);
        if (notes != null) {
            packageEntity.setNotes(notes);
        }
        
        if (newStatus == PackageStatus.DELIVERED) {
            packageEntity.setActualDeliveryDate(LocalDateTime.now());
        }
        
        packageRepository.save(packageEntity);
        
        // Create warehouse event
        createWarehouseEvent(EventType.PACKAGE_STATUS_CHANGED, trackingId, packageEntity.getOrderId(),
                previousStatus, newStatus, "Package status updated to " + newStatus);
        
        // Publish status update to Kafka
        PackageStatusDto statusDto = PackageStatusDto.builder()
                .trackingId(trackingId)
                .orderId(packageEntity.getOrderId())
                .status(newStatus)
                .previousStatus(previousStatus)
                .location(location)
                .carrierId(packageEntity.getCarrierId())
                .estimatedDelivery(packageEntity.getExpectedDeliveryDate())
                .actualDelivery(packageEntity.getActualDeliveryDate())
                .timestamp(LocalDateTime.now())
                .notes(notes)
                .source("WMS-ADAPTER")
                .build();
        
        kafkaProducerService.publishPackageStatus(statusDto);
        
        log.info("Updated package status for {}: {} -> {}", trackingId, previousStatus, newStatus);
    }

    /**
     * Query package status from the legacy WMS.
     */
    public Package queryPackageStatus(String trackingId) throws PackageNotFoundException, WmsCommunicationException {
        Package packageEntity = packageRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new PackageNotFoundException(trackingId));
        
        try {
            WmsTcpMessage response = tcpClientService.queryPackageStatus(trackingId);
            
            // Update package based on WMS response
            if ("STATUS".equals(response.getMessageType()) && response.getStatus() != null) {
                PackageStatus wmsStatus = parsePackageStatus(response.getStatus());
                if (wmsStatus != packageEntity.getStatus()) {
                    updatePackageStatus(trackingId, wmsStatus, response.getLocation(), 
                            "Status updated from WMS query");
                }
            }
            
            return packageRepository.findByTrackingId(trackingId).orElse(packageEntity);
            
        } catch (WmsCommunicationException e) {
            log.warn("Failed to query WMS for package status: {}", trackingId, e);
            // Return cached status from database
            return packageEntity;
        }
    }

    /**
     * Get package history including all events.
     */
    public List<WarehouseEvent> getPackageHistory(String trackingId) throws PackageNotFoundException {
        if (!packageRepository.existsByTrackingId(trackingId)) {
            throw new PackageNotFoundException(trackingId);
        }
        
        return eventRepository.findByTrackingIdOrderByEventTimestampDesc(trackingId);
    }

    /**
     * Cancel an order.
     */
    @Transactional
    public void cancelOrder(String trackingId, String reason) throws PackageNotFoundException, WmsCommunicationException {
        Package packageEntity = packageRepository.findByTrackingId(trackingId)
                .orElseThrow(() -> new PackageNotFoundException(trackingId));
        
        // Only allow cancellation for certain statuses
        if (packageEntity.getStatus() == PackageStatus.SHIPPED || 
            packageEntity.getStatus() == PackageStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel package that is already shipped or delivered");
        }
        
        // Send cancellation to WMS
        tcpClientService.cancelOrder(trackingId, packageEntity.getOrderId());
        
        // Update package status
        PackageStatus previousStatus = packageEntity.getStatus();
        packageEntity.setStatus(PackageStatus.FAILED);
        packageEntity.setNotes(reason);
        packageRepository.save(packageEntity);
        
        // Create event
        createWarehouseEvent(EventType.ORDER_CANCELLED, trackingId, packageEntity.getOrderId(),
                previousStatus, PackageStatus.FAILED, "Order cancelled: " + reason);
        
        log.info("Cancelled order for tracking ID: {}, reason: {}", trackingId, reason);
    }

    /**
     * Create a package entity from an order DTO.
     */
    private Package createPackageFromOrder(OrderDto orderDto) {
        return Package.builder()
                .trackingId(orderDto.getTrackingId())
                .orderId(orderDto.getOrderId())
                .status(PackageStatus.RECEIVED)
                .origin(orderDto.getOrigin())
                .destination(orderDto.getDestination())
                .currentLocation(orderDto.getOrigin())
                .weight(orderDto.getWeight())
                .dimensions(orderDto.getDimensions())
                .customerId(orderDto.getCustomerId())
                .expectedDeliveryDate(orderDto.getExpectedDeliveryDate())
                .notes(orderDto.getSpecialInstructions())
                .build();
    }

    /**
     * Create a warehouse event and persist it.
     */
    private void createWarehouseEvent(EventType eventType, String trackingId, String orderId,
                                      PackageStatus previousStatus, PackageStatus newStatus, String description) {
        WarehouseEvent event = WarehouseEvent.builder()
                .eventType(eventType)
                .trackingId(trackingId)
                .orderId(orderId)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .description(description)
                .source("WMS-ADAPTER")
                .eventTimestamp(LocalDateTime.now())
                .build();
        
        eventRepository.save(event);
        
        // Publish warehouse event to Kafka
        WarehouseEventDto eventDto = WarehouseEventDto.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .trackingId(trackingId)
                .orderId(orderId)
                .description(description)
                .timestamp(LocalDateTime.now())
                .source("WMS-ADAPTER")
                .build();
        
        kafkaProducerService.publishWarehouseEvent(eventDto);
    }

    /**
     * Process response from legacy WMS.
     */
    private void processWmsResponse(WmsTcpMessage response, Package packageEntity) throws PackageProcessingException {
        try {
            if ("ACK".equals(response.getMessageType())) {
                // Order accepted by WMS
                updatePackageStatus(packageEntity.getTrackingId(), PackageStatus.PROCESSING, 
                        packageEntity.getCurrentLocation(), "Order accepted by WMS");
            } else if ("ERROR".equals(response.getMessageType())) {
                // Order rejected by WMS
                updatePackageStatus(packageEntity.getTrackingId(), PackageStatus.FAILED, 
                        packageEntity.getCurrentLocation(), "Order rejected by WMS: " + response.getData());
            }
        } catch (PackageNotFoundException e) {
            throw new PackageProcessingException("Failed to process WMS response", e);
        }
    }

    /**
     * Handle errors during order processing.
     */
    private void handleOrderProcessingError(OrderDto orderDto, Exception error) {
        try {
            createWarehouseEvent(EventType.ERROR_OCCURRED, orderDto.getTrackingId(), 
                    orderDto.getOrderId(), null, null, 
                    "Error processing order: " + error.getMessage());
        } catch (Exception e) {
            log.error("Failed to create error event for order: {}", orderDto.getOrderId(), e);
        }
    }

    /**
     * Serialize order data for TCP transmission.
     */
    private String serializeOrderData(OrderDto orderDto) {
        try {
            return objectMapper.writeValueAsString(orderDto);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize order data, using simple format", e);
            return String.format("order:%s|customer:%s|destination:%s", 
                    orderDto.getOrderId(), orderDto.getCustomerId(), orderDto.getDestination());
        }
    }

    /**
     * Parse package status from WMS response.
     */
    private PackageStatus parsePackageStatus(String statusString) {
        try {
            return PackageStatus.valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown status from WMS: {}, defaulting to PROCESSING", statusString);
            return PackageStatus.PROCESSING;
        }
    }
}
