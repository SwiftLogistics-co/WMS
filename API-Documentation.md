# WMS Adapter Service - REST API Documentation

## Overview

The WMS Adapter Service provides REST API endpoints for package management, monitoring, and system operations. All endpoints return JSON responses and follow RESTful conventions.

## Base URL

```
http://localhost:9000/api/wms
```

## Authentication

The service integrates with Keycloak for authentication. Endpoints require appropriate roles:
- `ROLE_WMS` - Full access to WMS operations
- `ROLE_WAREHOUSE_OPERATOR` - Package management operations
- `ROLE_CUSTOMER_SERVICE` - Read-only access to package information

## Package Management Endpoints

### Get Package Details

**Endpoint:** `GET /packages/{trackingId}`

**Description:** Retrieve detailed information about a package by its tracking ID.

**Parameters:**
- `trackingId` (path) - The unique tracking identifier for the package

**Response:**
```json
{
  "id": 1,
  "trackingId": "TRK-001",
  "orderId": "ORD-001",
  "status": "SHIPPED",
  "origin": "WAREHOUSE-A",
  "destination": "Customer Address",
  "currentLocation": "IN_TRANSIT",
  "weight": 2.5,
  "dimensions": "30x20x15",
  "customerId": "CUST-001",
  "carrierId": "CARRIER-001",
  "expectedDeliveryDate": "2025-09-06T10:00:00",
  "actualDeliveryDate": null,
  "createdAt": "2025-09-04T09:00:00",
  "updatedAt": "2025-09-04T11:30:00",
  "notes": "Fragile items - handle with care"
}
```

**Status Codes:**
- `200 OK` - Package found and returned
- `404 Not Found` - Package not found
- `500 Internal Server Error` - Server error occurred

**Example:**
```bash
curl -X GET "http://localhost:9000/api/wms/packages/TRK-001" \
  -H "Authorization: Bearer {token}"
```

---

### Get Package History

**Endpoint:** `GET /packages/{trackingId}/history`

**Description:** Retrieve the complete event history for a package.

**Parameters:**
- `trackingId` (path) - The unique tracking identifier for the package

**Response:**
```json
[
  {
    "id": 1,
    "eventType": "ORDER_CREATED",
    "trackingId": "TRK-001",
    "orderId": "ORD-001",
    "previousStatus": null,
    "newStatus": "RECEIVED",
    "location": "WAREHOUSE-A",
    "description": "Order created and received for processing",
    "metadata": null,
    "source": "WMS-ADAPTER",
    "eventTimestamp": "2025-09-04T09:00:00",
    "createdAt": "2025-09-04T09:00:00"
  },
  {
    "id": 2,
    "eventType": "PACKAGE_STATUS_CHANGED",
    "trackingId": "TRK-001",
    "orderId": "ORD-001",
    "previousStatus": "RECEIVED",
    "newStatus": "PROCESSING",
    "location": "WAREHOUSE-A",
    "description": "Package status updated to PROCESSING",
    "metadata": null,
    "source": "WMS-ADAPTER",
    "eventTimestamp": "2025-09-04T09:15:00",
    "createdAt": "2025-09-04T09:15:00"
  }
]
```

**Status Codes:**
- `200 OK` - History retrieved successfully
- `404 Not Found` - Package not found
- `500 Internal Server Error` - Server error occurred

**Example:**
```bash
curl -X GET "http://localhost:9000/api/wms/packages/TRK-001/history" \
  -H "Authorization: Bearer {token}"
```

---

### Update Package Status

**Endpoint:** `PUT /packages/{trackingId}/status`

**Description:** Manually update the status of a package.

**Parameters:**
- `trackingId` (path) - The unique tracking identifier for the package
- `status` (query, required) - New package status (RECEIVED, PROCESSING, PICKED, PACKED, SHIPPED, DELIVERED, FAILED, RETURNED)
- `location` (query, optional) - Current location of the package
- `notes` (query, optional) - Additional notes about the status change

**Response:**
```
Package status updated successfully
```

**Status Codes:**
- `200 OK` - Status updated successfully
- `400 Bad Request` - Invalid status or request parameters
- `404 Not Found` - Package not found
- `500 Internal Server Error` - Server error occurred

**Example:**
```bash
curl -X PUT "http://localhost:9000/api/wms/packages/TRK-001/status?status=SHIPPED&location=IN_TRANSIT&notes=Package%20shipped%20via%20carrier" \
  -H "Authorization: Bearer {token}"
```

---

### Cancel Order

**Endpoint:** `DELETE /packages/{trackingId}`

**Description:** Cancel an order and update package status to FAILED.

**Parameters:**
- `trackingId` (path) - The unique tracking identifier for the package
- `reason` (query, optional) - Reason for cancellation (default: "Manual cancellation")

**Response:**
```
Order cancelled successfully
```

