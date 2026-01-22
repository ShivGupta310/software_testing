package ilpREST.ilp_submission_1.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
public class CalcDeliveryPathResponse {
    @JsonProperty("totalCost")
    private double totalCost;
    @JsonProperty("totalMoves")
    private int totalMoves;
    @JsonProperty("dronePaths")
    private List<DronePath> dronePaths;

    public CalcDeliveryPathResponse() {}

    public CalcDeliveryPathResponse(double totalCost, int totalMoves,
                                    List<DronePath> dronePaths
                                    ) {
        this.totalCost = totalCost;
        this.totalMoves = totalMoves;
        this.dronePaths = dronePaths;

    }

    // Getters and setters
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public int getTotalMoves() { return totalMoves; }
    public void setTotalMoves(int totalMoves) { this.totalMoves = totalMoves; }

    public List<DronePath> getDronePaths() { return dronePaths; }
    public void setDronePaths(List<DronePath> dronePaths) { this.dronePaths = dronePaths; }


    public static class DronePath {

        @JsonProperty("droneId")
        private String droneId;
        @JsonProperty("deliveries")
        private List<DeliveryPath> deliveries;

        public DronePath() {}

        public DronePath(String droneId, List<DeliveryPath> deliveries) {
            this.droneId = droneId;
            this.deliveries = deliveries;
        }

        public String getDroneId() { return droneId; }
        public void setDroneId(String droneId) { this.droneId = droneId; }

        public List<DeliveryPath> getDeliveries() { return deliveries; }
        public void setDeliveries(List<DeliveryPath> deliveries) { this.deliveries = deliveries; }
    }

    public static class DeliveryPath{
        @JsonProperty("deliveryId")
        private Integer deliveryId; // null for return leg
        @JsonProperty("flightPath")
        private List<Position> flightPath;

        public DeliveryPath() {}

        public DeliveryPath(Integer deliveryId, List<Position> flightPath) {
            this.deliveryId = deliveryId;
            this.flightPath = flightPath;
        }

        public Integer getDeliveryId() { return deliveryId; }
        public void setDeliveryId(Integer deliveryId) { this.deliveryId = deliveryId; }

        public List<Position> getFlightPath() { return flightPath; }
        public void setFlightPath(List<Position> flightPath) { this.flightPath = flightPath; }
    }
}
