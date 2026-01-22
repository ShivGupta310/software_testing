package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.Position;
import ilpREST.ilp_submission_1.services.PositionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class NextPositionDirectionTest {

    @Autowired
    private PositionService positionService;

    @Test
    void testCardinalDirections() {
        Position start = new Position(0.0, 0.0);

        System.out.println("Testing what direction each angle actually moves:\n");

        Object[][] tests = {
                {0, "0° (should be North)"},
                {90, "90° (should be East)"},
                {180, "180° (should be South)"},
                {270, "270° (should be West)"}
        };

        for (Object[] test : tests) {
            double angle = (double) (int) test[0];
            Position next = positionService.nextPosition(start, angle);

            double deltaLng = next.getLng() - start.getLng();
            double deltaLat = next.getLat() - start.getLat();

            String actualDirection = "";
            if (deltaLat > 0) actualDirection += "North ";
            if (deltaLat < 0) actualDirection += "South ";
            if (deltaLng > 0) actualDirection += "East";
            if (deltaLng < 0) actualDirection += "West";

            System.out.printf("%s:\n", (String)test[1]);
            System.out.printf("  Delta Lng: %+.10f\n", deltaLng);
            System.out.printf("  Delta Lat: %+.10f\n", deltaLat);
            System.out.printf("  Actually moving: %s\n\n", actualDirection.trim());
        }
    }
}