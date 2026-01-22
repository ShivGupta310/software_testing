package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BearingCalculationTest {

    @Autowired
    private PositionService positionService;

    @Test
    void testBearingChangesAsWeMove() {
        Position start = new Position(-3.1863580788986368, 55.94468066708487);
        Position target = new Position(-3.192, 55.944);

        Position current = start;

        System.out.println("=== Testing bearing recalculation ===\n");

        for (int step = 0; step < 20; step++) {
            double bearing = computeBearing(current, target);
            double roundedBearing = Math.round(bearing / 22.5) * 22.5;

            Position next = positionService.nextPosition(current, roundedBearing);
            double dist = positionService.distance(next, target);

            System.out.printf("Step %2d: bearing=%.2f° rounded=%.1f° dist_to_target=%.10f\n",
                    step, bearing, roundedBearing, dist);

            current = next;

            if (positionService.isCloseTo(current, target)) {
                System.out.println("\n✓ REACHED TARGET at step " + step);
                return;
            }
        }

        System.out.println("\n❌ Did not reach target in 20 steps");
        System.out.println("Final distance: " + positionService.distance(current, target));
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