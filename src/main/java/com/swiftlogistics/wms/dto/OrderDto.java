package com.swiftlogistics.wms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an order received from Kafka.
 * This DTO is used for deserializing order messages from the ESB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("tracking_id")
    private String trackingId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("origin")
    private String origin;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("weight")
    private Double weight;

    @JsonProperty("dimensions")
    private String dimensions;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("service_type")
    private String serviceType;

    @JsonProperty("expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    @JsonProperty("special_instructions")
    private String specialInstructions;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("metadata")
    private String metadata;
}
