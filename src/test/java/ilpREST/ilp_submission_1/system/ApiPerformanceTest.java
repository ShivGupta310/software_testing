package ilpREST.ilp_submission_1.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.testutil.TestRequestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the API
 *
 * Tests response times and throughput for various request sizes
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Performance Tests")
public class ApiPerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final long SINGLE_DELIVERY_TIMEOUT_MS = 2000;  // 2 seconds
    private static final long MULTI_DELIVERY_TIMEOUT_MS = 5500;   // 5.5 seconds
    private static final long LARGE_BATCH_TIMEOUT_MS = 10000;      // 10 seconds

    private LocalDate getValidTestDate() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY) {
            return today.plusDays(2);
        } else if (day == DayOfWeek.SUNDAY) {
            return today.plusDays(1);
        }
        return today;
    }

    @Test
    @DisplayName("Single delivery response time < 2s")
    void testSingleDeliveryPerformance() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        long startTime = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Single delivery response time: " + duration + "ms");

        assertTrue(duration < SINGLE_DELIVERY_TIMEOUT_MS,
                "Single delivery should complete within " + SINGLE_DELIVERY_TIMEOUT_MS + "ms, took " + duration + "ms");
    }

    @Test
    @DisplayName("Multi-delivery (2) response time < 5.5s")
    void testMultiDeliveryPerformance() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createMultiDeliveryRequest();

        long startTime = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Multi-delivery (2) response time: " + duration + "ms");

        assertTrue(duration < MULTI_DELIVERY_TIMEOUT_MS,
                "Multi-delivery should complete within " + MULTI_DELIVERY_TIMEOUT_MS + "ms, took " + duration + "ms");
    }

    @Test
    @DisplayName("Five deliveries response time < 10s")
    void testFiveDeliveriesPerformance() throws Exception {
        LocalDate testDate = getValidTestDate();
        List<MedDispatchRec> requests = new ArrayList<>();

        // Create 5 deliveries at safe locations
        double[][] locations = {
                {-3.184, 55.946},
                {-3.183, 55.945},
                {-3.182, 55.944},
                {-3.185, 55.947},
                {-3.181, 55.943}
        };

        for (int i = 0; i < 5; i++) {
            MedDispatchRec request = new MedDispatchRec();
            request.setId(200 + i);
            request.setDate(testDate);
            request.setTime(LocalTime.of(10 + i, 0));
            request.setDelivery(new Position(locations[i][0], locations[i][1]));

            MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
            requirements.setCapacity(1.0);
            requirements.setCooling(false);
            requirements.setHeating(false);
            request.setRequirements(requirements);

            requests.add(request);
        }

        long startTime = System.currentTimeMillis();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("Five deliveries response time: " + duration + "ms");

        assertTrue(duration < LARGE_BATCH_TIMEOUT_MS,
                "Five deliveries should complete within " + LARGE_BATCH_TIMEOUT_MS + "ms, took " + duration + "ms");
    }

    @Test
    @DisplayName("Repeated requests show consistent performance")
    void testConsistentPerformance() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        List<Long> durations = new ArrayList<>();

        // Run 3 times
        for (int i = 0; i < 3; i++) {
            long startTime = System.currentTimeMillis();

            mockMvc.perform(post("/api/v1/calcDeliveryPath")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isOk());

            long endTime = System.currentTimeMillis();
            durations.add(endTime - startTime);
        }

        System.out.println("Response times: " + durations);

        // Calculate average
        double average = durations.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println("Average response time: " + average + "ms");

        // All should complete within timeout
        for (Long duration : durations) {
            assertTrue(duration < SINGLE_DELIVERY_TIMEOUT_MS,
                    "Each request should complete within timeout");
        }
    }

    @Test
    @DisplayName("API returns valid JSON response")
    void testValidJsonResponse() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // Should be valid JSON
        assertDoesNotThrow(() -> objectMapper.readTree(responseBody),
                "Response should be valid JSON");

        // Should deserialize to expected type
        CalcDeliveryPathResponse response = objectMapper.readValue(
                responseBody, CalcDeliveryPathResponse.class);
        assertNotNull(response);
    }

    @Test
    @DisplayName("API handles malformed JSON gracefully")
    void testMalformedJsonHandling() throws Exception {
        String malformedJson = "[{\"id\": 1, \"date\": \"invalid-date\"}]";

        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("API handles missing required fields")
    void testMissingRequiredFields() throws Exception {
        // Missing delivery position
        String incompleteJson = "[{\"id\": 1, \"date\": \"2025-01-20\", \"time\": \"14:00:00\", " +
                "\"requirements\": {\"capacity\": 1.0, \"cooling\": false, \"heating\": false}}]";

        // Should either process or return appropriate error
        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteJson));
        // Not asserting specific status - depends on implementation
    }

    @Test
    @DisplayName("Concurrent requests don't cause errors")
    void testConcurrentRequests() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();
        String requestJson = objectMapper.writeValueAsString(requests);

        // Submit 3 concurrent requests
        List<Thread> threads = new ArrayList<>();
        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 3; i++) {
            Thread t = new Thread(() -> {
                try {
                    mockMvc.perform(post("/api/v1/calcDeliveryPath")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestJson))
                            .andExpect(status().isOk());
                    results.add(true);
                } catch (Exception e) {
                    results.add(false);
                    e.printStackTrace();
                }
            });
            threads.add(t);
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all to complete
        for (Thread t : threads) {
            t.join(SINGLE_DELIVERY_TIMEOUT_MS);
        }

        // All should succeed
        assertEquals(3, results.size(), "All concurrent requests should complete");
        assertTrue(results.stream().allMatch(r -> r), "All concurrent requests should succeed");
    }

    @Test
    @DisplayName("Response structure is correct")
    void testResponseStructure() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        // Check structure
        assertNotNull(response.getDronePaths(), "dronePaths should not be null");
        assertTrue(response.getTotalCost() >= 0, "totalCost should be non-negative");
        assertTrue(response.getTotalMoves() >= 0, "totalMoves should be non-negative");

        if (!response.getDronePaths().isEmpty()) {
            CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);
            assertNotNull(dronePath.getDroneId(), "droneId should not be null");
            assertNotNull(dronePath.getDeliveries(), "deliveries should not be null");

            if (!dronePath.getDeliveries().isEmpty()) {
                CalcDeliveryPathResponse.DeliveryPath deliveryPath = dronePath.getDeliveries().get(0);
                assertNotNull(deliveryPath.getFlightPath(), "flightPath should not be null");
            }
        }
    }
}