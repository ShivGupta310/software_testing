package ilpREST.ilp_submission_1.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;
import java.time.LocalTime;

public class MedDispatchRec {
    @JsonProperty("id")
    @NotNull
    private int id;

    @JsonProperty("date")

    private LocalDate date;
    @JsonProperty("time")

    private LocalTime time;
    @JsonProperty("requirements")

    private Requirements requirements;

    @JsonProperty("delivery")
    Position delivery;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }

    public Position getDelivery() { return delivery; }
    public void setDelivery(Position delivery) { this.delivery = delivery; }

    public Requirements getRequirements() { return requirements; }
    public void setRequirements(Requirements requirements) { this.requirements = requirements; }


    public static class Requirements{

        @JsonProperty("capacity")
        @NotNull
        private double capacity;

        @JsonProperty("cooling")
        private boolean cooling;
        @JsonProperty("heating")
        private boolean heating;
        @JsonProperty("maxCost")
        private Double maxCost; //nullable

        public double getCapacity() { return capacity; }
        public void setCapacity(double capacity) { this.capacity = capacity; }

        public boolean isCooling() { return cooling; }
        public void setCooling(boolean cooling) { this.cooling = cooling; }

        public boolean isHeating() { return heating; }
        public void setHeating(boolean heating) { this.heating = heating; }

        public Double getMaxCost() { return maxCost; }
        public void setMaxCost(Double maxCost) { this.maxCost = maxCost; }

    }

}
