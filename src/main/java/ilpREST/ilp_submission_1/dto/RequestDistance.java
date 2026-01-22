package ilpREST.ilp_submission_1.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;


public class RequestDistance {

    @NotNull
    @Valid
    @JsonProperty("position1")
    private Position position1;

    @NotNull
    @Valid
    @JsonProperty("position2")
    private Position position2;

    public  Position getPosition1() {return position1;}
    public  Position getPosition2() {return position2;}

    public void setPosition1(Position pos1) {this.position1 = pos1;}
    public void setPosition2(Position pos2) {this.position2 = pos2;}


    @Override
    public String toString(){
        return  "position 1: " + position1.toString() + " position 2: " + position2.toString();
    }
}