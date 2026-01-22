package ilpREST.ilp_submission_1.services;

import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class DeliveryPathServiceTest {

    @Autowired
    private DeliveryPathService deliveryPathService;

    @Autowired
    private PositionService positionService;

    private Position appletonTower;
    private Position oceanTerminal;
    private List<RequestRegion.Region> restrictedAreas;

    @BeforeEach
    void setUp() {
        // Service Points
        appletonTower = new Position(-3.1863580788986368, 55.94468066708487);
        oceanTerminal = new Position(-3.17732611501824, 55.981186279333656);

        // Setup restricted areas
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

        // Dr Elsie Inglis Quadrangle
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

        // Bristo Square Open Area
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

        // Bayes Central Area
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
    void testSimpleDeliveryNearAppletonTower() {
        // Test delivery close to Appleton Tower, should not cross restricted areas
        Position deliveryLocation = new Position(-3.185, 55.945); // East of Appleton Tower

        MedDispatchRec request = createTestRequest(
                1,
                LocalDate.of(2025, 1, 6), // Monday
                LocalTime.of(14, 0),
                deliveryLocation,
                1.0,
                false,
                false,
                null
        );

        CalcDeliveryPathResponse response = deliveryPathService.calculateDeliveryPath(
                Collections.singletonList(request)
        );

        System.out.println("=== Simple Delivery Test ===");
        printResponse(response);

        assertNotNull(response);
        assertTrue(response.getTotalCost() > 0);

        // Verify no path crosses restricted areas
        verifyNoRestrictedAreaViolations(response);
    }

    @Test
    void testDeliveryRequiringNavigationAroundRestrictions() {
        // Delivery west of Appleton Tower - requires navigating around George Square
        Position deliveryLocation = new Position(-3.192, 55.944);

        MedDispatchRec request = createTestRequest(
                2,
                LocalDate.of(2025, 1, 6), // Monday
                LocalTime.of(14, 0),
                deliveryLocation,
                1.0,
                false,
                false,
                null
        );

        CalcDeliveryPathResponse response = deliveryPathService.calculateDeliveryPath(
                Collections.singletonList(request)
        );

        System.out.println("=== Navigation Around Restrictions Test ===");
        printResponse(response);
        verifyNoRestrictedAreaViolations(response);
    }

    @Test
    void testMultipleDeliveriesOnSameDay() {
        List<MedDispatchRec> requests = Arrays.asList(
                createTestRequest(3, LocalDate.of(2025, 1, 6), LocalTime.of(14, 0),
                        new Position(-3.185, 55.945), 1.0, false, false, null),
                createTestRequest(4, LocalDate.of(2025, 1, 6), LocalTime.of(15, 0),
                        new Position(-3.184, 55.946), 1.0, false, false, null)
        );

        CalcDeliveryPathResponse response = deliveryPathService.calculateDeliveryPath(requests);

        System.out.println("=== Multiple Deliveries Test ===");
        printResponse(response);

        verifyNoRestrictedAreaViolations(response);
    }

    @Test
    void testDeliveryInDifferentDirections() {
        // Test deliveries in all cardinal directions from Appleton Tower
        List<MedDispatchRec> requests = Arrays.asList(
                // North
                createTestRequest(5, LocalDate.of(2025, 1, 6), LocalTime.of(10, 0),
                        new Position(-3.186, 55.946), 1.0, false, false, null),
                // South
                createTestRequest(6, LocalDate.of(2025, 1, 6), LocalTime.of(11, 0),
                        new Position(-3.186, 55.943), 1.0, false, false, null),
                // East
                createTestRequest(7, LocalDate.of(2025, 1, 6), LocalTime.of(12, 0),
                        new Position(-3.184, 55.944), 1.0, false, false, null),
                // West - OUTSIDE restricted areas
                createTestRequest(8, LocalDate.of(2025, 1, 6), LocalTime.of(13, 0),
                        new Position(-3.189, 55.9455), 1.0, false, false, null)  // Changed coordinates
        );

        CalcDeliveryPathResponse response = deliveryPathService.calculateDeliveryPath(requests);

        System.out.println("=== Multi-Direction Deliveries Test ===");
        printResponse(response);

        verifyNoRestrictedAreaViolations(response);
    }

    @Test
    void testPathGenerationDebug() {
        // Specific test to debug path generation
        Position start = appletonTower;
        Position end = new Position(-3.192, 55.944); // West, crosses George Square area

        System.out.println("=== Path Generation Debug ===");
        System.out.println("Start: " + start);
        System.out.println("End: " + end);
        System.out.println("Distance: " + positionService.distance(start, end) + " degrees");
        System.out.println("Distance in moves: " + Math.ceil(positionService.distance(start, end) / 0.00015));

        // Check if direct path crosses restricted areas
        System.out.println("\nChecking restricted areas:");
        for (RequestRegion.Region region : restrictedAreas) {
            boolean startInside = positionService.isInRegion(start, region.getVertices());
            boolean endInside = positionService.isInRegion(end, region.getVertices());
            System.out.println(region.getName() + ":");
            System.out.println("  Start inside: " + startInside);
            System.out.println("  End inside: " + endInside);
        }
    }

    private MedDispatchRec createTestRequest(
            int id,
            LocalDate date,
            LocalTime time,
            Position delivery,
            double capacity,
            boolean cooling,
            boolean heating,
            Double maxCost
    ) {
        MedDispatchRec request = new MedDispatchRec();
        request.setId(id);
        request.setDate(date);
        request.setTime(time);
        request.setDelivery(delivery);

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(capacity);
        requirements.setCooling(cooling);
        requirements.setHeating(heating);
        requirements.setMaxCost(maxCost);
        request.setRequirements(requirements);

        return request;
    }

    private void verifyNoRestrictedAreaViolations(CalcDeliveryPathResponse response) {
        if (response.getDronePaths() == null) return;

        for (CalcDeliveryPathResponse.DronePath dronePath : response.getDronePaths()) {
            for (CalcDeliveryPathResponse.DeliveryPath deliveryPath : dronePath.getDeliveries()) {
                for (Position pos : deliveryPath.getFlightPath()) {
                    for (RequestRegion.Region region : restrictedAreas) {
                        assertFalse(
                                positionService.isInRegion(pos, region.getVertices()),
                                "Path crosses restricted area: " + region.getName() + " at " + pos
                        );
                    }
                }
            }
        }
    }

    private void printResponse(CalcDeliveryPathResponse response) {
        System.out.println("Total Cost: " + response.getTotalCost());
        System.out.println("Total Moves: " + response.getTotalMoves());

        if (response.getDronePaths() != null) {
            for (CalcDeliveryPathResponse.DronePath dronePath : response.getDronePaths()) {
                System.out.println("Drone " + dronePath.getDroneId() + ":");
                for (CalcDeliveryPathResponse.DeliveryPath deliveryPath : dronePath.getDeliveries()) {
                    System.out.println("  Delivery " + deliveryPath.getDeliveryId() +
                            ": " + deliveryPath.getFlightPath().size() + " waypoints");
                }
            }
        }
        System.out.println();
    }
}