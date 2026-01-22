package ilpREST.ilp_submission_1.services;
import ilpREST.ilp_submission_1.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.web.mappings.MappingsEndpoint;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.util.stream.Collectors;
import java.time.*;

@Service
public class DroneService {
    private final String ilpEndpoint;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AvailabilityService availabilityService;
    private final PositionService positionService;

    final double STEP_CONST = 0.00015;


    public DroneService(
            AvailabilityService availabilityService,
            PositionService positionService,
            @Value("${ilp.endpoint}") String ilpEndpoint
    ) {
        this.availabilityService = availabilityService;
        this.positionService = positionService;
        this.ilpEndpoint = ilpEndpoint;
    }


    /**
     * Gets information on all the drones
     * @return List of Drone DTO objects
     */
    public List<Drone> getAllDrones(){
        Drone[] response = restTemplate.getForObject(ilpEndpoint + "/drones", Drone[].class);
        if (response == null) return new ArrayList<>();
        return Arrays.asList(response);
    }

    /**
     *
     * @param cooling_state: true or false boolean val
     * @return List of string drone ids
     */
    public List<String> getDronesWithCooling(boolean cooling_state){
        return getAllDrones().stream()
                .filter(drone -> drone.getCapability().isCooling() == cooling_state)
                .map(Drone::getId).collect(Collectors.toList());
    }

    /**
     * Query by ID to get Full information on Capabilities,
     * @param id: string drone id
     * @return Optional Drone, Optional empty if not found
     */
    public Optional<Drone> getDroneDetails(String id){
        for (Drone drone : getAllDrones()){
            if (drone.getId().equals(id)){
                return Optional.of(drone);
            }
        }

        return Optional.empty();
    }


    public List<String> queryAsPath(String attribute, String value){
        List<String> res  = new ArrayList<>();
        for (Drone drone : getAllDrones()){
            if (_matchSingleAttributeValue(drone, attribute, value)) {
                res.add(drone.getId());
            }
        }
        return res;
    }

    /**
     * Helper function to determine if drone capabilities meet string attribute and value
     * Equality (=) Check
     * @param drone
     * @param attr
     * @param val
     * @return T/F
     */
    private boolean _matchSingleAttributeValue(Drone drone, String attr, String val){
        Drone.Capability cap = drone.getCapability();
        if (cap == null) return false;
        switch (attr.toLowerCase().strip()) {
            case "heating":
                return Boolean.parseBoolean(val) == cap.isHeating();
            case "maxmoves":
                return Double.parseDouble(val) ==  cap.getMaxMoves();
            case "costpermove":
                return Double.parseDouble(val) ==  cap.getCostPerMove();
            case "costinitial":
                return  Double.parseDouble(val) ==  cap.getCostInitial();
            case "costfinal":
                return  Double.parseDouble(val) ==  cap.getCostFinal();
            case "cooling":
                return Boolean.parseBoolean(val) == cap.isCooling();
            case "capacity":
                return Double.parseDouble(val) == cap.getCapacity();
            default:
                return false;
        }
    }

    //Handle queries
    public List<String> query(List<AttributeQuery> queries){
        List<String> res = new  ArrayList<>();

        for (Drone drone: getAllDrones()){

            boolean meetsConditions = true;

            for (AttributeQuery q: queries){
                if (!_handleQuery(drone, q)){
                    meetsConditions = false;
                    break;
                }
            }
            if (meetsConditions){
                res.add(drone.getId());
            }
        }

        return res;
    }

