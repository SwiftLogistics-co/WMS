package com.swiftlogistics.wms.mock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Mock package representation used by the Mock WMS Server.
 * This simulates a package in the warehouse system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockPackage {

    private String trackingId;
    private String orderId;
    private String status;
    private String location;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}
