package com.swiftlogistics.wms.exception;

/**
 * Exception thrown when a package with a given tracking ID is not found.
 */
public class PackageNotFoundException extends WmsAdapterException {

    private final String trackingId;

    public PackageNotFoundException(String trackingId) {
        super("Package not found with tracking ID: " + trackingId);
        this.trackingId = trackingId;
    }

    public String getTrackingId() {
        return trackingId;
    }
}
