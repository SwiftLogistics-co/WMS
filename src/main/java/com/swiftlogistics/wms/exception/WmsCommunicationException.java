package com.swiftlogistics.wms.exception;

/**
 * Exception thrown when there are communication issues with the legacy WMS TCP server.
 */
public class WmsCommunicationException extends WmsAdapterException {

    public WmsCommunicationException(String message) {
        super(message);
    }

    public WmsCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
