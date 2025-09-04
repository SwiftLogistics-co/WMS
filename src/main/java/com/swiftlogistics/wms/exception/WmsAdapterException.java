package com.swiftlogistics.wms.exception;

/**
 * Base exception class for all WMS adapter related exceptions.
 */
public class WmsAdapterException extends Exception {

    public WmsAdapterException(String message) {
        super(message);
    }

    public WmsAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
