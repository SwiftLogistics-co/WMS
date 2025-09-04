package com.swiftlogistics.wms.repository;

import com.swiftlogistics.wms.model.Package;
import com.swiftlogistics.wms.model.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing Package entities.
 */
@Repository
public interface PackageRepository extends JpaRepository<Package, Long> {

    /**
     * Find a package by its tracking ID.
     */
    Optional<Package> findByTrackingId(String trackingId);

    /**
     * Find packages by order ID.
     */
    List<Package> findByOrderId(String orderId);

    /**
     * Find packages by current status.
     */
    List<Package> findByStatus(PackageStatus status);

    /**
     * Find packages by customer ID.
     */
    List<Package> findByCustomerId(String customerId);

    /**
     * Find packages by carrier ID.
     */
    List<Package> findByCarrierId(String carrierId);

    /**
     * Find packages created within a date range.
     */
    @Query("SELECT p FROM Package p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Package> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Find packages with expected delivery within a date range.
     */
    @Query("SELECT p FROM Package p WHERE p.expectedDeliveryDate BETWEEN :startDate AND :endDate")
    List<Package> findByExpectedDeliveryDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find packages by status and current location.
     */
    List<Package> findByStatusAndCurrentLocation(PackageStatus status, String currentLocation);

    /**
     * Count packages by status.
     */
    long countByStatus(PackageStatus status);

    /**
     * Check if a package exists with the given tracking ID.
     */
    boolean existsByTrackingId(String trackingId);
}
