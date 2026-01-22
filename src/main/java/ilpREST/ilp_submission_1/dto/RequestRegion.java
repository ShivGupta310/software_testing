package ilpREST.ilp_submission_1.dto;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class RequestRegion {
    @Valid
    @NotNull
    private Position position;

    @Valid
    @NotNull
    private Region region;

    public Position getPosition() { return position; }
    public void setPosition(Position position) { this.position = position; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }


    public static class Region{
        @NotNull
        private String name;

        @Valid
        @NotNull
        private List<Position> vertices;

        public String getName() {return name;}
        public void setName(String name) {this.name = name;}

        public List<Position> getVertices() {return vertices;}
        public void setVertices(List<Position> vertices) {this.vertices = vertices;}
    }
}
