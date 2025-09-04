package com.swiftlogistics.wms.exception;

/**
 * Exception thrown when there are issues processing package operations.
 */
public class PackageProcessingException extends WmsAdapterException {

    public PackageProcessingException(String message) {
        super(message);
    }

    public PackageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