**Status Codes:**
- `200 OK` - Order cancelled successfully
- `400 Bad Request` - Order cannot be cancelled (already shipped/delivered)
- `404 Not Found` - Package not found
- `500 Internal Server Error` - Server error occurred

**Example:**
```bash
curl -X DELETE "http://localhost:9000/api/wms/packages/TRK-001?reason=Customer%20request" \
  -H "Authorization: Bearer {token}"
```

---

## Monitoring Endpoints

### System Health Check

**Endpoint:** `GET /monitor/health`

**Description:** Check the overall health of the system and its components.

**Response:**
```json
{
  "wms_tcp_connection": "UP",
  "kafka_connection": "UP",
  "mock_wms_server": "UP",
  "mock_packages_count": 15,
  "overall_status": "UP",
  "timestamp": 1725451200000
}
```

**Status Codes:**
- `200 OK` - Health check completed

**Example:**
```bash
curl -X GET "http://localhost:9000/api/wms/monitor/health"
```

---

### System Status

**Endpoint:** `GET /monitor/status`

**Description:** Get detailed system status and component information.

**Response:**
```json
{
  "service_name": "WMS Adapter Service",
  "version": "1.0.0",
  "uptime": 1725451200000,
  "components": {
    "wms_tcp_client": "CONNECTED",
    "kafka_producer": "CONNECTED",
    "mock_wms_server": "RUNNING"
  }
}
```

**Status Codes:**
- `200 OK` - Status retrieved successfully

**Example:**
```bash
curl -X GET "http://localhost:9000/api/wms/monitor/status"
```

---

### Test WMS Connection

**Endpoint:** `GET /monitor/test-wms`

**Description:** Test the TCP connection to the legacy WMS system.

**Response:**
```json
{
  "wms_connection": "SUCCESS",
  "timestamp": 1725451200000
}
```

**Error Response:**
```json
{
  "wms_connection": "ERROR",
  "error": "Connection refused",
  "timestamp": 1725451200000
}
```

**Status Codes:**
- `200 OK` - Test completed (check response body for results)

**Example:**
```bash
curl -X GET "http://localhost:9000/api/wms/monitor/test-wms"
```

---

### Test Kafka Connection

**Endpoint:** `GET /monitor/test-kafka`

**Description:** Test the connection to Kafka broker.

**Response:**
```json
{
  "kafka_connection": "SUCCESS",
  "timestamp": 1725451200000
}
```

**Error Response:**
```json
{
  "kafka_connection": "ERROR",
  "error": "Broker not available",
  "timestamp": 1725451200000
}
```

**Status Codes:**
- `200 OK` - Test completed (check response body for results)

**Example:**
```bash
curl -X GET "http://localhost:9000/api/wms/monitor/test-kafka"
```

---

## Error Responses

### Standard Error Format

All endpoints may return errors in the following format:

```json
{
  "timestamp": "2025-09-04T12:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Package not found with tracking ID: TRK-999",
  "path": "/api/wms/packages/TRK-999"
}
```

### Common HTTP Status Codes

- `200 OK` - Request successful
- `400 Bad Request` - Invalid request parameters
- `401 Unauthorized` - Authentication required
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error occurred

---

## Package Status Values

The following status values are used throughout the API:

| Status | Description |
|--------|-------------|
| `RECEIVED` | Package order has been received but not yet processed |
| `PROCESSING` | Package order is being processed by warehouse staff |
| `PICKED` | Package has been picked from inventory |
| `PACKED` | Package has been packed and is ready for shipment |
| `SHIPPED` | Package has been shipped from the warehouse |
| `DELIVERED` | Package has been delivered to the recipient |
| `FAILED` | Package processing has failed or been cancelled |
| `RETURNED` | Package has been returned to the warehouse |

---

## Event Types

The following event types appear in package history:

| Event Type | Description |
|------------|-------------|
| `ORDER_CREATED` | New order received |
| `ORDER_UPDATED` | Order information changed |
| `ORDER_CANCELLED` | Order cancelled |
| `PACKAGE_STATUS_CHANGED` | Package status updated |
| `PACKAGE_ASSIGNED` | Package assigned to carrier |
| `LOCATION_UPDATED` | Package location changed |
| `ERROR_OCCURRED` | Processing error |
| `OPERATION_COMPLETED` | Warehouse operation completed |
| `INVENTORY_UPDATED` | Inventory levels updated |

---

## Rate Limiting

The API implements rate limiting to ensure system stability:
- **Package Operations**: 100 requests per minute per client
- **Monitoring Endpoints**: 300 requests per minute per client

Rate limit headers are included in responses:
- `X-RateLimit-Limit` - Request limit per time window
- `X-RateLimit-Remaining` - Remaining requests in current window
- `X-RateLimit-Reset` - Time when the rate limit resets

---

## Postman Collection

A Postman collection with all endpoints and example requests is available at:
```
./docs/WMS-Adapter-API.postman_collection.json
```

Import this collection to quickly test all API endpoints with proper authentication and sample data.
