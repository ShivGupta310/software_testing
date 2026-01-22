package ilpREST.ilp_submission_1.testutil;

import ilpREST.ilp_submission_1.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Assertion utilities for path validation testing
 *
 * Implements Property-Based Testing concepts - validates invariants
 */
public class TestPathAssertions {

    private static final Logger log = LoggerFactory.getLogger(TestPathAssertions.class);
    private static final double MOVE_STEP = 0.00015;
    private static final double TOLERANCE = 0.000001;
    private static final double CLOSE_ENOUGH = 0.00015;

    /**
     * Asserts hover occurs at delivery location (position appears twice consecutively)
     */
    public static void assertHoverAtDelivery(List<Position> path, Position deliveryLocation) {
        assertNotNull(path, "Path should not be null");
        assertTrue(path.size() >= 2, "Path must have at least 2 positions for hover");

        boolean hoverFound = false;
        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);

            if (positionsEqual(current, deliveryLocation, CLOSE_ENOUGH) &&
                    positionsEqual(next, deliveryLocation, CLOSE_ENOUGH) &&
                    positionsEqual(current, next, TOLERANCE)) {
                hoverFound = true;
                log.debug("Hover found at index {} and {}", i, i + 1);
                break;
            }
        }

        assertTrue(hoverFound,
                String.format("Expected hover at delivery location [%.6f, %.6f] but none found in path of %d positions",
                        deliveryLocation.getLng(), deliveryLocation.getLat(), path.size()));
    }

    /**
     * Asserts path ends at service point with hover
     */
    public static void assertEndsAtServicePoint(List<Position> path, Position servicePoint) {
        assertNotNull(path, "Path should not be null");
        assertTrue(path.size() >= 2, "Path must have at least 2 positions");

        Position last = path.get(path.size() - 1);
        Position secondLast = path.get(path.size() - 2);

        assertTrue(positionsEqual(last, servicePoint, CLOSE_ENOUGH),
                String.format("Path should end at service point [%.6f, %.6f] but ended at [%.6f, %.6f]",
                        servicePoint.getLng(), servicePoint.getLat(), last.getLng(), last.getLat()));

        assertTrue(positionsEqual(secondLast, servicePoint, CLOSE_ENOUGH),
                String.format("Path should hover at service point (second last position should match)"));
    }

    /**
     * Asserts position is not inside any restricted zone
     * Uses ray casting algorithm for point-in-polygon test
     */
    public static void assertNotInRestrictedZone(Position position, List<RequestRegion.Region> restrictedZones) {
        for (RequestRegion.Region zone : restrictedZones) {
            assertFalse(isPointInPolygon(position, zone.getVertices()),
                    String.format("Position [%.6f, %.6f] is inside restricted zone '%s'",
                            position.getLng(), position.getLat(), zone.getName()));
        }
    }

    /**
     * Checks if position is inside restricted zone (without asserting)
     */
    public static boolean isInRestrictedZone(Position position, List<RequestRegion.Region> restrictedZones) {
        for (RequestRegion.Region zone : restrictedZones) {
            if (isPointInPolygon(position, zone.getVertices())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ray casting algorithm for point-in-polygon test
     */
    public static boolean isPointInPolygon(Position point, List<Position> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }

        int n = polygon.size();
        boolean inside = false;
        double x = point.getLng();
        double y = point.getLat();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i).getLng();
            double yi = polygon.get(i).getLat();
            double xj = polygon.get(j).getLng();
            double yj = polygon.get(j).getLat();

            if (((yi > y) != (yj > y)) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)) {
                inside = !inside;
            }
        }

        return inside;
    }

    /**
     * Compares two positions with tolerance
     */
    public static boolean positionsEqual(Position p1, Position p2, double tolerance) {
        if (p1 == null || p2 == null) return false;
        return Math.abs(p1.getLng() - p2.getLng()) <= tolerance &&
                Math.abs(p1.getLat() - p2.getLat()) <= tolerance;
    }

    /**
     * Flattens all delivery paths into single list of positions
     */
    public static List<Position> flattenAllPaths(List<CalcDeliveryPathResponse.DeliveryPath> paths) {
        if (paths == null) return Collections.emptyList();
        return paths.stream()
                .filter(p -> p.getFlightPath() != null)
                .flatMap(p -> p.getFlightPath().stream())
                .collect(Collectors.toList());
    }

    /**
     * Calculates Euclidean distance between two positions
     */
    public static double distance(Position p1, Position p2) {
        double dLng = p1.getLng() - p2.getLng();
        double dLat = p1.getLat() - p2.getLat();
        return Math.sqrt(dLng * dLng + dLat * dLat);
    }

    /**
     * Asserts each move in path is exactly MOVE_STEP or hover (0)
     * Key for mutation testing - catches arithmetic operator changes
     */
    public static void assertValidMoveSteps(List<Position> path) {
        assertNotNull(path, "Path should not be null");

        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            double dist = distance(current, next);

            boolean isHover = dist < TOLERANCE;
            boolean isValidMove = Math.abs(dist - MOVE_STEP) < TOLERANCE;

            assertTrue(isHover || isValidMove,
                    String.format("Invalid move step at index %d: distance=%.8f (expected 0 or %.8f)",
                            i, dist, MOVE_STEP));
        }
    }

    /**
     * Asserts path is continuous (no teleportation)
     */
    public static void assertPathContinuous(List<Position> path) {
        assertNotNull(path, "Path should not be null");

        for (int i = 0; i < path.size() - 1; i++) {
            Position current = path.get(i);
            Position next = path.get(i + 1);
            double dist = distance(current, next);

            assertTrue(dist <= MOVE_STEP + TOLERANCE,
                    String.format("Path discontinuity at index %d: distance=%.8f exceeds max step %.8f",
                            i, dist, MOVE_STEP));
        }
    }

    /**
     * Asserts path starts at expected position
     */
    public static void assertStartsAt(List<Position> path, Position expected) {
        assertNotNull(path, "Path should not be null");
        assertFalse(path.isEmpty(), "Path should not be empty");

        Position first = path.get(0);
        assertTrue(positionsEqual(first, expected, CLOSE_ENOUGH),
                String.format("Path should start at [%.6f, %.6f] but started at [%.6f, %.6f]",
                        expected.getLng(), expected.getLat(), first.getLng(), first.getLat()));
    }

    /**
     * Counts number of hover moves in path
     */
    public static int countHovers(List<Position> path) {
        if (path == null || path.size() < 2) return 0;

        int hovers = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            if (positionsEqual(path.get(i), path.get(i + 1), TOLERANCE)) {
                hovers++;
            }
        }
        return hovers;
    }

    /**
     * Calculates total path distance
     */
    public static double totalPathDistance(List<Position> path) {
        if (path == null || path.size() < 2) return 0;

        double total = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            total += distance(path.get(i), path.get(i + 1));
        }
        return total;
    }

    /**
     * Verifies no segment crosses restricted zone boundary
     * More thorough than just checking waypoints
     */
    public static void assertNoSegmentCrossesRestrictedZone(
            List<Position> path,
            List<RequestRegion.Region> restrictedZones) {

        if (path == null || path.size() < 2) return;

        for (int i = 0; i < path.size() - 1; i++) {
            Position p1 = path.get(i);
            Position p2 = path.get(i + 1);

            for (RequestRegion.Region zone : restrictedZones) {
                assertFalse(segmentIntersectsPolygon(p1, p2, zone.getVertices()),
                        String.format("Segment from [%.6f,%.6f] to [%.6f,%.6f] crosses restricted zone '%s'",
                                p1.getLng(), p1.getLat(), p2.getLng(), p2.getLat(), zone.getName()));
            }
        }
    }

    /**
     * Checks if line segment intersects polygon
     */
    private static boolean segmentIntersectsPolygon(Position p1, Position p2, List<Position> polygon) {
        // Check if endpoints are inside
        if (isPointInPolygon(p1, polygon) || isPointInPolygon(p2, polygon)) {
            return true;
        }

        // Check if segment crosses any edge
        int n = polygon.size();
        for (int i = 0; i < n; i++) {
            Position v1 = polygon.get(i);
            Position v2 = polygon.get((i + 1) % n);

            if (segmentsIntersect(p1, p2, v1, v2)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if two line segments intersect
     */
    private static boolean segmentsIntersect(Position a1, Position a2, Position b1, Position b2) {
        double d1 = direction(b1, b2, a1);
        double d2 = direction(b1, b2, a2);
        double d3 = direction(a1, a2, b1);
        double d4 = direction(a1, a2, b2);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }

        return false;
    }

    private static double direction(Position p1, Position p2, Position p3) {
        return (p3.getLng() - p1.getLng()) * (p2.getLat() - p1.getLat()) -
                (p2.getLng() - p1.getLng()) * (p3.getLat() - p1.getLat());
    }
}