package ilpREST.ilp_submission_1.services;
import ilpREST.ilp_submission_1.dto.*;
import ilpREST.ilp_submission_1.model.*;
import org.springframework.stereotype.Service;
import java.util.*;
import java.time.*;
import java.util.stream.Collectors;

@Service
public class DeliveryPathService {
    private final DroneService droneService;
    private final AvailabilityService availabilityService;
    private final PositionService positionService;
    private final FlightPathGenerator pathGenerator;

    public DeliveryPathService(DroneService droneService,
                               AvailabilityService availabilityService,
                               PositionService positionService,
                               FlightPathGenerator pathGenerator) {
        this.droneService = droneService;
        this.availabilityService = availabilityService;
        this.positionService = positionService;
        this.pathGenerator = pathGenerator;
    }

    public CalcDeliveryPathResponse calculateDeliveryPath(List<MedDispatchRec> requests) {

        // Fetch restricted areas from ILP server
        List<RestrictedArea> restrictedAreas = availabilityService.getRestrictedAreas();
        List<RequestRegion.Region> forbiddenRegions = restrictedAreas.stream()
                .map(area -> {
                    RequestRegion.Region region = new RequestRegion.Region();
                    region.setName(area.getName());
                    region.setVertices(area.getVertices());
                    return region;
                })
                .collect(Collectors.toList());

        // Get ALL drones and build availability/position maps ONCE
        List<Drone> allDrones = droneService.getAllDrones();
        Map<String, Map<Long, List<ServicePointInfo.AvailabilityInfo>>> droneAvailabilityMap =
                availabilityService.buildDroneAvailabilityMap();
        Map<Long, Position> spPositions = availabilityService.buildServicePointPositionsMap();

        // Build a map: droneId -> set of request IDs it can potentially handle
        Map<String, Set<Integer>> droneCapabilityMap = buildDroneCapabilityMap(
                allDrones, requests, droneAvailabilityMap
        );

        // Filter to only drones that can handle at least one request
        List<Drone> candidateDrones = allDrones.stream()
                .filter(d -> droneCapabilityMap.containsKey(d.getId()) &&
                        !droneCapabilityMap.get(d.getId()).isEmpty())
                .collect(Collectors.toList());

        if (candidateDrones.isEmpty()) {
            List<Integer> unassignedIds = requests.stream()
                    .map(MedDispatchRec::getId)
                    .sorted()
                    .collect(Collectors.toList());
            List<String> warnings = List.of("No drones available to handle any requests");
            return new CalcDeliveryPathResponse(0.0, 0, new ArrayList<>());
        }

        Map<LocalDate, List<MedDispatchRec>> requestsByDate = requests.stream()
                .collect(Collectors.groupingBy(MedDispatchRec::getDate));

        List<AssignedFlight> allAssignedFlights = new ArrayList<>();
        Set<Integer> assignedDeliveryIds = new HashSet<>();
        List<String> warnings = new ArrayList<>();

        for (LocalDate date : requestsByDate.keySet().stream().sorted().collect(Collectors.toList())){
            List<MedDispatchRec> dateRequests = requestsByDate.get(date);
            Set<MedDispatchRec> unassigned = new HashSet<>(dateRequests);

            // Phase 2: Greedy multi-drone assignment
            while (!unassigned.isEmpty()) {
                FlightCandidate bestCandidate = null;

                for (Drone drone : candidateDrones) {
                    String droneId = drone.getId();
                    Map<Long, List<ServicePointInfo.AvailabilityInfo>> spAvailability =
                            droneAvailabilityMap.get(droneId);

                    if (spAvailability == null || spAvailability.isEmpty()) {
                        continue;
                    }

                    for (Long spId : spAvailability.keySet()) {
                        Position spPosition = spPositions.get(spId);
                        if (spPosition == null) {
                            continue;
                        }

                        FlightCandidate candidate = buildGreedyFlight(
                                drone, spId, spPosition,
                                new ArrayList<>(unassigned),
                                spAvailability.get(spId),
                                date,
                                forbiddenRegions
                        );

                        if (candidate != null && candidate.getDeliveryCount() > 0) {
                            if (bestCandidate == null || candidate.compareTo(bestCandidate) < 0) {
                                bestCandidate = candidate;
                            }
                        }
                    }
                }

                if (bestCandidate == null || bestCandidate.getDeliveryCount() == 0) {
                    break;
                }

                AssignedFlight assignedFlight = finaliseFlightWithPaths(
                        bestCandidate,
                        spPositions.get(bestCandidate.getServicePointId()),
                        forbiddenRegions
                );

                if (assignedFlight != null) {
                    allAssignedFlights.add(assignedFlight);
                    for (MedDispatchRec delivery : bestCandidate.getDeliveries()) {
                        unassigned.remove(delivery);
                        assignedDeliveryIds.add(delivery.getId());
                    }
                } else {
                    for (MedDispatchRec delivery : bestCandidate.getDeliveries()) {
                        unassigned.remove(delivery);
                        warnings.add("Delivery " + delivery.getId() + " routing failed (no-fly zones)");
                    }
                }
            }

            // Phase 3: Fallback singular flights
            for (MedDispatchRec unassignedReq : new ArrayList<>(unassigned)) {
                boolean assigned = tryAssignSingularFlight(
                        unassignedReq,
                        candidateDrones,
                        droneAvailabilityMap,
                        spPositions,
                        date,
                        forbiddenRegions,
                        allAssignedFlights,
                        warnings
                );

                if (assigned) {
                    assignedDeliveryIds.add(unassignedReq.getId());
                    unassigned.remove(unassignedReq);
                }
            }
        }

        List<Integer> unassignedDeliveryIds = requests.stream()
                .map(MedDispatchRec::getId)
                .filter(id -> !assignedDeliveryIds.contains(id))
                .sorted()
                .collect(Collectors.toList());

        return buildResponse(allAssignedFlights);
    }

