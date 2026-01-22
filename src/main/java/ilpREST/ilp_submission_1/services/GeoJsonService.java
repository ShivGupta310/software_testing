package ilpREST.ilp_submission_1.services;
import ilpREST.ilp_submission_1.dto.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GeoJsonService {

    public GeoJsonFeatureCollection convert(CalcDeliveryPathResponse input){
        GeoJsonFeatureCollection ret = new GeoJsonFeatureCollection();

        for (CalcDeliveryPathResponse.DronePath dp : input.getDronePaths()){
            String droneId = dp.getDroneId();

            for (CalcDeliveryPathResponse.DeliveryPath delivery : dp.getDeliveries()){

                List<List<Double>> coords = delivery.getFlightPath().stream()
                        .map(p -> List.of(p.getLng(), p.getLat()))
                        .toList();

                //LineString for drone flight
                GeoJsonFeature line = new GeoJsonFeature()
                        .prop("droneId", droneId)
                        .prop("deliveryId", delivery.getDeliveryId())
                        .prop("segmentType", delivery.getDeliveryId() == null ? "return" : "delivery")
                        .geometry(GeoJsonGeometry.line(coords)
                        );

                ret.add(line);
            }
        }
        return ret;
    }
}
