package ilpREST.ilp_submission_1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

public class ServicePointInfo {

    @JsonProperty("servicePointId")
    private long servicePointId;

    @JsonProperty("drones")
    private List<ServicePointDrone> drones;

    public long getServicePointId() { return servicePointId; }
    public void setServicePointId(long servicePointId) { this.servicePointId = servicePointId; }

    public List<ServicePointDrone> getDrones() { return drones; }
    public void setDrones(List<ServicePointDrone> drones) { this.drones = drones; }

    @Override
    public String toString() {
        return "ServicePointInfo{" +
                "servicePointId=" + servicePointId +
                ", drones=" + (drones != null ? drones.stream().map(ServicePointDrone::toString).collect(Collectors.joining(", ", "[", "]")) : "null") +
                '}';
    }

    public static class ServicePointDrone {

        @JsonProperty("id")
        private String id;

        @JsonProperty("availability")
        private List<AvailabilityInfo> availability;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public List<AvailabilityInfo> getAvailability() { return availability; }
        public void setAvailability(List<AvailabilityInfo> availability) { this.availability = availability; }

        @Override
        public String toString() {
            return "ServicePointDrone{" +
                    "id='" + id + '\'' +
                    ", availability=" + (availability != null ? availability.stream().map(AvailabilityInfo::toString).collect(Collectors.joining(", ", "[", "]")) : "null") +
                    '}';
        }
    }

    public static class AvailabilityInfo {

        @JsonProperty("dayOfWeek")
        private String dayOfWeek;

        // Here ILP sends "HH:mm:ss" as STRING → LocalTime is perfect
        @JsonProperty("from")
        private LocalTime from;

        @JsonProperty("until")
        private LocalTime until;

        // Add no-arg constructor for Jackson
        public AvailabilityInfo() {
        }

        public AvailabilityInfo(String dayOfWeek, LocalTime from, LocalTime until) {
            this.dayOfWeek = dayOfWeek;
            this.from = from;
            this.until = until;
        }

        public String getDayOfWeek() { return dayOfWeek; }
        public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

        public LocalTime getFrom() { return from; }
        public void setFrom(LocalTime from) { this.from = from; }

        public LocalTime getUntil() { return until; }
        public void setUntil(LocalTime until) { this.until = until; }

        @Override
        public String toString() {
            return "AvailabilityInfo{" +
                    "dayOfWeek='" + dayOfWeek + '\'' +
                    ", from=" + from +
                    ", until=" + until +
                    '}';
        }
    }
}
