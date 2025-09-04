package com.swiftlogistics.wms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * JPA Entity representing a package in the warehouse management system.
 * This entity tracks the lifecycle and current state of packages.
 */
@Entity
@Table(name = "packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Package {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique tracking identifier for the package
     */
    @Column(name = "tracking_id", unique = true, nullable = false)
    private String trackingId;

    /**
     * External order ID from the ordering system
     */
    @Column(name = "order_id", nullable = false)
    private String orderId;

    /**
     * Current status of the package
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PackageStatus status;

    /**
     * Origin warehouse or facility
     */
    @Column(name = "origin")
    private String origin;

    /**
     * Destination address or facility
     */
    @Column(name = "destination")
    private String destination;

    /**
     * Current location of the package
     */
    @Column(name = "current_location")
    private String currentLocation;

    /**
     * Weight of the package in kilograms
     */
    @Column(name = "weight")
    private Double weight;

    /**
     * Dimensions of the package (length x width x height in cm)
     */
    @Column(name = "dimensions")
    private String dimensions;

    /**
     * Customer ID who owns this package
     */
    @Column(name = "customer_id")
    private String customerId;

    /**
     * Carrier assigned to deliver this package
     */
    @Column(name = "carrier_id")
    private String carrierId;

    /**
     * Expected delivery date
     */
    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    /**
     * Actual delivery date (null if not yet delivered)
     */
    @Column(name = "actual_delivery_date")
    private LocalDateTime actualDeliveryDate;

    /**
     * Timestamp when the package record was created
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the package record was last updated
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Additional notes or comments about the package
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
