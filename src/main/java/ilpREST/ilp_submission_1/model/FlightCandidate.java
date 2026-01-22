package ilpREST.ilp_submission_1.model;

import ilpREST.ilp_submission_1.dto.MedDispatchRec;
import java.util.List;

public class FlightCandidate implements Comparable<FlightCandidate> {
    private final String droneId;
    private final Long servicePointId;
    private final List<MedDispatchRec> deliveries;
    private final int totalMoves;
    private final double flightCost;
    private final double costPerDelivery;

    public FlightCandidate(String droneId, Long servicePointId,
                           List<MedDispatchRec> deliveries,
                           int totalMoves, double flightCost) {
        this.droneId = droneId;
        this.servicePointId = servicePointId;
        this.deliveries = deliveries;
        this.totalMoves = totalMoves;
        this.flightCost = flightCost;
        this.costPerDelivery = deliveries.isEmpty() ? 0.0 : flightCost / deliveries.size();
    }

    public String getDroneId() { return droneId; }
    public Long getServicePointId() { return servicePointId; }
    public List<MedDispatchRec> getDeliveries() { return deliveries; }
    public int getTotalMoves() { return totalMoves; }
    public double getFlightCost() { return flightCost; }
    public double getCostPerDelivery() { return costPerDelivery; }
    public int getDeliveryCount() { return deliveries.size(); }

    @Override
    public int compareTo(FlightCandidate other) {
        int countCompare = Integer.compare(other.deliveries.size(), this.deliveries.size());
        if (countCompare != 0) return countCompare;

        double thisMovesPerDelivery = this.deliveries.isEmpty() ? Double.MAX_VALUE :
                (double) this.totalMoves / this.deliveries.size();
        double otherMovesPerDelivery = other.deliveries.isEmpty() ? Double.MAX_VALUE :
                (double) other.totalMoves / other.deliveries.size();
        int movesCompare = Double.compare(thisMovesPerDelivery, otherMovesPerDelivery);
        if (movesCompare != 0) return movesCompare;

        int costCompare = Double.compare(this.costPerDelivery, other.costPerDelivery);
        if (costCompare != 0) return costCompare;

        int droneCompare = this.droneId.compareTo(other.droneId);
        if (droneCompare != 0) return droneCompare;

        return this.servicePointId.compareTo(other.servicePointId);
    }
}