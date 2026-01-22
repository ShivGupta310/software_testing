package ilpREST.ilp_submission_1.dto;
import java.util.*;
public class GeoJsonGeometry {
    private String type;
    private List<List<Double>> coordinates;


    public GeoJsonGeometry(String type, List<List<Double>> coordinates) {
        this.type = type;
        this.coordinates = coordinates;
    }

    public String getType() { return type; }
    public List<List<Double>> getCoordinates() { return coordinates; }

    public static GeoJsonGeometry point(double lng, double lat) {
        return new GeoJsonGeometry("Point",
                List.of(List.of(lng, lat))
        );
    }

    public static GeoJsonGeometry line(List<List<Double>> coords) {
        return new GeoJsonGeometry("LineString", coords);
    }

}
