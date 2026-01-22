package ilpREST.ilp_submission_1.dto;
import java.util.*;
public class GeoJsonFeature {
    private final String type = "Feature";
    private Map<String, Object> properties = new LinkedHashMap<>();
    private GeoJsonGeometry geometry;

    public GeoJsonFeature geometry(GeoJsonGeometry g) {
        this.geometry = g;
        return this;
    }

    public GeoJsonFeature prop(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public String getType() { return type; }
    public Map<String, Object> getProperties() { return properties; }
    public GeoJsonGeometry getGeometry() { return geometry; }
}
