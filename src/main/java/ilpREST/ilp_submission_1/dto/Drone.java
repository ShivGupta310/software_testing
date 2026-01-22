package ilpREST.ilp_submission_1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Drone {
    @JsonProperty("id")

    private String id;
    @JsonProperty("name")

    private String name;

    @JsonProperty("capability")

    private Capability capability;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Capability getCapability() { return capability; }
    public void setCapability(Capability capability) { this.capability = capability; }

    @Override
    public String toString(){
        return "Drone:" + this.getId();
    }

    public boolean canHandle(double reqCap, boolean needsCooling, boolean needsHeating){
        if (this.capability == null) {
            return false;
        }
        if (this.capability.getCapacity() < reqCap) {
            return false;
        }
        if (needsCooling && !this.capability.isCooling()) {
            return false;
        }
        if (needsHeating && !this.capability.isHeating()) {
            return false;
        }
        return true;
    }

    public static class Capability{
        @JsonProperty("cooling")

        private boolean cooling;
        @JsonProperty("heating")

        private boolean heating;
        @JsonProperty("capacity")

        private double capacity;
        @JsonProperty("maxMoves")

        private double maxMoves;
        @JsonProperty("costPerMove")


        private double costPerMove;
        @JsonProperty("costInitial")

        private double costInitial;
        @JsonProperty("costFinal")

        private double costFinal;

        //getters and setters
        public boolean isCooling() { return cooling; }
        public void setCooling(boolean cooling) { this.cooling = cooling; }

        public boolean isHeating() { return heating; }
        public void setHeating(boolean heating) { this.heating = heating; }

        public double getCapacity() { return capacity; }
        public void setCapacity(double capacity) { this.capacity = capacity; }

        public double getMaxMoves() { return maxMoves; }
        public void setMaxMoves(double maxMoves) { this.maxMoves = maxMoves; }

        public double getCostPerMove() { return costPerMove; }
        public void setCostPerMove(double costPerMove) { this.costPerMove = costPerMove; }

        public double getCostInitial() { return costInitial; }
        public void setCostInitial(double costInitial) { this.costInitial = costInitial; }

        public double getCostFinal() { return costFinal; }
        public void setCostFinal(double costFinal) { this.costFinal = costFinal; }

    }


}
