package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import ilpREST.ilp_submission_1.services.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CartesianAngleTest {

    @Autowired
    private PositionService positionService;

    @Test
    void compareSphericalVsCartesianBearing() {
        Position start = new Position(-3.1863580788986368, 55.94468066708487);
        Position target = new Position(-3.192, 55.944);

        double dLng = target.getLng() - start.getLng();
        double dLat = target.getLat() - start.getLat();

        System.out.println("=== COMPARING BEARING CALCULATIONS ===\n");

        // Spherical bearing (great circle - what you're using)
        double sphericalBearing = computeSphericalBearing(start, target);
        System.out.println("Spherical bearing (great circle): " + sphericalBearing + "°");

        // Cartesian angle (what you SHOULD use with Euclidean distance)
        double cartesianAngle = Math.toDegrees(Math.atan2(dLat, dLng));
        if (cartesianAngle < 0) cartesianAngle += 360;
        System.out.println("Cartesian angle (atan2): " + cartesianAngle + "°");

        System.out.println("Difference: " + Math.abs(sphericalBearing - cartesianAngle) + "°\n");

        // Now test if using Cartesian angle works
        System.out.println("=== TESTING PATH WITH CARTESIAN ANGLE ===\n");

        Position current = start;
        for (int step = 0; step < 50; step++) {
            double dx = target.getLng() - current.getLng();
            double dy = target.getLat() - current.getLat();

            // Cartesian angle in math coords (0° = East)
            double mathAngle = Math.toDegrees(Math.atan2(dy, dx));
            if (mathAngle < 0) mathAngle += 360;

            // Round to nearest 22.5°
            double roundedAngle = Math.round(mathAngle / 22.5) * 22.5;

            Position next = positionService.nextPosition(current, roundedAngle);
            double dist = positionService.distance(next, target);

            if (step < 10 || step % 5 == 0) {
                System.out.printf("Step %2d: mathAngle=%.2f° rounded=%.1f° dist=%.10f\n",
                        step, mathAngle, roundedAngle, dist);
            }

            current = next;

            if (positionService.isCloseTo(current, target)) {
                System.out.println("\n✓✓✓ REACHED TARGET at step " + step + " ✓✓✓");
                System.out.println("Final distance: " + positionService.distance(current, target));
                return;
            }
        }

        System.out.println("\nFinal distance: " + positionService.distance(current, target));
    }

    private double computeSphericalBearing(Position from, Position to) {
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