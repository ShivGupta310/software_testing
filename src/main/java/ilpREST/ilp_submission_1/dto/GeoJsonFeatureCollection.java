package ilpREST.ilp_submission_1.dto;
import java.util.*;
public class GeoJsonFeatureCollection {

    private final String type = "FeatureCollection";
    private List<GeoJsonFeature> features = new ArrayList<>();

    public String getType() { return type; }
    public List<GeoJsonFeature> getFeatures() { return features; }
    public void add(GeoJsonFeature f) { features.add(f); }
}
