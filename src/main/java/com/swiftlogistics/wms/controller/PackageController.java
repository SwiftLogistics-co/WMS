package com.swiftlogistics.wms.controller;

import com.swiftlogistics.wms.exception.PackageNotFoundException;
import com.swiftlogistics.wms.model.Package;
import com.swiftlogistics.wms.model.PackageStatus;
import com.swiftlogistics.wms.model.WarehouseEvent;
import com.swiftlogistics.wms.service.PackageTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for package tracking and management operations.
 * Provides endpoints for querying package status, history, and manual operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/wms/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageTrackingService packageTrackingService;

    /**
     * Get package details by tracking ID.
     */
    @GetMapping("/{trackingId}")
    public ResponseEntity<Package> getPackage(@PathVariable String trackingId) {
        try {
            Package packageEntity = packageTrackingService.queryPackageStatus(trackingId);
            return ResponseEntity.ok(packageEntity);
        } catch (PackageNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving package: {}", trackingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get package tracking history.
     */
    @GetMapping("/{trackingId}/history")
    public ResponseEntity<List<WarehouseEvent>> getPackageHistory(@PathVariable String trackingId) {
        try {
            List<WarehouseEvent> history = packageTrackingService.getPackageHistory(trackingId);
            return ResponseEntity.ok(history);
        } catch (PackageNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error retrieving package history: {}", trackingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Manually update package status.
     */
    @PutMapping("/{trackingId}/status")
    public ResponseEntity<String> updatePackageStatus(
            @PathVariable String trackingId,
            @RequestParam PackageStatus status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String notes) {
        
        try {
            packageTrackingService.updatePackageStatus(trackingId, status, location, notes);
            return ResponseEntity.ok("Package status updated successfully");
        } catch (PackageNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error updating package status: {}", trackingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update package status: " + e.getMessage());
        }
    }

    /**
     * Cancel an order.
     */
    @DeleteMapping("/{trackingId}")
    public ResponseEntity<String> cancelOrder(
            @PathVariable String trackingId,
            @RequestParam(defaultValue = "Manual cancellation") String reason) {
        
        try {
            packageTrackingService.cancelOrder(trackingId, reason);
            return ResponseEntity.ok("Order cancelled successfully");
        } catch (PackageNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Error cancelling order: {}", trackingId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to cancel order: " + e.getMessage());
        }
    }
}
