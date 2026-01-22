package ilpREST.ilp_submission_1.testutil;

import ilpREST.ilp_submission_1.dto.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/**
 * Factory for creating test delivery requests with known properties
 *
 * Implements Equivalence Partitioning - provides requests from different equivalence classes
 */
public class TestRequestFactory {

    private static final Position APPLETON_TOWER = new Position(-3.1863580788986368, 55.94468066708487);

    // ==================== Date/Time Utilities ====================

    public static LocalDate getValidTestDate() {
        LocalDate today = LocalDate.now();
        DayOfWeek day = today.getDayOfWeek();

        if (day == DayOfWeek.SATURDAY) {
            return today.plusDays(2);
        } else if (day == DayOfWeek.SUNDAY) {
            return today.plusDays(1);
        }
        return today;
    }

    public static LocalTime getValidTestTime() {
        return LocalTime.of(14, 0);
    }

    // ==================== Standard Deliveries ====================

    public static List<MedDispatchRec> createSimpleNorthDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        requirements.setMaxCost(null);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    public static List<MedDispatchRec> createSimpleSouthDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.186, 55.942));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    public static List<MedDispatchRec> createSimpleEastDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.182, 55.945));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    public static List<MedDispatchRec> createSimpleWestDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.190, 55.945));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    public static List<MedDispatchRec> createMultiDeliveryRequest() {
        List<MedDispatchRec> deliveries = new ArrayList<>();
        LocalDate testDate = getValidTestDate();

        MedDispatchRec d1 = new MedDispatchRec();
        d1.setId(1);
        d1.setDate(testDate);
        d1.setTime(LocalTime.of(10, 0));
        d1.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements req1 = new MedDispatchRec.Requirements();
        req1.setCapacity(1.0);
        req1.setCooling(false);
        req1.setHeating(false);
        d1.setRequirements(req1);
        deliveries.add(d1);

        MedDispatchRec d2 = new MedDispatchRec();
        d2.setId(2);
        d2.setDate(testDate);
        d2.setTime(LocalTime.of(11, 0));
        d2.setDelivery(new Position(-3.183, 55.944));

        MedDispatchRec.Requirements req2 = new MedDispatchRec.Requirements();
        req2.setCapacity(1.0);
        req2.setCooling(false);
        req2.setHeating(false);
        d2.setRequirements(req2);
        deliveries.add(d2);

        return deliveries;
    }

    // ==================== Boundary Value Deliveries ====================

    /**
     * Delivery at minimum distance from service point (boundary)
     */
    public static List<MedDispatchRec> createMinDistanceDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        // Just one step away
        delivery.setDelivery(new Position(APPLETON_TOWER.getLng() + 0.00015, APPLETON_TOWER.getLat()));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    /**
     * Delivery at moderate distance
     */
    public static List<MedDispatchRec> createMediumDistanceDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.183, 55.947));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    /**
     * Delivery at maximum reasonable distance (boundary)
     */
    public static List<MedDispatchRec> createMaxDistanceDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.175, 55.950));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    // ==================== Equivalence Class Deliveries ====================

    /**
     * Delivery with minimal capacity requirement
     */
    public static List<MedDispatchRec> createMinCapacityDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(0.1);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    /**
     * Delivery with cooling requirement
     */
    public static List<MedDispatchRec> createCoolingRequiredDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(true);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    /**
     * Delivery with heating requirement
     */
    public static List<MedDispatchRec> createHeatingRequiredDelivery() {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(1);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(new Position(-3.184, 55.946));

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(true);
        delivery.setRequirements(requirements);

        return Collections.singletonList(delivery);
    }

    /**
     * Create N deliveries for stress testing
     */
    public static List<MedDispatchRec> createNDeliveries(int n) {
        List<MedDispatchRec> deliveries = new ArrayList<>();
        LocalDate testDate = getValidTestDate();

        double baseLng = -3.184;
        double baseLat = 55.945;

        for (int i = 0; i < n; i++) {
            MedDispatchRec delivery = new MedDispatchRec();
            delivery.setId(i + 1);
            delivery.setDate(testDate);
            delivery.setTime(LocalTime.of(10 + (i % 8), (i * 10) % 60));

            double lng = baseLng + (i % 3) * 0.001;
            double lat = baseLat + (i / 3) * 0.001;
            delivery.setDelivery(new Position(lng, lat));

            MedDispatchRec.Requirements req = new MedDispatchRec.Requirements();
            req.setCapacity(1.0);
            req.setCooling(false);
            req.setHeating(false);
            delivery.setRequirements(req);

            deliveries.add(delivery);
        }

        return deliveries;
    }

    /**
     * Delivery with specific ID
     */
    public static MedDispatchRec createDeliveryWithId(int id, Position location) {
        MedDispatchRec delivery = new MedDispatchRec();
        delivery.setId(id);
        delivery.setDate(getValidTestDate());
        delivery.setTime(getValidTestTime());
        delivery.setDelivery(location);

        MedDispatchRec.Requirements requirements = new MedDispatchRec.Requirements();
        requirements.setCapacity(1.0);
        requirements.setCooling(false);
        requirements.setHeating(false);
        delivery.setRequirements(requirements);

        return delivery;
    }

    // ==================== Restricted Area Positions ====================

    /**
     * Position definitely inside George Square
     */
    public static Position getInsideGeorgeSquare() {
        return new Position(-3.1885, 55.9435);
    }

    /**
     * Position definitely inside Bristo Square
     */
    public static Position getInsideBristoSquare() {
        return new Position(-3.1888, 55.9455);
    }

    /**
     * Position just outside George Square (boundary)
     */
    public static Position getJustOutsideGeorgeSquare() {
        return new Position(-3.1865, 55.9430);
    }

    // ==================== Standard Areas ====================

    public static List<RequestRegion.Region> getStandardRestrictedAreas() {
        List<RequestRegion.Region> restrictedAreas = new ArrayList<>();

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

        return restrictedAreas;
    }

    public static Position getAppletonTower() {
        return APPLETON_TOWER;
    }
}