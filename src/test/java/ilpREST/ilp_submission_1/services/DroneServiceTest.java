package ilpREST.ilp_submission_1.services;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import ilpREST.ilp_submission_1.dto.*;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class DroneServiceTest {

    private DroneService service;
    private final AvailabilityService availabilityService = new AvailabilityService("https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/");;
    private final PositionService positionService = new  PositionService();

    @BeforeEach
    void setUp() {
        this.service = new DroneService(
                this.availabilityService,
                positionService,
                "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/");

    }

    @Test
    void testGetAllDrones(){
        List<Drone> drones = service.getAllDrones();
        assertEquals(10, drones.size());
    }

    @Test
    void testDronesWithCoolingTrue(){
        List<String> ids = service.getDronesWithCooling(true);
        for (String id: ids){
            Optional<Drone> d = service.getDroneDetails(id);
            assertNotNull(d);
            Drone drone = d.orElseThrow();
            assertTrue(drone.getCapability().isCooling());

        }
    }

    @Test
    void testDronesWIthCoolingFalse(){
        List<String> ids = service.getDronesWithCooling(false);
        for (String id: ids){
            Optional<Drone> d = service.getDroneDetails(id);
            assertNotNull(d);
            Drone drone = d.orElseThrow();
            assertFalse(drone.getCapability().isCooling());
        }
    }

    @Test
    void testQueryAsPathCoolingFalse(){
        String attr = "cOOlINg";
        String value = "FaLsE  ";
        List<String> res = service.queryAsPath(attr, value);
        List<String> control = service.getDronesWithCooling(false);
        assertEquals(control, res);
    }

    @Test
    void testQueryAsPathCoolingTrue(){
        List<String> res = service.queryAsPath("cooling         ", "TRUE");
        assertEquals(res, service.getDronesWithCooling(true));
    }

    @Test
    void testQueryAsPathCapacity(){
        Optional<Drone> d = service.getDroneDetails("4");
        Drone.Capability cap = d.orElseThrow().getCapability();
        Double capacity = cap.getCapacity();
        List<String> res = service.queryAsPath("Capacity", capacity.toString());
        boolean found = false;
        for (String id: res){
            if (id.equals("4")){
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    void testQueryAsPathHeating(){
        List<String> res = service.queryAsPath("heating", "true");
        for (String id: res){
            Optional<Drone> d = service.getDroneDetails(id);
            Drone.Capability cap = d.orElseThrow().getCapability();
            assertTrue(cap.isHeating());
        }
    }

    @Test
    void testQueryAsPathBrokenString(){
        List<String> res = service.queryAsPath("maxMovez", "9hundred");
        assertEquals(0, res.size());
    }


    //Test query
    @Test
    void testQuery(){
        List<AttributeQuery> queries = new ArrayList<>();
        AttributeQuery q1 = new AttributeQuery();
        q1.setAttribute("capacity");
        q1.setOperator(">");
        q1.setValue("4");
        AttributeQuery q2 = new AttributeQuery();
        q2.setAttribute("cooling");
        q2.setOperator("=");
        q2.setValue("true");
        queries.add(q1); queries.add(q2);
        List<String> res = service.query(queries);

        for (String id: res){
            Optional<Drone> d = service.getDroneDetails(id);
            Drone.Capability cap = d.orElseThrow().getCapability();
            assertTrue(cap.isCooling());
            assertTrue(cap.getCapacity() > 4);
        }
    }

    @Test
    void testGetServicePointDrones(){
        List<ServicePointInfo> spis = availabilityService.getServicePointInfos();
        for  (ServicePointInfo spi: spis){
            System.out.println(spi.toString());
        }
    }
}
