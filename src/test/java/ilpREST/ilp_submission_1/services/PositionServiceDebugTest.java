package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import ilpREST.ilp_submission_1.services.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class PositionServiceDebugTest {

    @Autowired
    private PositionService positionService;

    @Test
    void diagnoseStepBehavior() {
        Position start = new Position(-3.1863580788986368, 55.94468066708487);
        Position target = new Position(-3.192, 55.944);

        double directDistance = positionService.distance(start, target);
        System.out.println("Direct distance: " + directDistance);
        System.out.println("Expected steps: " + Math.ceil(directDistance / positionService.STEP_CONST));
        System.out.println("STEP_CONST: " + positionService.STEP_CONST);
        System.out.println("ANGLE_CONST: " + positionService.ANGLE_CONST);

        // Test if nextPosition actually moves by STEP_CONST
        Position step1 = positionService.nextPosition(start, 0.0);
        double actualStepDistance = positionService.distance(start, step1);
        System.out.println("\nActual step distance (0°): " + actualStepDistance);
        System.out.println("Step distance / STEP_CONST ratio: " + (actualStepDistance / positionService.STEP_CONST));

        // Test multiple angles
        System.out.println("\nStep distances at different angles:");
        for (double angle = 0; angle < 360; angle += 45) {
            Position step = positionService.nextPosition(start, angle);
            double dist = positionService.distance(start, step);
            System.out.printf("  %3.0f°: %.10f (ratio: %.6f)\n",
                    angle, dist, dist / positionService.STEP_CONST);
        }

        // Simulate direct path
        System.out.println("\nSimulating direct path:");
        Position current = start;
        int steps = 0;
        double bearing = computeBearing(start, target);
        double roundedBearing = Math.round(bearing / 22.5) * 22.5;

        System.out.println("Target bearing: " + bearing);
        System.out.println("Rounded bearing: " + roundedBearing);

        while (positionService.distance(current, target) > positionService.CLOSENESS_CONST && steps < 50) {
            Position next = positionService.nextPosition(current, roundedBearing);
            double distToTarget = positionService.distance(next, target);
            double stepDist = positionService.distance(current, next);

            System.out.printf("Step %d: dist_to_target=%.10f, step_size=%.10f\n",
                    steps, distToTarget, stepDist);

            current = next;
            steps++;
        }

        System.out.println("Total steps taken: " + steps);
        System.out.println("Final distance to target: " + positionService.distance(current, target));
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