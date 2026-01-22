package ilpREST.ilp_submission_1.path;

import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.FlightPathGenerator;
import ilpREST.ilp_submission_1.testutil.TestPathAssertions;
import ilpREST.ilp_submission_1.testutil.TestRequestFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FlightPathGenerator
 *
 * Testing Concepts:
 * - Equivalence Partitioning: Different delivery directions/distances
 * - Boundary Value Analysis: Min/max distances, edge cases
 * - Property Testing: Invariants that must always hold
 */
@SpringBootTest
@DisplayName("FlightPathGenerator Unit Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightPathGeneratorTest {

    private static final Logger log = LoggerFactory.getLogger(FlightPathGeneratorTest.class);

    @Autowired
    private FlightPathGenerator pathGenerator;

    private static final Position START = TestRequestFactory.getAppletonTower();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        log.info("═".repeat(90));
        log.info("TEST: {}", testInfo.getDisplayName());
        log.info("─".repeat(90));
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("═".repeat(90));
        log.info("");
    }

    // ==================== Basic Path Generation ====================

    @Test
    @Order(1)
    @DisplayName("Generates non-null path for valid delivery")
    void testGeneratesNonNullPath() {
        log.info("Verifying path generation returns non-null result");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        assertNotNull(paths, "Generated paths should not be null");
        log.info("✓ Path generation returned non-null result with {} segments", paths.size());
    }

    @Test
    @Order(2)
    @DisplayName("Path has correct number of segments (delivery + return)")
    void testCorrectNumberOfSegments() {
        log.info("Verifying segment count = deliveries + 1 return");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        assertEquals(2, paths.size(), "Should have exactly 2 segments: 1 delivery + 1 return");
        log.info("✓ Correct segment count: {} (expected 2)", paths.size());
    }

    @Test
    @Order(3)
    @DisplayName("Each path segment has at least 2 positions")
    void testMinimumPositionsPerSegment() {
        log.info("Verifying each segment has minimum 2 positions (for hover)");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        for (int i = 0; i < paths.size(); i++) {
            List<Position> segment = paths.get(i).getFlightPath();
            assertTrue(segment.size() >= 2,
                    "Segment " + i + " should have at least 2 positions, has " + segment.size());
            log.info("  Segment {}: {} positions", i, segment.size());
        }
        log.info("✓ All segments have minimum required positions");
    }

    // ==================== Hover Verification ====================

    @Test
    @Order(4)
    @DisplayName("Delivery path ends with hover at delivery location")
    void testHoverAtDelivery() {
        log.info("Verifying hover (consecutive same position) at delivery");

        Position deliveryLocation = new Position(-3.184, 55.946);
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(deliveryLocation);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        List<Position> deliveryPath = paths.get(0).getFlightPath();
        TestPathAssertions.assertHoverAtDelivery(deliveryPath, deliveryLocation);

        int hovers = TestPathAssertions.countHovers(deliveryPath);
        log.info("✓ Hover confirmed at delivery location (total hovers in path: {})", hovers);
    }

    @Test
    @Order(5)
    @DisplayName("Return path ends with hover at service point")
    void testHoverAtServicePoint() {
        log.info("Verifying hover at service point on return");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        List<Position> returnPath = paths.get(paths.size() - 1).getFlightPath();
        TestPathAssertions.assertEndsAtServicePoint(returnPath, START);
        log.info("✓ Return path ends with hover at service point");
    }

    // ==================== Multi-Delivery Tests ====================

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Order(6)
    @DisplayName("Multi-delivery generates correct segments for N deliveries")
    void testMultiDeliverySegmentCount(int deliveryCount) {
        log.info("Testing with {} deliveries", deliveryCount);

        List<MedDispatchRec> deliveries = TestRequestFactory.createNDeliveries(deliveryCount);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, deliveries, Collections.emptyList());

        int expectedSegments = deliveryCount + 1; // N deliveries + 1 return
        assertEquals(expectedSegments, paths.size(),
                "Should have " + expectedSegments + " segments for " + deliveryCount + " deliveries");
        log.info("✓ {} deliveries → {} segments (correct)", deliveryCount, paths.size());
    }

    @Test
    @Order(7)
    @DisplayName("Each delivery has hover in multi-delivery route")
    void testMultiDeliveryHovers() {
        log.info("Verifying hover at each delivery in multi-stop route");

        List<MedDispatchRec> deliveries = TestRequestFactory.createMultiDeliveryRequest();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, deliveries, Collections.emptyList());

        for (int i = 0; i < deliveries.size(); i++) {
            Position target = deliveries.get(i).getDelivery();
            List<Position> path = paths.get(i).getFlightPath();
            TestPathAssertions.assertHoverAtDelivery(path, target);
            log.info("  Delivery {}: hover confirmed at [{}, {}]",
                    i + 1, target.getLng(), target.getLat());
        }
        log.info("✓ All {} deliveries have hover", deliveries.size());
    }

    // ==================== Delivery ID Metadata ====================

    @Test
    @Order(8)
    @DisplayName("Delivery segments have correct deliveryId")
    void testDeliveryIdAssignment() {
        log.info("Verifying deliveryId metadata is correctly assigned");

        List<MedDispatchRec> deliveries = new ArrayList<>();
        deliveries.add(TestRequestFactory.createDeliveryWithId(100, new Position(-3.184, 55.945)));
        deliveries.add(TestRequestFactory.createDeliveryWithId(200, new Position(-3.185, 55.946)));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, deliveries, Collections.emptyList());

        assertEquals(100, paths.get(0).getDeliveryId(), "First segment should have ID 100");
        assertEquals(200, paths.get(1).getDeliveryId(), "Second segment should have ID 200");
        log.info("✓ Delivery IDs correctly assigned: 100, 200");
    }

    @Test
    @Order(9)
    @DisplayName("Return segment has null deliveryId")
    void testReturnSegmentNullId() {
        log.info("Verifying return segment has null deliveryId");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(42);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        assertNotNull(paths.get(0).getDeliveryId(), "Delivery segment should have ID");
        assertNull(paths.get(paths.size() - 1).getDeliveryId(), "Return segment should have null ID");
        log.info("✓ Return segment correctly has null deliveryId");
    }

    // ==================== Edge Cases ====================

    @Test
    @Order(10)
    @DisplayName("Empty delivery list returns empty or minimal path")
    void testEmptyDeliveryList() {
        log.info("Testing empty delivery list handling");

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.emptyList(), Collections.emptyList());

        assertNotNull(paths, "Should return non-null for empty list");
        assertTrue(paths.isEmpty() || paths.size() == 1,
                "Empty delivery should produce empty or single hover path");
        log.info("✓ Empty list handled gracefully: {} paths", paths.size());
    }

    @Test
    @Order(11)
    @DisplayName("Very close delivery generates efficient path")
    void testNearbyDeliveryEfficiency() {
        log.info("Testing nearby delivery generates short path");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(START.getLng() + 0.001, START.getLat() + 0.001));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        int waypointCount = paths.get(0).getFlightPath().size();
        assertTrue(waypointCount < 30, "Nearby delivery should have short path, got " + waypointCount);
        log.info("✓ Nearby delivery path efficient: {} waypoints", waypointCount);
    }

    // ==================== Direction Tests (Equivalence Classes) ====================

    @Test
    @Order(12)
    @DisplayName("North delivery generates valid path")
    void testNorthDelivery() {
        log.info("Testing delivery to the NORTH");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleNorthDelivery();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, requests, Collections.emptyList());

        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        TestPathAssertions.assertHoverAtDelivery(paths.get(0).getFlightPath(), requests.get(0).getDelivery());
        log.info("✓ North delivery successful");
    }

    @Test
    @Order(13)
    @DisplayName("South delivery generates valid path")
    void testSouthDelivery() {
        log.info("Testing delivery to the SOUTH");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleSouthDelivery();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, requests, Collections.emptyList());

        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        TestPathAssertions.assertHoverAtDelivery(paths.get(0).getFlightPath(), requests.get(0).getDelivery());
        log.info("✓ South delivery successful");
    }

    @Test
    @Order(14)
    @DisplayName("East delivery generates valid path")
    void testEastDelivery() {
        log.info("Testing delivery to the EAST");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleEastDelivery();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, requests, Collections.emptyList());

        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        TestPathAssertions.assertHoverAtDelivery(paths.get(0).getFlightPath(), requests.get(0).getDelivery());
        log.info("✓ East delivery successful");
    }

    @Test
    @Order(15)
    @DisplayName("West delivery generates valid path")
    void testWestDelivery() {
        log.info("Testing delivery to the WEST");

        List<MedDispatchRec> requests = TestRequestFactory.createSimpleWestDelivery();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, requests, Collections.emptyList());

        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        TestPathAssertions.assertHoverAtDelivery(paths.get(0).getFlightPath(), requests.get(0).getDelivery());
        log.info("✓ West delivery successful");
    }
}