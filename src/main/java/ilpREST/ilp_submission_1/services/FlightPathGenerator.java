package ilpREST.ilp_submission_1.services;
import ilpREST.ilp_submission_1.dto.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class FlightPathGenerator {
    private final PositionService positionService;

    public FlightPathGenerator(PositionService positionService) {
        this.positionService = positionService;
    }

    public List<CalcDeliveryPathResponse.DeliveryPath> generateFlightPath(
            Position servicePoint,
            List<MedDispatchRec> deliveries,
            List<RequestRegion.Region> forbiddenRegions
    ){
        List<CalcDeliveryPathResponse.DeliveryPath> result = new ArrayList<>();
        Position currentPos = servicePoint;

        for (int i = 0; i < deliveries.size(); i++){
            MedDispatchRec delivery = deliveries.get(i);
            Position deliveryPos = delivery.getDelivery();

            List<Position> flightPath = new ArrayList<>();

            // Generate leg from current position to delivery
            List<Position> leg = generateLeg(currentPos, deliveryPos, forbiddenRegions);

            if (leg == null){
                System.out.println("Failed to generate leg from " + currentPos + " to " + deliveryPos);
                return null;
            }

            // Add all positions from leg (including the delivery position at the end)
            for (Position pos : leg) {
                flightPath.add(toResponsePosition(pos));
            }

            // Add the hover at delivery position (duplicate)
            flightPath.add(toResponsePosition(deliveryPos));

            result.add(new CalcDeliveryPathResponse.DeliveryPath(delivery.getId(), flightPath));
            currentPos = deliveryPos;
        }

        // Generate final return leg from last delivery to service point
        List<Position> returnLeg = generateLeg(currentPos, servicePoint, forbiddenRegions);
        if (returnLeg == null) {
            System.out.println("Failed to generate final return leg");
            return null;
        }

        List<Position> returnPath = new ArrayList<>();
        for (int i = 0; i < returnLeg.size(); i++) {
            returnPath.add(toResponsePosition(returnLeg.get(i)));
        }
        result.add(new CalcDeliveryPathResponse.DeliveryPath(null, returnPath));

        return result;
    }

    private List<Position> generateLeg(Position start, Position end, List<RequestRegion.Region> forbiddenRegions){
        // Check if destination is valid
        if (!isValidStep(end, forbiddenRegions)) {
            System.out.println("Destination " + end + " is inside a no-fly zone!");
            return null;
        }

        // Use A* pathfinding
        return aStarPathfind(start, end, forbiddenRegions);
    }

    private List<Position> aStarPathfind(Position start, Position end, List<RequestRegion.Region> forbiddenRegions) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<String, Node> allNodes = new HashMap<>();

        Node startNode = new Node(start, null, 0, positionService.distance(start, end));
        openSet.add(startNode);
        allNodes.put(positionKey(start), startNode);

        int maxIterations = 10000;
        int iterations = 0;

        while (!openSet.isEmpty() && iterations++ < maxIterations) {
            Node current = openSet.poll();

            if (positionService.isCloseTo(current.position, end)) {
                // Reconstruct path
                List<Position> path = new ArrayList<>();
                Node node = current;
                while (node != null) {
                    path.add(0, node.position);
                    node = node.parent;
                }

                // Add exact end position if not already there
                Position lastPos = path.get(path.size() - 1);
                if (!lastPos.getLng().equals(end.getLng()) || !lastPos.getLat().equals(end.getLat())) {
                    path.add(end);
                }

                return path;
            }

            current.closed = true;

            // Explore neighbors
            for (double angle = 0; angle < 360; angle += positionService.ANGLE_CONST) {
                Position neighbor = positionService.nextPosition(current.position, angle);

                if (!isValidStep(neighbor, forbiddenRegions)) {
                    continue;
                }

                String neighborKey = positionKey(neighbor);
                double tentativeG = current.gScore + positionService.STEP_CONST;

                Node neighborNode = allNodes.get(neighborKey);

                if (neighborNode == null) {
                    double h = positionService.distance(neighbor, end);
                    neighborNode = new Node(neighbor, current, tentativeG, h);
                    allNodes.put(neighborKey, neighborNode);
                    openSet.add(neighborNode);
                } else if (!neighborNode.closed && tentativeG < neighborNode.gScore) {
                    openSet.remove(neighborNode);
                    neighborNode.gScore = tentativeG;
                    neighborNode.fScore = tentativeG + neighborNode.hScore;
                    neighborNode.parent = current;
                    openSet.add(neighborNode);
                }
            }
        }

        System.out.println("A* pathfinding failed - no path found after " + iterations + " iterations");
        return null;
    }

    private static class Node {
        Position position;
        Node parent;
        double gScore;
        double hScore;
        double fScore;
        boolean closed = false;

        Node(Position position, Node parent, double gScore, double hScore) {
            this.position = position;
            this.parent = parent;
            this.gScore = gScore;
            this.hScore = hScore;
            this.fScore = gScore + hScore;
        }
    }

    private String positionKey(Position pos) {
        long lngKey = Math.round(pos.getLng() / positionService.STEP_CONST);
        long latKey = Math.round(pos.getLat() / positionService.STEP_CONST);
        return lngKey + "," + latKey;
    }

    private Position stepTowards(Position current, Position target, List<RequestRegion.Region> forbiddenRegions){
        double targetAngle = computeBearing(current, target);
        double roundedAngle = Math.round(targetAngle / positionService.ANGLE_CONST) * positionService.ANGLE_CONST;

        Position directCandidate = positionService.nextPosition(current, roundedAngle);
        if (isValidStep(directCandidate, forbiddenRegions)){
            return directCandidate;
        }

        List<AngleCandidate> candidates = new ArrayList<>();

        for (double angle = 0; angle < 360; angle += positionService.ANGLE_CONST) {
            if (angle == roundedAngle) continue;

            Position candidate = positionService.nextPosition(current, angle);
            if (isValidStep(candidate, forbiddenRegions)) {
                double distToTarget = positionService.distance(candidate, target);
                candidates.add(new AngleCandidate(angle, candidate, distToTarget));
            }
        }

        if (!candidates.isEmpty()) {
            candidates.sort(Comparator.comparingDouble(c -> c.distanceToTarget));
            return candidates.get(0).position;
        }

        return null;
    }

    private static class AngleCandidate {
        double angle;
        Position position;
        double distanceToTarget;

        AngleCandidate(double angle, Position position, double distanceToTarget) {
            this.angle = angle;
            this.position = position;
            this.distanceToTarget = distanceToTarget;
        }
    }

    private boolean isValidStep(Position candidate, List<RequestRegion.Region> forbiddenRegions){
        if (forbiddenRegions != null && !forbiddenRegions.isEmpty()){
            for (RequestRegion.Region region : forbiddenRegions){
                if (positionService.isInRegion(candidate, region.getVertices())){
                    return false;
                }
            }
        }
        return true;
    }

    private double computeBearing(Position curr, Position target){
        double dx = target.getLng() - curr.getLng();
        double dy = target.getLat() - curr.getLat();

        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);

        if (angleDeg < 0) angleDeg += 360;

        return angleDeg;
    }

    private Position toResponsePosition(Position position) {
        double roundedLat = position.getLat();
        double roundedLng = position.getLng();
        return new Position(roundedLng, roundedLat);
    }

    public int calculateTotalMoves(Position servicePoint, List<MedDispatchRec> deliveries){
        int totalMoves = 0;
        Position current = servicePoint;

        for (MedDispatchRec delivery : deliveries) {
            double dist = positionService.distance(current, delivery.getDelivery());
            totalMoves += (int) Math.ceil(dist / positionService.STEP_CONST);
            current = delivery.getDelivery();
        }

        double returnDist = positionService.distance(current, servicePoint);
        totalMoves += (int) Math.ceil(returnDist / positionService.STEP_CONST);

        return totalMoves;
    }
}