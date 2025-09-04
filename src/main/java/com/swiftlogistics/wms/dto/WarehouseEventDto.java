package com.swiftlogistics.wms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swiftlogistics.wms.model.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a warehouse event that will be published to Kafka.
 * This DTO is used for serializing warehouse events to the ESB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseEventDto {

    @JsonProperty("event_id")
    private String eventId;

    @JsonProperty("event_type")
    private EventType eventType;

    @JsonProperty("tracking_id")
    private String trackingId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("location")
    private String location;

    @JsonProperty("description")
    private String description;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("metadata")
    private String metadata;
}
