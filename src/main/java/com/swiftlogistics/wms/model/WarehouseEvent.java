package com.swiftlogistics.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity representing events that occur in the warehouse management system.
 * This provides an audit trail of all package and order related activities.
 */
@Entity
@Table(name = "warehouse_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type of event that occurred
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    /**
     * Tracking ID of the package this event relates to
     */
    @Column(name = "tracking_id")
    private String trackingId;

    /**
     * Order ID this event relates to
     */
    @Column(name = "order_id")
    private String orderId;

    /**
     * Previous status (for status change events)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private PackageStatus previousStatus;

    /**
     * New status (for status change events)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private PackageStatus newStatus;

    /**
     * Location where the event occurred
     */
    @Column(name = "location")
    private String location;

    /**
     * Detailed description of the event
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Additional metadata as JSON string
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * System or user that triggered this event
     */
    @Column(name = "source")
    private String source;

    /**
     * Timestamp when the event occurred
     */
    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    /**
     * Timestamp when the event record was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (eventTimestamp == null) {
            eventTimestamp = now;
        }
        createdAt = now;
    }
}
