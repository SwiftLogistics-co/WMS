package com.swiftlogistics.wms.model;

/**
 * Enumeration representing different event types that can occur in the warehouse system.
 */
public enum EventType {
    /**
     * A new order has been created
     */
    ORDER_CREATED,
    
    /**
     * An order has been updated
     */
    ORDER_UPDATED,
    
    /**
     * An order has been cancelled
     */
    ORDER_CANCELLED,
    
    /**
     * Package status has changed
     */
    PACKAGE_STATUS_CHANGED,
    
    /**
     * Package has been assigned to a carrier
     */
    PACKAGE_ASSIGNED,
    
    /**
     * Package location has been updated
     */
    LOCATION_UPDATED,
    
    /**
     * An error occurred during processing
     */
    ERROR_OCCURRED,
    
    /**
     * Warehouse operation completed successfully
     */
    OPERATION_COMPLETED,
    
    /**
     * Inventory levels have been updated
     */
    INVENTORY_UPDATED
}
