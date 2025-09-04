package com.swiftlogistics.wms.repository;

import com.swiftlogistics.wms.model.EventType;
import com.swiftlogistics.wms.model.WarehouseEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for managing WarehouseEvent entities.
 */
@Repository
public interface WarehouseEventRepository extends JpaRepository<WarehouseEvent, Long> {

    /**
     * Find events by tracking ID.
     */
    List<WarehouseEvent> findByTrackingId(String trackingId);

    /**
     * Find events by order ID.
     */
    List<WarehouseEvent> findByOrderId(String orderId);

    /**
     * Find events by event type.
     */
    List<WarehouseEvent> findByEventType(EventType eventType);

    /**
     * Find events by tracking ID ordered by event timestamp.
     */
    List<WarehouseEvent> findByTrackingIdOrderByEventTimestampDesc(String trackingId);

    /**
     * Find events within a date range.
     */
    @Query("SELECT e FROM WarehouseEvent e WHERE e.eventTimestamp BETWEEN :startDate AND :endDate ORDER BY e.eventTimestamp DESC")
    List<WarehouseEvent> findByEventTimestampBetween(@Param("startDate") LocalDateTime startDate, 
                                                      @Param("endDate") LocalDateTime endDate);

    /**
     * Find events by event type and tracking ID.
     */
    List<WarehouseEvent> findByEventTypeAndTrackingId(EventType eventType, String trackingId);

    /**
     * Find recent events (last N events).
     */
    @Query("SELECT e FROM WarehouseEvent e ORDER BY e.eventTimestamp DESC")
    List<WarehouseEvent> findRecentEvents();

    /**
     * Count events by event type.
     */
    long countByEventType(EventType eventType);

    /**
     * Find events by location.
     */
    List<WarehouseEvent> findByLocation(String location);
}
