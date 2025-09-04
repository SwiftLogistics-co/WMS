package com.swiftlogistics.wms.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for WMS adapter settings.
 */
@Data
@Component
@ConfigurationProperties(prefix = "wms")
public class WmsProperties {

    /**
     * Legacy WMS TCP connection settings
     */
    private Legacy legacy = new Legacy();

    /**
     * Kafka topic settings
     */
    private Kafka kafka = new Kafka();

    @Data
    public static class Legacy {
        private String host = "localhost";
        private int port = 9000;
        private int connectionTimeout = 5000;
        private int readTimeout = 10000;
        private int maxRetries = 3;
        private long retryDelay = 1000;
    }

    @Data
    public static class Kafka {
        private Topics topics = new Topics();

        @Data
        public static class Topics {
            private String warehouseEvents = "warehouse-events";
            private String packageStatus = "package-status";
            private String dispatchEvents = "dispatch-events";
        }
    }
}
