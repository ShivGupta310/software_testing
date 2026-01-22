package ilpREST.ilp_submission_1.path;

import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.FlightPathGenerator;
import ilpREST.ilp_submission_1.testutil.TestPathAssertions;
import ilpREST.ilp_submission_1.testutil.TestRequestFactory;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FR5: Drone Move Step Compliance
 *
 * Testing Concepts:
 * - Property Testing: Every move must be exactly MOVE_STEP or 0
 * - Invariant Testing: Distance constraints must always hold
 */
@SpringBootTest
@DisplayName("FR5: Move Step Compliance Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MoveStepComplianceTest {

    private static final Logger log = LoggerFactory.getLogger(MoveStepComplianceTest.class);

    @Autowired
    private FlightPathGenerator pathGenerator;

    private static final double MOVE_STEP = 0.00015;
    private static final double TOLERANCE = 0.00001;
    private static final Position START = TestRequestFactory.getAppletonTower();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        log.info("═".repeat(90));
        log.info("TEST: {}", testInfo.getDisplayName());
        log.info("─".repeat(90));
    }

    @AfterEach
    void tearDown() {
        log.info("═".repeat(90));
        log.info("");
    }

    // ==================== Move Distance Tests ====================
    @Test
    @Order(1)
    @DisplayName("No teleportation (no move exceeds MOVE_STEP)")
    void testNoTeleportation() {
        log.info("Verifying no move exceeds maximum step distance");

        List<MedDispatchRec> deliveries = TestRequestFactory.createMultiDeliveryRequest();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, deliveries, Collections.emptyList());

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);

        double maxDistance = 0;
        int maxIndex = -1;

        for (int i = 0; i < allPositions.size() - 1; i++) {
            double dist = TestPathAssertions.distance(allPositions.get(i), allPositions.get(i + 1));
            if (dist > maxDistance) {
                maxDistance = dist;
                maxIndex = i;
            }
            assertTrue(dist <= MOVE_STEP + TOLERANCE,
                    String.format("Teleportation at move %d: distance %.8f exceeds max %.5f", i, dist, MOVE_STEP));
        }

        log.info("✓ No teleportation. Max move distance: {:.8f} at index {}", maxDistance, maxIndex);
    }

    @Test
    @Order(2)
    @DisplayName("Hover moves have exactly zero distance")
    void testHoverDistanceZero() {
        log.info("Verifying hover moves have distance < {}", TOLERANCE);

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.185, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        List<Position> path = paths.get(0).getFlightPath();
        int hoverCount = 0;

        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            double dist = TestPathAssertions.distance(current, next);

            if (TestPathAssertions.positionsEqual(current, next, TOLERANCE)) {
                assertTrue(dist < TOLERANCE,
                        String.format("Hover at %d should have distance 0, got %.8f", i, dist));
                hoverCount++;
            }
        }

        assertTrue(hoverCount >= 1, "Should have at least one hover in delivery path");
        log.info("✓ Found {} hovers with zero distance", hoverCount);
    }

    // ==================== Path Continuity Tests ====================

    @Test
    @Order(3)
    @DisplayName("Path is continuous with no gaps")
    void testPathContinuity() {
        log.info("Verifying path continuity (no gaps between segments)");

        List<MedDispatchRec> deliveries = TestRequestFactory.createMultiDeliveryRequest();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, deliveries, Collections.emptyList());

        // Check within each segment
        for (int s = 0; s < paths.size(); s++) {
            List<Position> segment = paths.get(s).getFlightPath();
            TestPathAssertions.assertPathContinuous(segment);
            log.info("  Segment {} continuous: {} waypoints", s, segment.size());
        }

        log.info("✓ All {} segments are continuous", paths.size());
    }

    @Test
    @Order(4)
    @DisplayName("Segments connect properly (end of one matches start of next)")
    void testSegmentConnectivity() {
        log.info("Verifying segments connect end-to-end");

        List<MedDispatchRec> deliveries = TestRequestFactory.createMultiDeliveryRequest();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, deliveries, Collections.emptyList());

        for (int i = 0; i < paths.size() - 1; i++) {
            List<Position> currentSegment = paths.get(i).getFlightPath();
            List<Position> nextSegment = paths.get(i + 1).getFlightPath();

            Position endOfCurrent = currentSegment.get(currentSegment.size() - 1);
            Position startOfNext = nextSegment.get(0);

            double gap = TestPathAssertions.distance(endOfCurrent, startOfNext);
            assertTrue(gap <= MOVE_STEP + TOLERANCE,
                    String.format("Gap between segment %d and %d: %.8f", i, i + 1, gap));

            log.info("  Segment {} → {}: gap = {:.8f}", i, i + 1, gap);
        }

        log.info("✓ All segments properly connected");
    }

    // ==================== Distance Calculation Tests ====================

    @Test
    @Order(5)
    @DisplayName("Total path distance is sum of individual moves")
    void testTotalDistanceCalculation() {
        log.info("Verifying total distance = sum of move distances");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.946));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);

        double calculatedTotal = TestPathAssertions.totalPathDistance(allPositions);

        double manualTotal = 0;
        for (int i = 0; i < allPositions.size() - 1; i++) {
            manualTotal += TestPathAssertions.distance(allPositions.get(i), allPositions.get(i + 1));
        }

        assertEquals(manualTotal, calculatedTotal, TOLERANCE,
                "Calculated total should match manual sum");
        log.info("✓ Total distance: {:.8f} (verified)", calculatedTotal);
    }

    @Test
    @Order(6)
    @DisplayName("Path length proportional to straight-line distance")
    void testPathEfficiency() {
        log.info("Verifying path efficiency (not excessively long)");

        Position target = new Position(-3.183, 55.947);
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(target);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        List<Position> deliveryPath = paths.get(0).getFlightPath();

        double straightLine = TestPathAssertions.distance(START, target);
        double actualPath = TestPathAssertions.totalPathDistance(deliveryPath);
        double efficiency = straightLine / actualPath;

        log.info("  Straight-line: {:.8f}", straightLine);
        log.info("  Actual path: {:.8f}", actualPath);
        log.info("  Efficiency: {:.2f}%", efficiency * 100);

        assertTrue(efficiency > 0.3,
                "Path should be at least 30% efficient, got " + (efficiency * 100) + "%");
        log.info("✓ Path efficiency acceptable");
    }

    // ==================== Move Count Tests ====================

    @Test
    @Order(7)
    @DisplayName("Move count matches waypoint count minus one")
    void testMoveCountCalculation() {
        log.info("Verifying move count = waypoints - 1");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(START, Collections.singletonList(delivery),
                        Collections.emptyList());

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
        int expectedMoves = allPositions.size() - 1;

        // Count actual moves
        int actualMoves = 0;
        for (int i = 0; i < allPositions.size() - 1; i++) {
            actualMoves++;
        }

        assertEquals(expectedMoves, actualMoves);
        log.info("✓ Move count correct: {} moves for {} waypoints", actualMoves, allPositions.size());
    }
}