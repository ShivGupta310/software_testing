package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

@SpringBootTest
public class MultiDirectionDiagnosticTest {

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
    void diagnoseFailingDeliveries() {
        Position[] targets = {
                new Position(-3.186, 55.946),   // North (Delivery 5)
                new Position(-3.186, 55.943),   // South (Delivery 6)
                new Position(-3.184, 55.944),   // East (Delivery 7)
                new Position(-3.188, 55.944)    // West (Delivery 8)
        };

        String[] directions = {"North", "South", "East", "West"};

        for (int i = 0; i < targets.length; i++) {
            System.out.println("=== Testing " + directions[i] + " delivery ===");
            System.out.println("Target: " + targets[i]);

            // Check if target is in restricted area
            boolean targetRestricted = false;
            for (RequestRegion.Region region : restrictedAreas) {
                if (positionService.isInRegion(targets[i], region.getVertices())) {
                    System.out.println("❌ TARGET IS INSIDE: " + region.getName());
                    targetRestricted = true;
                }
            }

            // Check if start is in restricted area
            boolean startRestricted = false;
            for (RequestRegion.Region region : restrictedAreas) {
                if (positionService.isInRegion(appletonTower, region.getVertices())) {
                    System.out.println("❌ START IS INSIDE: " + region.getName());
                    startRestricted = true;
                }
            }

            if (!targetRestricted && !startRestricted) {
                // Check if ANY angle from start is clear
                int clearAngles = 0;
                for (double angle = 0; angle < 360; angle += 22.5) {
                    Position testStep = positionService.nextPosition(appletonTower, angle);
                    boolean blocked = false;
                    for (RequestRegion.Region region : restrictedAreas) {
                        if (positionService.isInRegion(testStep, region.getVertices())) {
                            blocked = true;
                            break;
                        }
                    }
                    if (!blocked) clearAngles++;
                }
                System.out.println("Clear angles from start: " + clearAngles + " / 16");

                if (clearAngles == 0) {
                    System.out.println("❌ START IS COMPLETELY SURROUNDED!");
                }
            }

            // Try to generate path
            MedDispatchRec delivery = new MedDispatchRec();
            delivery.setId(i + 5);
            delivery.setDelivery(targets[i]);

            List<CalcDeliveryPathResponse.DeliveryPath> path =
                    pathGenerator.generateFlightPath(appletonTower, Collections.singletonList(delivery), restrictedAreas);

            if (path == null) {
                System.out.println("❌ Path generation FAILED\n");
            } else {
                System.out.println("✓ Path generated: " + path.get(0).getFlightPath().size() + " waypoints\n");
            }
        }
    }
}