    private Map<String, Set<Integer>> buildDroneCapabilityMap(
            List<Drone> allDrones,
            List<MedDispatchRec> requests,
            Map<String, Map<Long, List<ServicePointInfo.AvailabilityInfo>>> droneAvailabilityMap) {

        Map<String, Set<Integer>> capabilityMap = new HashMap<>();

        for (Drone drone : allDrones) {
            String droneId = drone.getId();
            Set<Integer> capableRequestIds = new HashSet<>();

            Map<Long, List<ServicePointInfo.AvailabilityInfo>> spAvailability =
                    droneAvailabilityMap.get(droneId);

            if (spAvailability == null || spAvailability.isEmpty()) {
                continue;
            }

            List<ServicePointInfo.AvailabilityInfo> allWindows = spAvailability.values().stream()
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            for (MedDispatchRec request : requests) {
                if (!drone.canHandle(
                        request.getRequirements().getCapacity(),
                        request.getRequirements().isCooling(),
                        request.getRequirements().isHeating())) {
                    continue;
                }

                boolean available = allWindows.stream()
                        .anyMatch(window -> availabilityService.isAvailableAtServicePoint(
                                List.of(window), request.getDate(), request.getTime()
                        ));

                if (available) {
                    capableRequestIds.add(request.getId());
                }
            }

            if (!capableRequestIds.isEmpty()) {
                capabilityMap.put(droneId, capableRequestIds);
            }
        }

        return capabilityMap;
    }

    private FlightCandidate buildGreedyFlight(
            Drone drone,
            Long spId,
            Position spPosition,
            List<MedDispatchRec> available,
            List<ServicePointInfo.AvailabilityInfo> availabilityWindows,
            LocalDate date,
            List<RequestRegion.Region> forbiddenRegions
    ){
        List<MedDispatchRec> flightDeliveries = new ArrayList<>();
        Set<MedDispatchRec> remaining = new HashSet<>(available);
        Position currentPos = spPosition;
        int movesSoFar = 0;

        remaining.removeIf(req -> !drone.canHandle(
                req.getRequirements().getCapacity(),
                req.getRequirements().isCooling(),
                req.getRequirements().isHeating()
        ));

        remaining.removeIf(req -> !availabilityService.isAvailableAtServicePoint(
                availabilityWindows, date, req.getTime()
        ));

        while (!remaining.isEmpty() && flightDeliveries.size() < 10) {
            MedDispatchRec nearest = findNearestDelivery(currentPos, remaining);
            if (nearest == null) break;

            double legDist = positionService.distance(currentPos, nearest.getDelivery());
            int legMoves = (int) Math.ceil(legDist / positionService.STEP_CONST);

            double returnDist = positionService.distance(nearest.getDelivery(), spPosition);
            int returnMoves = (int) Math.ceil(returnDist / positionService.STEP_CONST);

            int tentativeTotalMoves = movesSoFar + legMoves + returnMoves;

            if (tentativeTotalMoves > drone.getCapability().getMaxMoves()) {
                break;
            }

            flightDeliveries.add(nearest);
            remaining.remove(nearest);
            currentPos = nearest.getDelivery();
            movesSoFar += legMoves;
        }

        if (flightDeliveries.isEmpty()) {
            return null;
        }

        int finalMoves = pathGenerator.calculateTotalMoves(spPosition, flightDeliveries);
        double flightCost = drone.getCapability().getCostInitial() +
                finalMoves * drone.getCapability().getCostPerMove() +
                drone.getCapability().getCostFinal();

        double perDeliveryCost = flightCost / flightDeliveries.size();
        for (MedDispatchRec delivery : flightDeliveries) {
            Double maxCost = delivery.getRequirements().getMaxCost();
            if (maxCost != null && perDeliveryCost > maxCost) {
                return null;
            }
        }

        return new FlightCandidate(drone.getId(), spId, flightDeliveries, finalMoves, flightCost);
    }

