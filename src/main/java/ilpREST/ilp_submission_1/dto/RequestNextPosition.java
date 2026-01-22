package ilpREST.ilp_submission_1.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RequestNextPosition {

    @Valid
    @NotNull
    private Position start;

    @NotNull
    private Double angle;

    public Position getStart() { return start; }
    public void setStart(Position start) { this.start = start; }

    public Double getAngle() { return angle; }
    public void setAngle(Double angle) { this.angle = angle; }

}
