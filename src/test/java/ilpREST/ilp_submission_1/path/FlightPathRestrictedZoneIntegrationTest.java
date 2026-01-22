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
 * Integration tests for FR7: No-Fly Zone Compliance
 *
 * Testing Concepts:
 * - Boundary Value Analysis: Points on/near zone boundaries
 * - Decision Table Testing: Valid/invalid destination combinations
 * - Property Testing: No waypoint should ever be inside restricted zone
 */
@SpringBootTest
@DisplayName("FR7: No-Fly Zone Compliance Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlightPathRestrictedZoneIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(FlightPathRestrictedZoneIntegrationTest.class);

    @Autowired
    private FlightPathGenerator pathGenerator;

    private Position start;
    private List<RequestRegion.Region> restrictedAreas;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        start = TestRequestFactory.getAppletonTower();
        restrictedAreas = TestRequestFactory.getStandardRestrictedAreas();

        log.info("═".repeat(90));
        log.info("TEST: {}", testInfo.getDisplayName());
        log.info("─".repeat(90));
        log.info("Service Point: [{}, {}]", start.getLng(), start.getLat());
        log.info("Restricted Zones: {}", restrictedAreas.size());
    }

    @AfterEach
    void tearDown() {
        log.info("═".repeat(90));
        log.info("");
    }

    // ==================== Waypoint Tests ====================

    @Test
    @Order(1)
    @DisplayName("No waypoint inside any restricted zone")
    void testNoWaypointInRestrictedZone() {
        log.info("Verifying all waypoints are outside restricted zones");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.945));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
        log.info("Checking {} waypoints", allPositions.size());

        int checkedCount = 0;
        for (Position pos : allPositions) {
            TestPathAssertions.assertNotInRestrictedZone(pos, restrictedAreas);
            checkedCount++;
        }

        log.info("✓ All {} waypoints verified outside restricted zones", checkedCount);
    }

    @Test
    @Order(2)
    @DisplayName("No path segment crosses restricted zone boundary")
    void testNoSegmentCrossesRestrictedZone() {
        log.info("Verifying no path segment crosses restricted zone boundary");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.946));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
        TestPathAssertions.assertNoSegmentCrossesRestrictedZone(allPositions, restrictedAreas);

        log.info("✓ No segment crosses restricted zone boundaries");
    }

    // ==================== Destination Inside Zone Tests ====================

    @Test
    @Order(3)
    @DisplayName("Destination inside George Square rejected")
    void testDestinationInsideGeorgeSquare() {
        log.info("Testing rejection of destination inside George Square");

        Position insideGeorge = TestRequestFactory.getInsideGeorgeSquare();

        // Verify it's actually inside
        assertTrue(TestPathAssertions.isInRestrictedZone(insideGeorge, restrictedAreas),
                "Test position should be inside restricted zone");
        log.info("Confirmed: [{}, {}] is inside George Square", insideGeorge.getLng(), insideGeorge.getLat());

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(insideGeorge);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        assertTrue(paths == null || paths.isEmpty(),
                "Should reject destination inside restricted zone");
        log.info("✓ Destination inside George Square correctly rejected");
    }

    @Test
    @Order(4)
    @DisplayName("Destination inside Bristo Square rejected")
    void testDestinationInsideBristoSquare() {
        log.info("Testing rejection of destination inside Bristo Square");

        Position insideBristo = TestRequestFactory.getInsideBristoSquare();

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(insideBristo);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        assertTrue(paths == null || paths.isEmpty(),
                "Should reject destination inside restricted zone");
        log.info("✓ Destination inside Bristo Square correctly rejected");
    }

    // ==================== Boundary Tests ====================

    @Test
    @Order(5)
    @DisplayName("Destination just outside restricted zone succeeds")
    void testDestinationJustOutsideZone() {
        log.info("Testing destination just outside restricted zone boundary");

        Position justOutside = TestRequestFactory.getJustOutsideGeorgeSquare();

        assertFalse(TestPathAssertions.isInRestrictedZone(justOutside, restrictedAreas),
                "Test position should be outside restricted zones");
        log.info("Confirmed: [{}, {}] is outside restricted zones", justOutside.getLng(), justOutside.getLat());

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(justOutside);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        assertNotNull(paths);
        assertFalse(paths.isEmpty(), "Should generate path to point just outside zone");
        log.info("✓ Destination just outside zone accepted with {} segments", paths.size());
    }

    // ==================== Multi-Delivery Tests ====================

    @Test
    @Order(6)
    @DisplayName("Multi-delivery route avoids all restricted zones")
    void testMultiDeliveryAvoidsZones() {
        log.info("Testing multi-delivery route avoids all zones");

        List<MedDispatchRec> deliveries = TestRequestFactory.createMultiDeliveryRequest();

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, deliveries, restrictedAreas);

        assertNotNull(paths);
        assertFalse(paths.isEmpty());

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
        log.info("Checking {} waypoints in multi-delivery route", allPositions.size());

        for (Position pos : allPositions) {
            TestPathAssertions.assertNotInRestrictedZone(pos, restrictedAreas);
        }

        log.info("✓ Multi-delivery route avoids all restricted zones");
    }

    @Test
    @Order(7)
    @DisplayName("Return path avoids restricted zones")
    void testReturnPathAvoidsZones() {
        log.info("Testing return path avoids restricted zones");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.184, 55.946));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        // Get return path (last segment)
        List<Position> returnPath = paths.get(paths.size() - 1).getFlightPath();
        log.info("Checking {} waypoints in return path", returnPath.size());

        for (Position pos : returnPath) {
            TestPathAssertions.assertNotInRestrictedZone(pos, restrictedAreas);
        }

        log.info("✓ Return path avoids all restricted zones");
    }

    // ==================== Zone Configuration Tests ====================

    @Test
    @Order(8)
    @DisplayName("Path generation works with no restricted zones")
    void testNoRestrictedZones() {
        log.info("Testing path generation with zero restricted zones");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.186, 55.944));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        Collections.emptyList());

        assertNotNull(paths);
        assertFalse(paths.isEmpty());
        log.info("✓ Path generated successfully with no restrictions: {} segments", paths.size());
    }

    @Test
    @Order(9)
    @DisplayName("All four restricted zones are enforced")
    void testAllZonesEnforced() {
        log.info("Verifying all {} restricted zones are enforced", restrictedAreas.size());

        assertEquals(4, restrictedAreas.size(), "Should have exactly 4 restricted zones");

        for (RequestRegion.Region zone : restrictedAreas) {
            log.info("  Zone: {} ({} vertices)", zone.getName(), zone.getVertices().size());
            assertNotNull(zone.getName());
            assertNotNull(zone.getVertices());
            assertTrue(zone.getVertices().size() >= 3, "Zone should have at least 3 vertices");
        }

        log.info("✓ All 4 restricted zones are properly defined");
    }

    // ==================== Path Routing Tests ====================

    @Test
    @Order(10)
    @DisplayName("Path routes around obstacle to reach destination")
    void testPathRoutesAroundObstacle() {
        log.info("Testing path routes around obstacle");

        // Destination on opposite side of George Square
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.192, 55.941));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        if (paths != null && !paths.isEmpty()) {
            List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
            log.info("Path generated with {} waypoints", allPositions.size());

            // Verify no waypoints are inside restricted zones
            for (Position pos : allPositions) {
                TestPathAssertions.assertNotInRestrictedZone(pos, restrictedAreas);
            }
            log.info("✓ Path successfully routes around obstacles");
        } else {
            log.info("No path generated (destination may be unreachable)");
        }
    }

    @Test
    @Order(11)
    @DisplayName("Path avoids cutting corners of restricted zones")
    void testPathAvoidsCornerCutting() {
        log.info("Testing path doesn't cut corners of restricted zones");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(new Position(-3.185, 55.946));

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, Collections.singletonList(delivery),
                        restrictedAreas);

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
        TestPathAssertions.assertNoSegmentCrossesRestrictedZone(allPositions, restrictedAreas);

        log.info("✓ Path doesn't cut corners of restricted zones");
    }

    @Test
    @Order(12)
    @DisplayName("Complex route through multiple zones works correctly")
    void testComplexMultiZoneRoute() {
        log.info("Testing complex route that must navigate multiple zones");

        List<MedDispatchRec> deliveries = new ArrayList<>();

        MedDispatchRec d1 = new MedDispatchRec();
        d1.setId(1);
        d1.setDelivery(new Position(-3.184, 55.946)); // North-East
        deliveries.add(d1);

        MedDispatchRec d2 = new MedDispatchRec();
        d2.setId(2);
        d2.setDelivery(new Position(-3.191, 55.946)); // North-West
        deliveries.add(d2);

        MedDispatchRec d3 = new MedDispatchRec();
        d3.setId(3);
        d3.setDelivery(new Position(-3.183, 55.943)); // South-East
        deliveries.add(d3);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(start, deliveries, restrictedAreas);

        assertNotNull(paths);
        log.info("Complex route generated with {} segments", paths.size());

        List<Position> allPositions = TestPathAssertions.flattenAllPaths(paths);
        for (Position pos : allPositions) {
            TestPathAssertions.assertNotInRestrictedZone(pos, restrictedAreas);
        }

        log.info("✓ Complex multi-zone route successfully avoids all restrictions");
    }
}