package com.swiftlogistics.wms.integration;

import com.swiftlogistics.wms.dto.OrderDto;
import com.swiftlogistics.wms.model.PackageStatus;
import com.swiftlogistics.wms.service.PackageTrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the WMS Adapter Service.
 * Tests the complete workflow from order processing to package tracking.
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    brokerProperties = { "listeners=PLAINTEXT://localhost:0", "port=0" }
)
@TestPropertySource(properties = {
    "spring.kafka.consumer.auto-offset-reset=earliest",
    "spring.kafka.consumer.group-id=wms-adapter-test-group",
    "mock-wms.enabled=false"
})
public class WmsAdapterIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PackageTrackingService packageTrackingService;

    private MockMvc mockMvc;

    @Test
    public void testCompleteOrderWorkflow() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Create a test order
        OrderDto testOrder = OrderDto.builder()
                .orderId("TEST-ORDER-001")
                .trackingId("TEST-TRK-001")
                .customerId("CUSTOMER-001")
                .origin("WAREHOUSE-A")
                .destination("Customer Address")
                .weight(2.5)
                .dimensions("30x20x15")
                .expectedDeliveryDate(LocalDateTime.now().plusDays(2))
                .createdAt(LocalDateTime.now())
                .build();

        // Process the order
        packageTrackingService.processNewOrder(testOrder);

        // Wait a bit for processing
        Thread.sleep(1000);

        // Test: Get package details
        mockMvc.perform(get("/api/wms/packages/TEST-TRK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingId").value("TEST-TRK-001"))
                .andExpect(jsonPath("$.orderId").value("TEST-ORDER-001"));

        // Test: Get package history
        mockMvc.perform(get("/api/wms/packages/TEST-TRK-001/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Test: Update package status
        mockMvc.perform(put("/api/wms/packages/TEST-TRK-001/status")
                        .param("status", PackageStatus.SHIPPED.toString())
                        .param("location", "IN_TRANSIT")
                        .param("notes", "Package shipped via carrier"))
                .andExpect(status().isOk());

        // Test: Verify status update
        mockMvc.perform(get("/api/wms/packages/TEST-TRK-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    @Test
    public void testMonitoringEndpoints() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Test health check
        mockMvc.perform(get("/api/wms/monitor/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overall_status").exists());

        // Test system status
        mockMvc.perform(get("/api/wms/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service_name").value("WMS Adapter Service"));
    }

    @Test
    public void testPackageNotFound() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        // Test with non-existent tracking ID
        mockMvc.perform(get("/api/wms/packages/NON-EXISTENT"))
                .andExpect(status().isNotFound());
    }
}
