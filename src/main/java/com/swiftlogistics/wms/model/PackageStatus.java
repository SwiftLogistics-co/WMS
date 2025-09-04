package com.swiftlogistics.wms.model;

/**
 * Enumeration representing the various states a package can be in during its lifecycle
 * in the warehouse management system.
 */
public enum PackageStatus {
    /**
     * Package order has been received but not yet processed
     */
    RECEIVED,
    
    /**
     * Package order is being processed by warehouse staff
     */
    PROCESSING,
    
    /**
     * Package has been picked from inventory
     */
    PICKED,
    
    /**
     * Package has been packed and is ready for shipment
     */
    PACKED,
    
    /**
     * Package has been shipped from the warehouse
     */
    SHIPPED,
    
    /**
     * Package has been delivered to the recipient
     */
    DELIVERED,
    
    /**
     * Package processing has failed or been cancelled
     */
    FAILED,
    
    /**
     * Package has been returned to the warehouse
     */
    RETURNED
}
