package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import ilpREST.ilp_submission_1.services.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AngleSelectionTest {

    @Autowired
    private PositionService positionService;

    @Test
    void testAngleRoundingProblem() {
        Position start = new Position(-3.1863580788986368, 55.94468066708487);
        Position target = new Position(-3.192, 55.944);

        // Calculate true bearing
        double trueBearing = computeBearing(start, target);
        System.out.println("True bearing: " + trueBearing);

        // Find the two adjacent allowed angles
        double lowerAngle = Math.floor(trueBearing / 22.5) * 22.5;
        double upperAngle = lowerAngle + 22.5;

        System.out.println("Lower allowed angle: " + lowerAngle);
        System.out.println("Upper allowed angle: " + upperAngle);
        System.out.println("Difference to lower: " + Math.abs(trueBearing - lowerAngle));
        System.out.println("Difference to upper: " + Math.abs(trueBearing - upperAngle));

        // Test which angle gets us closer to target
        Position stepLower = positionService.nextPosition(start, lowerAngle);
        Position stepUpper = positionService.nextPosition(start, upperAngle);

        double distLower = positionService.distance(stepLower, target);
        double distUpper = positionService.distance(stepUpper, target);

        System.out.println("\nAfter one step:");
        System.out.println("Using " + lowerAngle + "° -> distance to target: " + distLower);
        System.out.println("Using " + upperAngle + "° -> distance to target: " + distUpper);

        if (distUpper < distLower) {
            System.out.println("\n✓ UPPER angle (" + upperAngle + "°) is BETTER");
        } else {
            System.out.println("\n✓ LOWER angle (" + lowerAngle + "°) is BETTER");
        }

        // Now simulate what happens with each angle over multiple steps
        System.out.println("\n=== Simulating 20 steps with LOWER angle (" + lowerAngle + "°) ===");
        simulateSteps(start, target, lowerAngle, 20);

        System.out.println("\n=== Simulating 20 steps with UPPER angle (" + upperAngle + "°) ===");
        simulateSteps(start, target, upperAngle, 20);
    }

    private void simulateSteps(Position start, Position target, double angle, int numSteps) {
        Position current = start;
        for (int i = 0; i < numSteps; i++) {
            Position next = positionService.nextPosition(current, angle);
            double dist = positionService.distance(next, target);
            System.out.printf("Step %2d: distance = %.10f\n", i, dist);
            current = next;
        }
    }

    private double computeBearing(Position from, Position to) {
        double dLng = Math.toRadians(to.getLng() - from.getLng());
        double lat1 = Math.toRadians(from.getLat());
        double lat2 = Math.toRadians(to.getLat());

        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }
}