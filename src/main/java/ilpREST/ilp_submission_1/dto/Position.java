package ilpREST.ilp_submission_1.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Position {
    // Double for non-primitive decimal to allow possible nulls in req to not default to 0.0
    @NotNull
    @JsonProperty("lng")
    private Double lng;

    @NotNull
    @JsonProperty("lat")
    private Double lat;

    //Basic getters and setters
    public Double getLat(){return lat;}
    public Double getLng(){return lng;}

    public void setLat(Double lat){this.lat = lat;}
    public void setLng(Double lng){this.lng = lng;}

    //for debug prints
    @Override
    public String toString(){
        return "lng: " + lng.toString() + "lat: " + lat.toString();
    }

    //Constructors
    //Null for Jackson
    public Position(){

    }

    public Position(Double lng, Double lat){
        this.lng = lng;
        this.lat = lat;
    }
}
