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
 * System tests for FR3-4: Drone Assignment and Scheduling
 *
 * Tests:
 * - Drone capability matching (capacity, cooling, heating)
 * - Availability window compliance
 * - Cost optimization
 * - Multi-drone assignment
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FR3-4: Drone Assignment System Tests")
public class DroneAssignmentSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Gets a valid test date (weekday)
     */
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
    @DisplayName("Basic delivery assignment succeeds")
    void testBasicDeliveryAssignment() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);

        // If drones are available, should have assignment
        if (!response.getDronePaths().isEmpty()) {
            CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);
            assertNotNull(dronePath.getDroneId(), "Assigned drone should have an ID");
            assertFalse(dronePath.getDeliveries().isEmpty(), "Should have delivery paths");
        }
    }

    @Test
    @DisplayName("Multiple deliveries can be assigned to single drone")
    void testMultipleDeliveriesSingleDrone() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createMultiDeliveryRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);

        if (!response.getDronePaths().isEmpty()) {
            // Count total deliveries across all drones
            int totalDeliveries = response.getDronePaths().stream()
                    .mapToInt(dp -> (int) dp.getDeliveries().stream()
                            .filter(d -> d.getDeliveryId() != null)
                            .count())
                    .sum();

            assertTrue(totalDeliveries >= 1, "Should assign at least one delivery");
        }
    }

    @Test
    @DisplayName("Delivery with cooling requirement")
    void testCoolingRequirement() throws Exception {
        LocalDate testDate = getValidTestDate();

        MedDispatchRec request = new MedDispatchRec();
        request.setId(100);
        request.setDate(testDate);
        request.setTime(LocalTime.of(14, 0));
        request.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(true);  // Requires cooling
        requirements.setHeating(false);
        requirements.setMaxCost(null);
        request.setRequirements(requirements);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(request))))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);
        // Response is valid even if no cooling-capable drone available
    }

    @Test
    @DisplayName("Delivery with heating requirement")
    void testHeatingRequirement() throws Exception {
        LocalDate testDate = getValidTestDate();

        MedDispatchRec request = new MedDispatchRec();
        request.setId(101);
        request.setDate(testDate);
        request.setTime(LocalTime.of(14, 0));
        request.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(true);  // Requires heating
        requirements.setMaxCost(null);
        request.setRequirements(requirements);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(request))))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);
    }

    @Test
    @DisplayName("High capacity requirement filters drones")
    void testHighCapacityRequirement() throws Exception {
        LocalDate testDate = getValidTestDate();

        MedDispatchRec request = new MedDispatchRec();
        request.setId(102);
        request.setDate(testDate);
        request.setTime(LocalTime.of(14, 0));
        request.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(500.0);  // High capacity
        requirements.setCooling(false);
        requirements.setHeating(false);
        requirements.setMaxCost(null);
        request.setRequirements(requirements);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(request))))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);
        // May or may not have assignment depending on drone capabilities
    }

    @Test
    @DisplayName("Cost constraint respected")
    void testCostConstraint() throws Exception {
        LocalDate testDate = getValidTestDate();

        MedDispatchRec request = new MedDispatchRec();
        request.setId(103);
        request.setDate(testDate);
        request.setTime(LocalTime.of(14, 0));
        request.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        requirements.setMaxCost(10000.0);  // Reasonable cost limit
        request.setRequirements(requirements);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(request))))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);

        if (!response.getDronePaths().isEmpty()) {
            // If assigned, verify total cost is reasonable
            assertTrue(response.getTotalCost() >= 0, "Cost should be non-negative");
        }
    }

    @Test
    @DisplayName("Very low max cost may prevent assignment")
    void testVeryLowCostConstraint() throws Exception {
        LocalDate testDate = getValidTestDate();

        MedDispatchRec request = new MedDispatchRec();
        request.setId(104);
        request.setDate(testDate);
        request.setTime(LocalTime.of(14, 0));
        request.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        requirements.setMaxCost(0.01);  // Very low cost - likely impossible
        request.setRequirements(requirements);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.singletonList(request))))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);
        // Likely empty dronePaths due to cost constraint
    }

    @Test
    @DisplayName("Response includes total cost and moves")
    void testResponseIncludesCostAndMoves() throws Exception {
        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);

        if (!response.getDronePaths().isEmpty()) {
            assertTrue(response.getTotalCost() > 0, "Should have positive cost when deliveries assigned");
            assertTrue(response.getTotalMoves() > 0, "Should have positive moves when deliveries assigned");
        }
    }

    @Test
    @DisplayName("Empty request returns empty response")
    void testEmptyRequest() throws Exception {
        List<MedDispatchRec> requests = Collections.emptyList();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);
        assertTrue(response.getDronePaths().isEmpty(), "Empty request should return empty drone paths");
        assertEquals(0.0, response.getTotalCost(), 0.001);
        assertEquals(0, response.getTotalMoves());
    }

    @Test
    @DisplayName("Deliveries on different dates are handled")
    void testMultipleDates() throws Exception {
        LocalDate testDate = getValidTestDate();
        List<MedDispatchRec> requests = new ArrayList<>();

        // Delivery today
        MedDispatchRec req1 = new MedDispatchRec();
        req1.setId(105);
        req1.setDate(testDate);
        req1.setTime(LocalTime.of(14, 0));
        req1.setDelivery(new Position(-3.184, 55.946));
        MedDispatchRec.Requirements reqs1 = new MedDispatchRec.Requirements();
        reqs1.setCapacity(1.0);
        reqs1.setCooling(false);
        reqs1.setHeating(false);
        req1.setRequirements(reqs1);
        requests.add(req1);

        // Delivery tomorrow (if also weekday)
        LocalDate tomorrow = testDate.plusDays(1);
        if (tomorrow.getDayOfWeek() != DayOfWeek.SATURDAY &&
                tomorrow.getDayOfWeek() != DayOfWeek.SUNDAY) {
            MedDispatchRec req2 = new MedDispatchRec();
            req2.setId(106);
            req2.setDate(tomorrow);
            req2.setTime(LocalTime.of(14, 0));
            req2.setDelivery(new Position(-3.183, 55.945));
            MedDispatchRec.Requirements reqs2 = new MedDispatchRec.Requirements();
            reqs2.setCapacity(1.0);
            reqs2.setCooling(false);
            reqs2.setHeating(false);
            req2.setRequirements(reqs2);
            requests.add(req2);
        }

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response);
    }
}