    private boolean _handleQuery(Drone d, AttributeQuery q){
        String attr = q.getAttribute().toLowerCase().strip();
        String op = q.getOperator().toLowerCase().strip();
        String val = q.getValue().toLowerCase().strip();

        Drone.Capability cap = d.getCapability();
        if (cap == null) return false;

        try{
            switch (attr){
                case "heating":
                    return cap.isHeating() == Boolean.parseBoolean(val);
                case "cooling":
                    return cap.isCooling() == Boolean.parseBoolean(val);
                case "maxmoves":
                    return _compareNumericAttr(cap.getMaxMoves(), op, Double.parseDouble(val));
                case "costpermove":
                    return _compareNumericAttr(cap.getCostPerMove(), op, Double.parseDouble(val));
                case "costinitial":
                    return _compareNumericAttr(cap.getCostInitial(), op, Double.parseDouble(val));
                case "costfinal":
                    return _compareNumericAttr(cap.getCostFinal(), op, Double.parseDouble(val));
                case "capacity":
                    return _compareNumericAttr(cap.getCapacity(), op, Double.parseDouble(val));
                default:
                    return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Helper for numeric comparisons
     * @param n1 (left operand)
     * @param op (operator)
     * @param n2 (right operand)
     * @return boolean result
     */
    private boolean _compareNumericAttr(double n1, String op, double n2){
        switch (op){
            case "=":
                return n1 == n2;
            case "!=":
                return n1 != n2;
            case "<":
                return n1 < n2;
            case ">":
                return n1 > n2;
            default:
                return false;
        }
    }

    //Availability Queries

    public List<String> queryAvailableDrones(List<MedDispatchRec> requests){
        if (requests == null || requests.isEmpty()) return new ArrayList<>();

        //Validate input
        for (MedDispatchRec req : requests) {
            if (req.getDate() == null || req.getTime() == null ||
                    req.getDelivery() == null || req.getRequirements() == null) {
                throw new IllegalArgumentException("Invalid request: missing required fields");
            }
        }

        //Precompute global constraints
        double requiredMaxCapacity = requests.stream()
                .mapToDouble(r -> r.getRequirements().getCapacity())
                .max()
                .orElse(0.0);

        boolean needsCooling = requests.stream()
                .anyMatch(r -> r.getRequirements().isCooling());

        boolean needsHeating = requests.stream()
                .anyMatch(r -> r.getRequirements().isHeating());

        //Fetch external data
        List<Drone> allDrones = getAllDrones();
        Map<String, Map<Long, List<ServicePointInfo.AvailabilityInfo>>> droneAvailability =
                availabilityService.buildDroneAvailabilityMap();
        Map<Long, Position> spPositions = availabilityService.buildServicePointPositionsMap();

        //Group the Requests by Date
        Map<LocalDate, List<MedDispatchRec>> requestsByDate = new HashMap<>();
        for (MedDispatchRec req : requests) {
            requestsByDate.computeIfAbsent(req.getDate(), k -> new ArrayList<>()).add(req);
        }

        List<String> resIds = new ArrayList<>();

        for (Drone drone : allDrones){
            if (drone.getCapability() == null){
                continue;
            }
            Drone.Capability cap = drone.getCapability();
            // Capability checks
            if (cap.getCapacity() < requiredMaxCapacity) continue;
            if (needsCooling && !cap.isCooling()) continue;
            if (needsHeating && !cap.isHeating()) continue;

            // Get availability for this drone (map of SP -> windows)
            Map<Long, List<ServicePointInfo.AvailabilityInfo>> spAvailability =
                    droneAvailability.get(drone.getId());

            if (spAvailability == null || spAvailability.isEmpty()) continue;

            // Check all requests fall within availability at SOME service point
            boolean allAvailable = requests.stream().allMatch(req ->
                    availabilityService.isAvailableAtAnyServicePoint(spAvailability, req.getDate(), req.getTime())
            );

            if (!allAvailable) continue;

            // Get service points for this drone
            Set<Long> droneServicePoints = spAvailability.keySet();
            // For each date partition, try bundling then fallback
            boolean droneCanHandleAllDates = true;

            for (Map.Entry<LocalDate, List<MedDispatchRec>> dateEntry : requestsByDate.entrySet()) {
                List<MedDispatchRec> dateRequests = dateEntry.getValue();

                //Try to bundle requests
                //TODO: continue here
                boolean bundlingSucceeded = tryBundling(drone, dateRequests, spAvailability, spPositions);
                if (!bundlingSucceeded){
                    // Fallback to singular flights
                    boolean singularSucceeded = trySingularFlights(drone, dateRequests, droneServicePoints, spPositions);

                    if (!singularSucceeded) {
                        droneCanHandleAllDates = false;
                        break;
                    }
                }
            }
            if (droneCanHandleAllDates) {
                resIds.add(drone.getId());
            }
        }
        return resIds;
    }

    private boolean tryBundling(
            Drone drone,
            List<MedDispatchRec> dateRequests,
            Map<Long, List<ServicePointInfo.AvailabilityInfo>> spAvailability,
            Map<Long, Position> spPositions
    ){
        // Try each service point where drone is available
        for (Map.Entry<Long, List<ServicePointInfo.AvailabilityInfo>> entry : spAvailability.entrySet()) {
            Long spId =  entry.getKey();
            List<ServicePointInfo.AvailabilityInfo> windows = entry.getValue();

            Position spPos = spPositions.get(spId);
            if (spPos == null) continue;

            // Check if ALL requests on this date are available at THIS specific service point
            boolean allAvailableAtThisSP = dateRequests.stream().allMatch(req ->
                    availabilityService.isAvailableAtServicePoint(windows, req.getDate(), req.getTime())
            );

            if (!allAvailableAtThisSP) continue; // Skip this SP if any request isn't available here

            List<MedDispatchRec> remaining = new ArrayList<>(dateRequests);
            boolean allCovered = true;

            while (!remaining.isEmpty()) {
                //Build greedy flight path
                List<MedDispatchRec> flight = new ArrayList<>();
                Position current = spPos;

                //Const for now
                while (!remaining.isEmpty() && flight.size() < 10) {
                    // Find nearest delivery to current position
                    MedDispatchRec nearest = null;
                    double minDist = Double.MAX_VALUE;

                    for (MedDispatchRec req : remaining) {
                        double dist = positionService.distance(current, req.getDelivery());
                        if (dist < minDist) {
                            minDist = dist;
                            nearest = req;
                        }
                    }

                    if (nearest != null) {
                        flight.add(nearest);
                        remaining.remove(nearest);
                        current = nearest.getDelivery();
                    }
                }

                // Compute route moves: SP -> d1 -> d2 -> ... -> dN -> SP
                int totalMoves = 0;
                Position prev = spPos;

                for (MedDispatchRec delivery : flight) {
                    double dist = positionService.distance(prev, delivery.getDelivery());
                    totalMoves += (int) Math.ceil(dist / STEP_CONST);
                    prev = delivery.getDelivery();
                }

                // Return to SP
                double returnDist = positionService.distance(prev, spPos);
                totalMoves += (int) Math.ceil(returnDist / STEP_CONST);

                // Check maxMoves
                if (totalMoves > drone.getCapability().getMaxMoves()) {
                    allCovered = false;
                    break;
                }

                // Compute cost
                double flightCost = drone.getCapability().getCostInitial() +
                        totalMoves * drone.getCapability().getCostPerMove() +
                        drone.getCapability().getCostFinal();

                double perDeliveryShare = flightCost / flight.size();

                // Check cost constraints
                for (MedDispatchRec req : flight) {
                    Double maxCost = req.getRequirements().getMaxCost();
                    if (maxCost != null && perDeliveryShare > maxCost) {
                        allCovered = false;
                        break;
                    }
                }

                if (!allCovered) break;
            }

            if (allCovered) {
                return true; // Bundling succeeded from this SP
            }
        }
        return false;
    }

    private boolean trySingularFlights(
            Drone drone, List<MedDispatchRec> dateRequests,
            Set<Long> servicePointIds, Map<Long, Position> spPositions
    ) {
        for (MedDispatchRec req : dateRequests) {
            // Find nearest service point for this delivery
            Long nearestSpId = null;
            double minDist = Double.MAX_VALUE;

            for (Long spId : servicePointIds) {
                Position spPos = spPositions.get(spId);
                if (spPos != null) {
                    double dist = positionService.distance(spPos, req.getDelivery());
                    if (dist < minDist) {
                        minDist = dist;
                        nearestSpId = spId;
                    }
                }
            }

            if (nearestSpId == null) return false;

            Position spPos = spPositions.get(nearestSpId);

            // Round trip moves
            double oneWayDist = positionService.distance(spPos, req.getDelivery());
            int moves = (int) Math.ceil(oneWayDist / STEP_CONST) * 2;

            // Check maxMoves
            if (moves > drone.getCapability().getMaxMoves()) {
                return false;
            }

            // Compute cost
            double flightCost = drone.getCapability().getCostInitial() +
                    moves * drone.getCapability().getCostPerMove() +
                    drone.getCapability().getCostFinal();

            // Check maxCost
            Double maxCost = req.getRequirements().getMaxCost();
            if (maxCost != null && flightCost > maxCost) {
                return false;
            }
        }

        return true;
    }

}
