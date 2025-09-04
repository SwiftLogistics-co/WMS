package com.swiftlogistics.wms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the WMS Adapter Service.
 * This service acts as a bridge between the SwiftTrack platform and warehouse systems,
 * handling package-related events and state changes through Kafka messaging and TCP communication.
 */
@SpringBootApplication
@EnableKafka
@EnableRetry
@EnableAsync
@EnableScheduling
public class WmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(WmsApplication.class, args);
    }
}
