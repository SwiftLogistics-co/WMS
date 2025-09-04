package com.swiftlogistics.wms.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.swiftlogistics.wms.model.PackageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a package status update that will be published to Kafka.
 * This DTO is used for serializing package status messages to the ESB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageStatusDto {

    @JsonProperty("tracking_id")
    private String trackingId;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("status")
    private PackageStatus status;

    @JsonProperty("previous_status")
    private PackageStatus previousStatus;

    @JsonProperty("location")
    private String location;

    @JsonProperty("carrier_id")
    private String carrierId;

    @JsonProperty("estimated_delivery")
    private LocalDateTime estimatedDelivery;

    @JsonProperty("actual_delivery")
    private LocalDateTime actualDelivery;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("source")
    private String source;
}
