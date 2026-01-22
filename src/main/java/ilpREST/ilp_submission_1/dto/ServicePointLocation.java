package ilpREST.ilp_submission_1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServicePointLocation {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("location")
    private Position location;

    // No-arg constructor for Jackson
    public ServicePointLocation() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Position getLocation() { return location; }
    public void setLocation(Position location) { this.location = location; }
}