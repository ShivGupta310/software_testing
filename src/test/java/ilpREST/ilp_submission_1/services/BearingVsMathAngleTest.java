package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import ilpREST.ilp_submission_1.services.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BearingVsMathAngleTest {

    @Autowired
    private PositionService positionService;

    @Test
    void analyzeTheActualBug() {
        Position start = new Position(-3.1863580788986368, 55.94468066708487);
        Position target = new Position(-3.192, 55.944);

        // Target is WEST and slightly SOUTH of start
        double dLng = target.getLng() - start.getLng();
        double dLat = target.getLat() - start.getLat();

        System.out.println("=== GEOMETRIC ANALYSIS ===");
        System.out.println("Delta Lng: " + dLng + " (negative = target is WEST)");
        System.out.println("Delta Lat: " + dLat + " (negative = target is SOUTH)");
        System.out.println("Target is: WEST and SOUTH of start\n");

        // What does computeBearing return?
        double navBearing = computeNavigationBearing(start, target);
        System.out.println("Navigation bearing (0°=North): " + navBearing + "°");
        System.out.println("This means: " + describeNavBearing(navBearing) + "\n");

        // What SHOULD the math angle be to go West-Southwest?
        // West = 180°, Southwest = 225° in math coords
        System.out.println("=== WHAT MATH ANGLE SHOULD WE USE? ===");
        System.out.println("To go WEST in math coords: 180°");
        System.out.println("To go SOUTHWEST in math coords: ~225°");

        // What math angle would actually point toward target?
        double correctMathAngle = Math.toDegrees(Math.atan2(dLat, dLng));
        if (correctMathAngle < 0) correctMathAngle += 360;
        System.out.println("Correct math angle (atan2): " + correctMathAngle + "°\n");

        // What's the relationship?
        System.out.println("=== THE CONVERSION ===");
        System.out.println("Navigation bearing: " + navBearing + "°");
        System.out.println("Correct math angle: " + correctMathAngle + "°");
        System.out.println("Difference: " + (correctMathAngle - navBearing) + "°");
        System.out.println("Conversion formula: mathAngle = 90 - navBearing");
        System.out.println("Verification: 90 - " + navBearing + " = " + (90 - navBearing) + "°");

        // Test if the conversion works
        double convertedAngle = (90 - navBearing + 360) % 360;
        System.out.println("\nDoes (90 - navBearing) = correctMathAngle?");
        System.out.println("Converted: " + convertedAngle + "°");
        System.out.println("Expected: " + correctMathAngle + "°");
        System.out.println("Match: " + (Math.abs(convertedAngle - correctMathAngle) < 1));
    }

    private double computeNavigationBearing(Position from, Position to) {
        double dLng = Math.toRadians(to.getLng() - from.getLng());
        double lat1 = Math.toRadians(from.getLat());
        double lat2 = Math.toRadians(to.getLat());

        double y = Math.sin(dLng) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng);

        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360) % 360;
    }

    private String describeNavBearing(double bearing) {
        if (bearing >= 337.5 || bearing < 22.5) return "North";
        if (bearing >= 22.5 && bearing < 67.5) return "Northeast";
        if (bearing >= 67.5 && bearing < 112.5) return "East";
        if (bearing >= 112.5 && bearing < 157.5) return "Southeast";
        if (bearing >= 157.5 && bearing < 202.5) return "South";
        if (bearing >= 202.5 && bearing < 247.5) return "Southwest";
        if (bearing >= 247.5 && bearing < 292.5) return "West";
        return "Northwest";
    }
}