    private MedDispatchRec findNearestDelivery(Position curr, Set<MedDispatchRec> deliveries){
        MedDispatchRec nearest = null;
        double minDist = Double.MAX_VALUE;

        List<MedDispatchRec> sortedCandidates = new ArrayList<>(deliveries);
        sortedCandidates.sort(Comparator.comparingInt(MedDispatchRec::getId));

        for (MedDispatchRec candidate : sortedCandidates) {
            double dist = positionService.distance(curr, candidate.getDelivery());
            if (dist < minDist) {
                minDist = dist;
                nearest = candidate;
            }
        }

        return nearest;
    }

    private AssignedFlight finaliseFlightWithPaths(
            FlightCandidate candidate,
            Position spPosition,
            List<RequestRegion.Region> forbiddenRegions
    ){
        List<CalcDeliveryPathResponse.DeliveryPath> paths =
                pathGenerator.generateFlightPath(spPosition, candidate.getDeliveries(), forbiddenRegions);

        if (paths == null) {
            return null;
        }

        return new AssignedFlight(
                candidate.getDroneId(),
                candidate.getServicePointId(),
                candidate.getDeliveries(),
                paths,
                candidate.getTotalMoves(),
                candidate.getFlightCost()
        );
    }

    private boolean tryAssignSingularFlight(
            MedDispatchRec request,
            List<Drone> candidateDrones,
            Map<String, Map<Long, List<ServicePointInfo.AvailabilityInfo>>> droneAvailabilityMap,
            Map<Long, Position> spPositions,
            LocalDate date,
            List<RequestRegion.Region> forbiddenRegions,
            List<AssignedFlight> assignedFlights,
            List<String> warnings
    ) {
        List<Drone> sortedDrones = new ArrayList<>(candidateDrones);
        sortedDrones.sort(Comparator.comparing(Drone::getId));

        for (Drone drone : sortedDrones) {
            if (!drone.canHandle(
                    request.getRequirements().getCapacity(),
                    request.getRequirements().isCooling(),
                    request.getRequirements().isHeating())) {
                continue;
            }

            Map<Long, List<ServicePointInfo.AvailabilityInfo>> spAvailability =
                    droneAvailabilityMap.get(drone.getId());

            if (spAvailability == null) continue;

            List<Long> sortedSpIds = new ArrayList<>(spAvailability.keySet());
            sortedSpIds.sort(Comparator.naturalOrder());

            for (Long spId : sortedSpIds) {
                Position spPosition = spPositions.get(spId);
                if (spPosition == null) continue;

                if (!availabilityService.isAvailableAtServicePoint(
                        spAvailability.get(spId), date, request.getTime())) {
                    continue;
                }

                int moves = pathGenerator.calculateTotalMoves(spPosition, List.of(request));

                if (moves > drone.getCapability().getMaxMoves()) {
                    continue;
                }

                double flightCost = drone.getCapability().getCostInitial() +
                        moves * drone.getCapability().getCostPerMove() +
                        drone.getCapability().getCostFinal();

                Double maxCost = request.getRequirements().getMaxCost();

                if (maxCost != null && flightCost > maxCost) {
                    continue;
                }

                List<CalcDeliveryPathResponse.DeliveryPath> paths =
                        pathGenerator.generateFlightPath(spPosition, List.of(request), forbiddenRegions);

                if (paths != null) {
                    assignedFlights.add(new AssignedFlight(
                            drone.getId(), spId, List.of(request), paths, moves, flightCost
                    ));
                    return true;
                }
            }
        }

        warnings.add("Delivery " + request.getId() + " could not be assigned");
        return false;
    }

    private CalcDeliveryPathResponse buildResponse(
            List<AssignedFlight> assignedFlights
    ){
        Map<String, List<AssignedFlight>> flightsByDrone = assignedFlights.stream()
                .collect(Collectors.groupingBy(AssignedFlight::getDroneId));

        List<CalcDeliveryPathResponse.DronePath> dronePaths = new ArrayList<>();
        double totalCost = 0.0;
        int totalMoves = 0;

        List<String> sortedDroneIds = new ArrayList<>(flightsByDrone.keySet());
        sortedDroneIds.sort(String::compareTo);

        for (String droneId : sortedDroneIds) {
            List<AssignedFlight> flights = flightsByDrone.get(droneId);
            List<CalcDeliveryPathResponse.DeliveryPath> allDeliveryPaths = new ArrayList<>();

            for (AssignedFlight flight : flights) {
                allDeliveryPaths.addAll(flight.getDeliveryPaths());
                totalCost += flight.getFlightCost();
                totalMoves += flight.getTotalMoves();
            }

            dronePaths.add(new CalcDeliveryPathResponse.DronePath(droneId, allDeliveryPaths));
        }

        return new CalcDeliveryPathResponse(totalCost, totalMoves, dronePaths);
    }
}