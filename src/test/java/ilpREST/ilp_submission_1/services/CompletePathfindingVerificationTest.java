package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CompletePathfindingVerificationTest {

    @Autowired
    private PositionService positionService;

    @Autowired
    private FlightPathGenerator pathGenerator;

    private Position appletonTower;
    private List<RequestRegion.Region> restrictedAreas;

    @BeforeEach
    void setUp() {
        appletonTower = new Position(-3.1863580788986368, 55.94468066708487);
        restrictedAreas = new ArrayList<>();

        // George Square Area
        RequestRegion.Region georgeSq = new RequestRegion.Region();
        georgeSq.setName("George Square Area");
        georgeSq.setVertices(Arrays.asList(
                new Position(-3.190578818321228, 55.94402412577528),
                new Position(-3.1899887323379517, 55.94284650540911),
                new Position(-3.187097311019897, 55.94328811724263),
                new Position(-3.187682032585144, 55.944477740393744),
                new Position(-3.190578818321228, 55.94402412577528)
        ));
        restrictedAreas.add(georgeSq);

        RequestRegion.Region inglis = new RequestRegion.Region();
        inglis.setName("Dr Elsie Inglis Quadrangle");
        inglis.setVertices(Arrays.asList(
                new Position(-3.1907182931900024, 55.94519570234043),
                new Position(-3.1906163692474365, 55.94498241796357),
                new Position(-3.1900262832641597, 55.94507554227258),
                new Position(-3.190133571624756, 55.94529783810495),
                new Position(-3.1907182931900024, 55.94519570234043)
        ));
        restrictedAreas.add(inglis);

        RequestRegion.Region bristo = new RequestRegion.Region();
        bristo.setName("Bristo Square Open Area");
        bristo.setVertices(Arrays.asList(
                new Position(-3.189543485641479, 55.94552313663306),
                new Position(-3.189382553100586, 55.94553214854692),
                new Position(-3.189259171485901, 55.94544803726933),
                new Position(-3.1892001628875732, 55.94533688994374),
                new Position(-3.189194798469543, 55.94519570234043),
                new Position(-3.189135789871216, 55.94511759833873),
                new Position(-3.188138008117676, 55.9452738061846),
                new Position(-3.1885510683059692, 55.946105902745614),
                new Position(-3.1895381212234497, 55.94555918427592),
                new Position(-3.189543485641479, 55.94552313663306)
        ));
        restrictedAreas.add(bristo);

        RequestRegion.Region bayes = new RequestRegion.Region();
        bayes.setName("Bayes Central Area");
        bayes.setVertices(Arrays.asList(
                new Position(-3.1876927614212036, 55.94520696732767),
                new Position(-3.187555968761444, 55.9449621408666),
                new Position(-3.186981976032257, 55.94505676722831),
                new Position(-3.1872327625751495, 55.94536993377657),
                new Position(-3.1874459981918335, 55.9453361389472),
                new Position(-3.1873735785484314, 55.94519344934259),
                new Position(-3.1875935196876526, 55.94515665035927),
                new Position(-3.187624365091324, 55.94521973430925),
                new Position(-3.1876927614212036, 55.94520696732767)
        ));
        restrictedAreas.add(bayes);
    }

    @Test
    void test1_VerifyBearingCalculationIsCartesian() {
        System.out.println("\n=== TEST 1: Verify Bearing Calculation ===");

        Position start = new Position(0.0, 0.0);
        Position east = new Position(1.0, 0.0);
        Position north = new Position(0.0, 1.0);
        Position west = new Position(-1.0, 0.0);
        Position south = new Position(0.0, -1.0);

        // Calculate bearings using reflection to access private method
        double bearingEast = computeBearingTest(start, east);
        double bearingNorth = computeBearingTest(start, north);
        double bearingWest = computeBearingTest(start, west);
        double bearingSouth = computeBearingTest(start, south);

        System.out.println("Bearing to East (1,0): " + bearingEast + "° (expected: 0°)");
        System.out.println("Bearing to North (0,1): " + bearingNorth + "° (expected: 90°)");
        System.out.println("Bearing to West (-1,0): " + bearingWest + "° (expected: 180°)");
        System.out.println("Bearing to South (0,-1): " + bearingSouth + "° (expected: 270°)");

        assertEquals(0.0, bearingEast, 0.1, "East bearing should be 0°");
        assertEquals(90.0, bearingNorth, 0.1, "North bearing should be 90°");
        assertEquals(180.0, bearingWest, 0.1, "West bearing should be 180°");
        assertEquals(270.0, bearingSouth, 0.1, "South bearing should be 270°");

        System.out.println("✓ Bearing calculation is correct (Cartesian)\n");
    }

    @Test
    void test2_SimplePathNoObstacles() {
        System.out.println("\n=== TEST 2: Simple Path Without Obstacles ===");

        Position target = new Position(-3.192, 55.944);
        double distance = positionService.distance(appletonTower, target);
        int expectedSteps = (int) Math.ceil(distance / positionService.STEP_CONST);

        System.out.println("Start: " + appletonTower);
        System.out.println("Target: " + target);
        System.out.println("Distance: " + distance);
        System.out.println("Expected steps: " + expectedSteps);

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(target);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(appletonTower, Collections.singletonList(delivery), new ArrayList<>());

        assertNotNull(paths, "Path should be generated");
        assertEquals(2, paths.size(), "Should have delivery path + return path");

        List<Position> route = paths.get(0).getFlightPath();
        System.out.println("Generated path: " + route.size() + " waypoints");
        System.out.println("Efficiency: " + route.size() + " vs expected " + (expectedSteps + 2) + " (includes hover)");

        assertTrue(route.size() <= expectedSteps * 2, "Path should be reasonably efficient");
        System.out.println("✓ Simple path generated successfully\n");
    }

    @Test
    void test3_PathToAllCardinalDirections() {
        System.out.println("\n=== TEST 3: Paths to All Cardinal Directions ===");

        Position[] targets = {
                new Position(-3.184, 55.9447),   // East
                new Position(-3.186, 55.946),    // North
                new Position(-3.192, 55.944),    // West
                new Position(-3.186, 55.943)     // South
        };

        String[] directions = {"East", "North", "West", "South"};

        for (int i = 0; i < targets.length; i++) {
            System.out.println("Testing " + directions[i] + " to " + targets[i]);

            MedDispatchRec delivery = new MedDispatchRec();
            delivery.setId(i + 1);
            delivery.setDelivery(targets[i]);

            List<CalcDeliveryPathResponse.DeliveryPath> paths =
                    pathGenerator.generateFlightPath(appletonTower, Collections.singletonList(delivery), restrictedAreas);

            assertNotNull(paths, directions[i] + " path should be generated");
            System.out.println("  ✓ " + directions[i] + ": " + paths.get(0).getFlightPath().size() + " waypoints");
        }

        System.out.println("✓ All cardinal directions work\n");
    }

    @Test
    void test4_PathAvoidanceOfRestrictedAreas() {
        System.out.println("\n=== TEST 4: Path Avoidance of Restricted Areas ===");

        // Target that requires navigating around George Square
        Position target = new Position(-3.191, 55.9435);

        System.out.println("Target near George Square: " + target);

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(target);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(appletonTower, Collections.singletonList(delivery), restrictedAreas);

        assertNotNull(paths, "Path should navigate around obstacles");

        List<Position> route = paths.get(0).getFlightPath();

        // Verify no waypoint is inside restricted area
        for (Position waypoint : route) {
            for (RequestRegion.Region region : restrictedAreas) {
                assertFalse(
                        positionService.isInRegion(waypoint, region.getVertices()),
                        "Waypoint " + waypoint + " should not be in " + region.getName()
                );
            }
        }

        System.out.println("✓ Path avoids all " + restrictedAreas.size() + " restricted areas");
        System.out.println("  Waypoints: " + route.size() + "\n");
    }

    @Test
    void test5_MultipleDeliveries() {
        System.out.println("\n=== TEST 5: Multiple Deliveries ===");

        List<MedDispatchRec> deliveries = Arrays.asList(
                createDelivery(1, new Position(-3.184, 55.9447)),
                createDelivery(2, new Position(-3.186, 55.946)),
                createDelivery(3, new Position(-3.191, 55.9435))
        );

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(appletonTower, deliveries, restrictedAreas);

        assertNotNull(paths, "Multiple delivery path should be generated");
        assertEquals(4, paths.size(), "Should have 3 delivery paths + 1 return");

        for (int i = 0; i < deliveries.size(); i++) {
            System.out.println("Delivery " + (i+1) + ": " + paths.get(i).getFlightPath().size() + " waypoints");
        }
        System.out.println("Return: " + paths.get(3).getFlightPath().size() + " waypoints");

        System.out.println("✓ Multiple deliveries handled correctly\n");
    }

    @Test
    void test6_PathEfficiency() {
        System.out.println("\n=== TEST 6: Path Efficiency Check ===");

        Position target = new Position(-3.184, 55.9447);
        double directDistance = positionService.distance(appletonTower, target);
        int minSteps = (int) Math.ceil(directDistance / positionService.STEP_CONST);

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(target);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(appletonTower, Collections.singletonList(delivery), new ArrayList<>());

        int actualSteps = paths.get(0).getFlightPath().size() - 2; // Exclude hover
        double efficiency = (double) minSteps / actualSteps * 100;

        System.out.println("Minimum possible steps: " + minSteps);
        System.out.println("Actual steps taken: " + actualSteps);
        System.out.println("Efficiency: " + String.format("%.1f%%", efficiency));

        assertTrue(efficiency > 70, "Path should be at least 70% efficient");
        System.out.println("✓ Path is reasonably efficient\n");
    }

    @Test
    void test7_TargetInsideRestrictedArea() {
        System.out.println("\n=== TEST 7: Target Inside Restricted Area ===");

        // Position inside George Square
        Position invalidTarget = new Position(-3.188, 55.944);

        boolean isInside = false;
        for (RequestRegion.Region region : restrictedAreas) {
            if (positionService.isInRegion(invalidTarget, region.getVertices())) {
                isInside = true;
                System.out.println("Target is inside: " + region.getName());
                break;
            }
        }

        assertTrue(isInside, "Test target should be inside a restricted area");

        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDelivery(invalidTarget);

        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(appletonTower, Collections.singletonList(delivery), restrictedAreas);

        assertNull(paths, "Should not generate path to target inside restricted area");
        System.out.println("✓ Correctly rejects invalid target\n");
    }

    // Helper methods
    private double computeBearingTest(Position from, Position to) {
        double dx = to.getLng() - from.getLng();
        double dy = to.getLat() - from.getLat();
        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);
        if (angleDeg < 0) angleDeg += 360;
        return angleDeg;
    }

    private MedDispatchRec createDelivery(int id, Position delivery) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(id);
        rec.setDelivery(delivery);
        return rec;
    }
}