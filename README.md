# WMS Adapter Service

The WMS Adapter Service acts as a bridge between the SwiftTrack platform and warehouse management systems, handling all package-related events and state changes through reliable, asynchronous, and real-time integration.

## Overview

This service provides:
- **Kafka Integration**: Consumes commands from Kafka topics and publishes warehouse events
- **TCP Communication**: Translates Kafka messages into proprietary TCP protocol for legacy WMS systems
- **Package Lifecycle Tracking**: Persistent tracking of package states and event history
- **Mock WMS Server**: Built-in simulation of warehouse behavior for testing
- **REST API**: Monitoring and manual operations endpoints
- **Real-time Events**: Asynchronous processing with comprehensive event logging

## Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   ESB/Kafka     │───▶│  WMS Adapter     │───▶│  Legacy WMS     │
│   (Orders)      │    │   Service        │    │  (TCP Server)   │
└─────────────────┘    └──────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   PostgreSQL    │
                       │   (Events &     │
                       │   Packages)     │
                       └─────────────────┘
```

## Features

### Core Functionality
- **Order Processing**: Receives new orders from Kafka and processes them through the WMS
- **Status Tracking**: Real-time package status updates with complete audit trail
- **Event Publishing**: Publishes warehouse events and package status changes to Kafka
- **Error Handling**: Comprehensive error handling with retry mechanisms
- **Async Processing**: Non-blocking operations using Spring's async capabilities

### Mock WMS Server
- Simulates realistic warehouse operations
- Automatic package lifecycle progression (RECEIVED → PROCESSING → PICKED → PACKED → SHIPPED → DELIVERED)
- TCP protocol implementation matching legacy systems
- Configurable response times and behaviors

### Monitoring & Management
- Health check endpoints
- System status monitoring
- Package tracking and history
- Manual status updates
- Connection testing for all components

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `WMS_MOCK_ENABLED` | `true` | Enable/disable mock WMS server |
| `WMS_LEGACY_HOST` | `localhost` | Legacy WMS TCP server host |
| `WMS_LEGACY_PORT` | `8888` | Legacy WMS TCP server port |
| `POSTGRES_HOST` | `localhost` | PostgreSQL database host |
| `POSTGRES_DB` | `swiftlog` | PostgreSQL database name |
| `KAFKA_BROKER` | `localhost:29092` | Kafka broker address |

### Application Profiles
- **default**: Production configuration with external WMS
- **docker**: Docker environment with container networking
- **test**: Test configuration with H2 database and mock services

## API Endpoints

### Package Management
```
GET    /api/wms/packages/{trackingId}         - Get package details
GET    /api/wms/packages/{trackingId}/history - Get package history
PUT    /api/wms/packages/{trackingId}/status  - Update package status
DELETE /api/wms/packages/{trackingId}         - Cancel order
```

### Monitoring
```
GET /api/wms/monitor/health      - System health check
GET /api/wms/monitor/status      - System status information
GET /api/wms/monitor/test-wms    - Test WMS TCP connection
GET /api/wms/monitor/test-kafka  - Test Kafka connectivity
```

## Data Models

### Package Status Lifecycle
```
RECEIVED → PROCESSING → PICKED → PACKED → SHIPPED → DELIVERED
                   ↓
                 FAILED / RETURNED
```

### Event Types
- `ORDER_CREATED` - New order received
- `ORDER_UPDATED` - Order information changed
- `ORDER_CANCELLED` - Order cancelled
- `PACKAGE_STATUS_CHANGED` - Package status updated
- `PACKAGE_ASSIGNED` - Package assigned to carrier
- `LOCATION_UPDATED` - Package location changed
- `ERROR_OCCURRED` - Processing error
- `OPERATION_COMPLETED` - Warehouse operation completed

## TCP Protocol

The service uses a simple pipe-delimited protocol for WMS communication:

```
messageType|sequenceNumber|trackingId|orderId|operation|status|location|data|timestamp
```

### Message Types
- `ORDER` - Order operations (CREATE, CANCEL)
- `QUERY` - Status queries
- `STATUS` - Status responses
- `ACK` - Acknowledgment
- `ERROR` - Error responses
- `PING/PONG` - Health checks

## Getting Started

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker & Docker Compose (for infrastructure)

### Running the Infrastructure
```bash
cd Infrastructure
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Redis (port 6379)
- Redpanda/Kafka (ports 9092, 29092)
- Kafka UI (port 8081)
- Keycloak (port 8080)

### Running the Service
```bash
cd WMS
./mvnw spring-boot:run
```

The service will start on port 8083 with the mock WMS server enabled by default.

### Testing the Service

1. **Health Check**:
```bash
curl http://localhost:8083/api/wms/monitor/health
```

2. **Create a Test Order** (via Kafka):
```bash
# Use Kafka UI at http://localhost:8081 to publish to 'orders' topic
{
  "order_id": "ORD-001",
  "tracking_id": "TRK-001",
  "customer_id": "CUST-001",
  "origin": "WAREHOUSE-A",
  "destination": "Customer Address",
  "weight": 2.5,
  "dimensions": "30x20x15"
}
```

3. **Track Package**:
```bash
curl http://localhost:8083/api/wms/packages/TRK-001
```

## Development

### Project Structure
```
src/main/java/com/swiftlogistics/wms/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── dto/             # Data transfer objects
├── exception/       # Custom exceptions
├── mock/            # Mock WMS server
├── model/           # JPA entities
├── repository/      # Data repositories
└── service/         # Business logic services
```

### Building
```bash
./mvnw clean package
```

### Running Tests
```bash
./mvnw test
```

### Docker Build
```bash
docker build -t wms-adapter:latest .
```

## Deployment

The service is designed for cloud-native deployment with:
- Health check endpoints for Kubernetes liveness/readiness probes
- Graceful shutdown handling
- External configuration support
- Observability through metrics and logging

## Monitoring

The service provides comprehensive monitoring through:
- **Health Checks**: Component-level health status
- **Metrics**: Custom business metrics via Micrometer
- **Logging**: Structured logging with correlation IDs
- **Event Tracking**: Complete audit trail of package operations

## Security

- Integration with Keycloak for authentication/authorization
- Role-based access control (RBAC)
- Secure communication with external systems
- API endpoint protection

## Contributing

1. Follow standard Spring Boot project conventions
2. Write tests for all new functionality
3. Use meaningful commit messages
4. Update documentation for API changes

## License

Copyright (c) 2025 SwiftLogistics. All rights reserved.
