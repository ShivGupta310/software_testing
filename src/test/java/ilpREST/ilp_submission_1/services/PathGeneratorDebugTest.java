package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
public class PathGeneratorDebugTest {

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
    void testDirectPathToWest() {
        Position target = new Position(-3.192, 55.944);

        System.out.println("=== Testing Path West (through George Square) ===");
        System.out.println("Start: " + appletonTower);
        System.out.println("Target: " + target);
        System.out.println("Direct distance: " + positionService.distance(appletonTower, target));

        // Test if direct bearing crosses restricted area
        double bearing = computeBearing(appletonTower, target);
        System.out.println("Direct bearing: " + bearing + "°");

        // Sample points along direct path
        System.out.println("\nSampling direct path:");
        for (int i = 0; i <= 10; i++) {
            double fraction = i / 10.0;
            Position sample = interpolate(appletonTower, target, fraction);

            boolean inRestricted = false;
            String restrictedName = "";
            for (RequestRegion.Region region : restrictedAreas) {
                if (positionService.isInRegion(sample, region.getVertices())) {
                    inRestricted = true;
                    restrictedName = region.getName();
                    break;
                }
            }

            System.out.printf("  %d%%: %s - %s\n",
                    (int)(fraction * 100),
                    sample,
                    inRestricted ? "IN " + restrictedName : "clear"
            );
        }

        // Try to generate actual path
        List<MedDispatchRec> deliveries = Collections.singletonList(
                createDelivery(1, target)
        );

        List<CalcDeliveryPathResponse.DeliveryPath> result =
                pathGenerator.generateFlightPath(appletonTower, deliveries, restrictedAreas);

        if (result == null) {
            System.out.println("\n❌ PATH GENERATION FAILED");

            // Try alternative routes manually
            System.out.println("\nTrying alternative bearings:");
            double[] alternatives = {0, 45, 90, 135, 180, 225, 270, 315};
            for (double alt : alternatives) {
                Position step = positionService.nextPosition(appletonTower, alt);
                boolean clear = true;
                for (RequestRegion.Region region : restrictedAreas) {
                    if (positionService.isInRegion(step, region.getVertices())) {
                        clear = false;
                        break;
                    }
                }
                System.out.printf("  %3.0f°: %s\n", alt, clear ? "✓ clear" : "✗ blocked");
            }
        } else {
            System.out.println("\n✓ Path generated successfully");
            for (CalcDeliveryPathResponse.DeliveryPath path : result) {
                System.out.println("  Waypoints: " + path.getFlightPath().size());
            }
        }
    }

    @Test
    void testPathNorth() {
        Position target = new Position(-3.186, 55.946);
        testPath("North", target);
    }

    @Test
    void testPathSouth() {
        Position target = new Position(-3.186, 55.943);
        testPath("South", target);
    }

    @Test
    void testPathEast() {
        Position target = new Position(-3.184, 55.944);
        testPath("East", target);
    }

    private void testPath(String direction, Position target) {
        System.out.println("=== Testing Path " + direction + " ===");
        System.out.println("Target: " + target);

        List<MedDispatchRec> deliveries = Collections.singletonList(
                createDelivery(1, target)
        );

        List<CalcDeliveryPathResponse.DeliveryPath> result =
                pathGenerator.generateFlightPath(appletonTower, deliveries, restrictedAreas);

        if (result == null) {
            System.out.println("❌ FAILED");

            // Check why
            for (RequestRegion.Region region : restrictedAreas) {
                if (positionService.isInRegion(target, region.getVertices())) {
                    System.out.println("  Target is inside: " + region.getName());
                }
            }
        } else {
            System.out.println("✓ SUCCESS - " + result.get(0).getFlightPath().size() + " waypoints");
        }
        System.out.println();
    }

    private Position interpolate(Position start, Position end, double fraction) {
        double lng = start.getLng() + (end.getLng() - start.getLng()) * fraction;
        double lat = start.getLat() + (end.getLat() - start.getLat()) * fraction;
        return new Position(lng, lat);
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

    private MedDispatchRec createDelivery(int id, Position delivery) {
        MedDispatchRec rec = new MedDispatchRec();
        rec.setId(id);
        rec.setDelivery(delivery);
        return rec;
    }
}