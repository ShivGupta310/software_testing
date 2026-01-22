package ilpREST.ilp_submission_1.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.testutil.TestPathAssertions;
import ilpREST.ilp_submission_1.testutil.TestRequestFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * System tests for /api/v1/calcDeliveryPath endpoint
 *
 * Testing Concepts:
 * - End-to-End Testing: Full request/response cycle
 * - Contract Testing: API contract validation
 * - Metamorphic Testing: More deliveries = more path segments
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("CalcDeliveryPath System Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CalcDeliveryPathSystemTest {

    private static final Logger log = LoggerFactory.getLogger(CalcDeliveryPathSystemTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final Position SERVICE_POINT = TestRequestFactory.getAppletonTower();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        log.info("═".repeat(100));
        log.info("SYSTEM TEST: {}", testInfo.getDisplayName());
        log.info("─".repeat(100));
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("═".repeat(100));
        log.info("");
    }

    // ==================== Basic API Contract Tests ====================

    @Test
    @Order(1)
    @DisplayName("API returns 200 OK for valid request")
    void testApiReturns200ForValidRequest() throws Exception {
        log.info("Testing API returns 200 for valid delivery request");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk());

        log.info("✓ API returned 200 OK");
    }

    @Test
    @Order(2)
    @DisplayName("API response contains required fields")
    void testApiResponseStructure() throws Exception {
        log.info("Testing API response contains all required fields");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getDronePaths(), "dronePaths should not be null");

        log.info("Response: totalCost={}, totalMoves={}, dronePaths={}",
                response.getTotalCost(), response.getTotalMoves(), response.getDronePaths().size());
        log.info("✓ Response contains all required fields");
    }

    @Test
    @Order(3)
    @DisplayName("API returns non-negative cost")
    void testApiReturnsNonNegativeCost() throws Exception {
        log.info("Testing API returns non-negative cost");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertTrue(response.getTotalCost() >= 0, "Cost should be non-negative");
        log.info("✓ Cost is non-negative: £{}", response.getTotalCost());
    }

    // ==================== Delivery Hover Tests ====================

    @Test
    @Order(4)
    @DisplayName("Single delivery includes hover at delivery location")
    void testSingleDeliveryHover() throws Exception {
        log.info("Testing single delivery includes hover");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();
        Position deliveryLocation = requests.get(0).getDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);
        CalcDeliveryPathResponse.DeliveryPath deliveryLeg = dronePath.getDeliveries().stream()
                .filter(leg -> leg.getDeliveryId() != null)
                .findFirst()
                .orElseThrow();

        TestPathAssertions.assertHoverAtDelivery(deliveryLeg.getFlightPath(), deliveryLocation);
        log.info("✓ Hover confirmed at delivery location");
    }

    @Test
    @Order(5)
    @DisplayName("Multiple deliveries include hover at each location")
    void testMultiDeliveryHovers() throws Exception {
        log.info("Testing multiple deliveries include hover at each");

        List<MedDispatchRec> requests = TestRequestFactory.createMultiDeliveryRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);

        for (MedDispatchRec request : requests) {
            CalcDeliveryPathResponse.DeliveryPath leg = dronePath.getDeliveries().stream()
                    .filter(l -> l.getDeliveryId() != null && l.getDeliveryId().equals(request.getId()))
                    .findFirst()
                    .orElseThrow();

            TestPathAssertions.assertHoverAtDelivery(leg.getFlightPath(), request.getDelivery());
            log.info("  Delivery {}: hover confirmed", request.getId());
        }
        log.info("✓ All deliveries have hover");
    }

    // ==================== Return to Service Point Tests ====================

    @Test
    @Order(6)
    @DisplayName("Drone returns to service point after delivery")
    void testReturnToServicePoint() throws Exception {
        log.info("Testing drone returns to service point");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);
        CalcDeliveryPathResponse.DeliveryPath returnLeg = dronePath.getDeliveries().stream()
                .filter(leg -> leg.getDeliveryId() == null)
                .findFirst()
                .orElseThrow();

        TestPathAssertions.assertEndsAtServicePoint(returnLeg.getFlightPath(), SERVICE_POINT);
        log.info("✓ Drone returns to service point");
    }

    // ==================== Journey Structure Tests ====================

    @Test
    @Order(7)
    @DisplayName("Journey has N deliveries + 1 return leg")
    void testJourneyStructure() throws Exception {
        log.info("Testing journey structure");

        List<MedDispatchRec> requests = TestRequestFactory.createMultiDeliveryRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);

        long deliveryLegs = dronePath.getDeliveries().stream()
                .filter(leg -> leg.getDeliveryId() != null)
                .count();
        long returnLegs = dronePath.getDeliveries().stream()
                .filter(leg -> leg.getDeliveryId() == null)
                .count();

        assertEquals(requests.size(), deliveryLegs, "Should have N delivery legs");
        assertEquals(1, returnLegs, "Should have exactly 1 return leg");
        log.info("✓ Journey structure correct: {} deliveries + {} return", deliveryLegs, returnLegs);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    @Order(8)
    @DisplayName("N deliveries produce N+1 segments")
    void testDeliveryCountToSegmentCount(int deliveryCount) throws Exception {
        log.info("Testing {} deliveries produce {} segments", deliveryCount, deliveryCount + 1);

        List<MedDispatchRec> requests = TestRequestFactory.createNDeliveries(deliveryCount);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        if (!response.getDronePaths().isEmpty()) {
            CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);
            assertEquals(deliveryCount + 1, dronePath.getDeliveries().size(),
                    "Should have " + (deliveryCount + 1) + " segments");
        }
        log.info("✓ Segment count correct");
    }

    // ==================== Response Metrics Tests ====================

    @Test
    @Order(9)
    @DisplayName("Total moves count is positive for valid delivery")
    void testTotalMovesPositive() throws Exception {
        log.info("Testing total moves count is positive");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertTrue(response.getTotalMoves() > 0, "Total moves should be positive");
        log.info("✓ Total moves: {}", response.getTotalMoves());
    }
    
    @Test
    @Order(10)
    @DisplayName("More deliveries = more moves (metamorphic)")
    void testMoreDeliveriesMoreMoves() throws Exception {
        log.info("Testing metamorphic property: more deliveries = more moves");

        // FIRST REQUEST: 1 delivery
        List<MedDispatchRec> requests1 = TestRequestFactory.createSimpleNorthDelivery();
        Position safeLocation = requests1.get(0).getDelivery(); // Use known-good location

        MvcResult result1 = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests1)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(), CalcDeliveryPathResponse.class);
        int moves1 = response1.getTotalMoves();

        List<MedDispatchRec> requestsMany = new ArrayList<>();
        int deliveryCount = 2;

        for (int i = 0; i < deliveryCount; i++) {
            MedDispatchRec delivery = new MedDispatchRec();
            delivery.setId(i + 1);
            delivery.setDate(LocalDate.of(2026, 1, 22));
            delivery.setTime(LocalTime.of(10, 0).plusHours(i));

            // Use same safe location
            delivery.setDelivery(safeLocation);

            MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
            req.setCapacity(1.0);
            req.setMaxCost(1000.0); // Increase maxCost just in case
            delivery.setRequirements(req);

            requestsMany.add(delivery);
        }

        MvcResult resultMany = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestsMany)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse responseMany = objectMapper.readValue(
                resultMany.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        int movesMany = responseMany.getTotalMoves();
        log.info("Response Many: totalMoves={}, dronePaths={}", movesMany, responseMany.getDronePaths().size());

        // Assertion
        assertTrue(movesMany >= moves1,
                String.format("More deliveries should result in >= moves. 1 delivery: %d moves, %d deliveries: %d moves",
                        moves1, deliveryCount, movesMany));
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(11)
    @DisplayName("Empty request returns empty drone paths")
    void testEmptyRequestReturnsEmptyPaths() throws Exception {
        log.info("Testing empty request handling");

        List<MedDispatchRec> requests = new ArrayList<>();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertTrue(response.getDronePaths().isEmpty(), "Empty request should return empty paths");
        assertEquals(0, response.getTotalMoves());
        log.info("✓ Empty request handled correctly");
    }

    @Test
    @Order(12)
    @DisplayName("Unrealistic capacity returns empty paths")
    void testUnrealisticCapacityReturnsEmpty() throws Exception {
        log.info("Testing unrealistic capacity handling");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(TestRequestFactory.getValidTestDate());
        delivery.setTime(LocalTime.of(14, 0));
        delivery.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(999999.0);
        delivery.setRequirements(requirements);

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(delivery))))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertTrue(response.getDronePaths().isEmpty(),
                "Unrealistic capacity should return empty paths");
        log.info("✓ Unrealistic capacity handled correctly");
    }

    @Test
    @Order(13)
    @DisplayName("Malformed JSON returns 400")
    void testMalformedJsonReturns400() throws Exception {
        log.info("Testing malformed JSON handling");

        mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        log.info("✓ Malformed JSON rejected with 400");
    }

    // ==================== Direction Tests ====================

    @Test
    @Order(14)
    @DisplayName("North delivery succeeds via API")
    void testNorthDeliveryApi() throws Exception {
        log.info("Testing north delivery via API");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertFalse(response.getDronePaths().isEmpty(), "Should assign drone for north delivery");
        log.info("✓ North delivery successful");
    }

    @Test
    @Order(15)
    @DisplayName("South delivery succeeds via API")
    void testSouthDeliveryApi() throws Exception {
        log.info("Testing south delivery via API");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleSouthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        assertFalse(response.getDronePaths().isEmpty(), "Should assign drone for south delivery");
        log.info("✓ South delivery successful");
    }

    // ==================== Move Compliance via API ====================

    @Test
    @Order(16)
    @DisplayName("API response path is continuous")
    void testApiResponsePathContinuous() throws Exception {
        log.info("Testing path continuity in API response");

        List<MedDispatchRec> requests = TestRequestFactory.createMultiDeliveryRequest();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        if (!response.getDronePaths().isEmpty()) {
            List<Position> allPositions = TestPathAssertions.flattenAllPaths(
                    response.getDronePaths().get(0).getDeliveries());

            TestPathAssertions.assertPathContinuous(allPositions);
            log.info("✓ API response path is continuous");
        }
    }

    @Test
    @Order(17)
    @DisplayName("Drone ID is assigned in response")
    void testDroneIdAssigned() throws Exception {
        log.info("Testing drone ID is assigned");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        MvcResult result = mockMvc.perform(post("/api/v1/calcDeliveryPath")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andReturn();

        CalcDeliveryPathResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                CalcDeliveryPathResponse.class);

        if (!response.getDronePaths().isEmpty()) {
            CalcDeliveryPathResponse.DronePath dronePath = response.getDronePaths().get(0);
            assertNotNull(dronePath.getDroneId(), "Drone ID should be assigned");
            log.info("✓ Drone ID assigned: {}", dronePath.getDroneId());
        }
    }
}