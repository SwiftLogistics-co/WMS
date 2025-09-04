package com.swiftlogistics.wms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object representing TCP messages exchanged with the legacy WMS system.
 * This follows a simple proprietary protocol for warehouse operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsTcpMessage {

    /**
     * Message type identifier (e.g., "ORDER", "STATUS", "ACK", "ERROR")
     */
    private String messageType;

    /**
     * Sequence number for message ordering and acknowledgment
     */
    private String sequenceNumber;

    /**
     * Tracking ID of the package
     */
    private String trackingId;

    /**
     * Order ID
     */
    private String orderId;

    /**
     * Operation code (e.g., "CREATE", "UPDATE", "QUERY", "CANCEL")
     */
    private String operation;

    /**
     * Status information
     */
    private String status;

    /**
     * Location information
     */
    private String location;

    /**
     * Additional data payload
     */
    private String data;

    /**
     * Timestamp of the message
     */
    private String timestamp;

    /**
     * Convert this DTO to a TCP protocol string format.
     * Format: messageType|sequenceNumber|trackingId|orderId|operation|status|location|data|timestamp
     */
    public String toTcpString() {
        return String.format("%s|%s|%s|%s|%s|%s|%s|%s|%s",
                safeString(messageType),
                safeString(sequenceNumber),
                safeString(trackingId),
                safeString(orderId),
                safeString(operation),
                safeString(status),
                safeString(location),
                safeString(data),
                safeString(timestamp));
    }

    /**
     * Parse a TCP protocol string into a WmsTcpMessage DTO.
     * Expected format: messageType|sequenceNumber|trackingId|orderId|operation|status|location|data|timestamp
     */
    public static WmsTcpMessage fromTcpString(String tcpMessage) {
        if (tcpMessage == null || tcpMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("TCP message cannot be null or empty");
        }

        String[] parts = tcpMessage.split("\\|", -1);
        if (parts.length != 9) {
            throw new IllegalArgumentException("Invalid TCP message format. Expected 9 parts, got " + parts.length);
        }

        return WmsTcpMessage.builder()
                .messageType(nullIfEmpty(parts[0]))
                .sequenceNumber(nullIfEmpty(parts[1]))
                .trackingId(nullIfEmpty(parts[2]))
                .orderId(nullIfEmpty(parts[3]))
                .operation(nullIfEmpty(parts[4]))
                .status(nullIfEmpty(parts[5]))
                .location(nullIfEmpty(parts[6]))
                .data(nullIfEmpty(parts[7]))
                .timestamp(nullIfEmpty(parts[8]))
                .build();
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private static String nullIfEmpty(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
