package ilpREST.ilp_submission_1.controller;
import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.services.*;
import ilpREST.ilp_submission_1.dto.RequestDistance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final PositionService positionService;
    private final DroneService droneService;
    private final DeliveryPathService deliveryPathService;

    private final GeoJsonService geoJsonService;

    public ApiController(PositionService positionService, DroneService droneService, DeliveryPathService deliveryPathService, GeoJsonService geoJsonService) {
        this.positionService = positionService;
        this.droneService = droneService;
        this.deliveryPathService = deliveryPathService;
        this.geoJsonService = geoJsonService;

    }

    @GetMapping("/uid")
    public ResponseEntity<String> getUid() {
        return ResponseEntity.ok("s2484890");
    }

    @PostMapping("/distanceTo")
    public ResponseEntity<?>distanceTo(@Valid @RequestBody RequestDistance req){
        return ResponseEntity.ok(positionService.distance(req.getPosition1(), req.getPosition2()));
    }

    @PostMapping("/isCloseTo")
    public ResponseEntity<?> isCloseTo(@Valid @RequestBody RequestDistance req){
        return  ResponseEntity.ok(positionService.isCloseTo(req.getPosition1(), req.getPosition2()));
    }

    @PostMapping("/nextPosition")
    public ResponseEntity<?> nextPosition(@Valid @RequestBody RequestNextPosition req){
        return ResponseEntity.ok(positionService.nextPosition(req.getStart(), req.getAngle()));
    }

    @PostMapping("/isInRegion")
    public ResponseEntity<?> isInRegion(@Valid @RequestBody RequestRegion req) {
        return ResponseEntity.ok(positionService.isInRegion(req.getPosition(), req.getRegion().getVertices()));
    }

    //CW2 Endpoint
    @GetMapping("/dronesWithCooling/{state}")
    public ResponseEntity<List<String>> dronesWithCooling(@PathVariable boolean state) {
        return ResponseEntity.ok(droneService.getDronesWithCooling(state));
    }

    @GetMapping("/droneDetails/{id}")
    public ResponseEntity<Drone> droneDetails(@PathVariable String id) {
        Optional<Drone> drone = droneService.getDroneDetails(id);

        if (drone.isPresent()) {
            return ResponseEntity.ok(drone.get());
        }
        else{
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/queryAsPath/{attributeName}/{attributeValue}")
    public ResponseEntity<List<String>> queryAsPath(@PathVariable String attributeName, @PathVariable String attributeValue) {
        return ResponseEntity.ok(droneService.queryAsPath(attributeName, attributeValue));
    }

    @PostMapping("/queryAvailableDrones")
    public ResponseEntity<List<String>> queryAvailableDrones(@Valid @RequestBody List<MedDispatchRec> requests){
        return ResponseEntity.ok(droneService.queryAvailableDrones(requests));
    }

    @PostMapping("/calcDeliveryPath")
    public ResponseEntity<CalcDeliveryPathResponse> calcDeliveryPath(@Valid @RequestBody List<MedDispatchRec> requests) {
        try {
            CalcDeliveryPathResponse response = deliveryPathService.calculateDeliveryPath(requests);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("calcDeliveryPathAsGeoJson")
    public ResponseEntity<GeoJsonFeatureCollection> calcDeliveryPathAsGeoJson(@Valid @RequestBody List<MedDispatchRec> requests){
        try{
            CalcDeliveryPathResponse flightPathsResponse = deliveryPathService.calculateDeliveryPath(requests);
            GeoJsonFeatureCollection res = geoJsonService.convert(flightPathsResponse);
            return ResponseEntity.ok(res);

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }
}
