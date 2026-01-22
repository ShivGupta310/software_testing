package ilpREST.ilp_submission_1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RestrictedArea {

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private long id;

    @JsonProperty("limits")
    private Limits limits;

    @JsonProperty("vertices")
    private List<Position> vertices;

    public RestrictedArea() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public Limits getLimits() { return limits; }
    public void setLimits(Limits limits) { this.limits = limits; }

    public List<Position> getVertices() { return vertices; }
    public void setVertices(List<Position> vertices) { this.vertices = vertices; }

    public static class Limits {
        @JsonProperty("lower")
        private long lower;

        @JsonProperty("upper")
        private long upper;

        public Limits() {}

        public long getLower() { return lower; }
        public void setLower(long lower) { this.lower = lower; }

        public long getUpper() { return upper; }
        public void setUpper(long upper) { this.upper = upper; }
    }
}