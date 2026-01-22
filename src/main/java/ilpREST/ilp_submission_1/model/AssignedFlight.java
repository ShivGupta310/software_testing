package ilpREST.ilp_submission_1.model;

import ilpREST.ilp_submission_1.dto.CalcDeliveryPathResponse;
import ilpREST.ilp_submission_1.dto.MedDispatchRec;
import java.util.List;

public class AssignedFlight {
    private final String droneId;
    private final Long servicePointId;
    private final List<MedDispatchRec> deliveries;
    private final List<CalcDeliveryPathResponse.DeliveryPath> deliveryPaths;
    private final int totalMoves;
    private final double flightCost;

    public AssignedFlight(String droneId, Long servicePointId,
                          List<MedDispatchRec> deliveries,
                          List<CalcDeliveryPathResponse.DeliveryPath> deliveryPaths,
                          int totalMoves, double flightCost) {
        this.droneId = droneId;
        this.servicePointId = servicePointId;
        this.deliveries = deliveries;
        this.deliveryPaths = deliveryPaths;
        this.totalMoves = totalMoves;
        this.flightCost = flightCost;
    }

    public String getDroneId() { return droneId; }
    public Long getServicePointId() { return servicePointId; }
    public List<MedDispatchRec> getDeliveries() { return deliveries; }
    public List<CalcDeliveryPathResponse.DeliveryPath> getDeliveryPaths() { return deliveryPaths; }
    public int getTotalMoves() { return totalMoves; }
    public double getFlightCost() { return flightCost; }
}