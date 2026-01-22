package ilpREST.ilp_submission_1.services;
import ilpREST.ilp_submission_1.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.*;
import java.util.*;

/**
 *
 */
@Service
public class AvailabilityService {
    private final String ilpEndpoint;
    private final RestTemplate restTemplate;

    public AvailabilityService(@Value("${ilp.endpoint}") String ilpEndpoint) {
        this.ilpEndpoint = ilpEndpoint;
        this.restTemplate = createRestTemplateWithJavaTimeModule();
    }

    private RestTemplate createRestTemplateWithJavaTimeModule() {
        RestTemplate rt = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter conv = new MappingJackson2HttpMessageConverter();
        conv.setObjectMapper(mapper);
        // replace default MappingJackson2HttpMessageConverter to ensure JavaTime support
        List converters = rt.getMessageConverters();
        converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
        converters.add(conv);
        return rt;
    }

    /**
     * 
     */
    public List<ServicePointInfo> getServicePointInfos(){
        ServicePointInfo[] response = restTemplate.getForObject(
            ilpEndpoint + "/drones-for-service-points",
            ServicePointInfo[].class);
        return response != null ? Arrays.asList(response) : new ArrayList<>();
    }

    // ...existing code...

    public List<ServicePointLocation> getServicePointLocations(){
        try {
            ServicePointLocation[] response = restTemplate.getForObject(
                    ilpEndpoint + "/service-points",
                    ServicePointLocation[].class);
            System.out.println("Successfully fetched " + (response != null ? response.length : 0) + " service points");
            return response != null ? Arrays.asList(response) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error fetching service points: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

   /**
    * Fetch restricted regions from the ILP server
     */
    public List<RestrictedArea> getRestrictedAreas() {
        RestrictedArea[] response = restTemplate.getForObject(
                ilpEndpoint + "/restricted-areas",
                RestrictedArea[].class
        );
        if (response == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(response);
    }

    /**
     *
     */
    public Map<String, Map<Long, List<ServicePointInfo.AvailabilityInfo>>> buildDroneAvailabilityMap(){
        Map<String, Map<Long, List<ServicePointInfo.AvailabilityInfo>>> map = new HashMap<>();
        List<ServicePointInfo> spInfos = getServicePointInfos();
        for (ServicePointInfo spi : spInfos) {
            if (spi.getDrones() == null){
                continue;
            }
            long spId = spi.getServicePointId();
            for (ServicePointInfo.ServicePointDrone spd : spi.getDrones()){
                String droneId = spd.getId();
                Map<Long, List<ServicePointInfo.AvailabilityInfo>> spMap = map.computeIfAbsent(
                        droneId, k -> new HashMap<>());

                List<ServicePointInfo.AvailabilityInfo> windows = new ArrayList<>();

                if (spd.getAvailability() != null){
                    for (ServicePointInfo.AvailabilityInfo avail : spd.getAvailability()){
                        windows.add(new ServicePointInfo.AvailabilityInfo(
                                avail.getDayOfWeek(),
                                avail.getFrom(),
                                avail.getUntil()
                        ));
                    }
                }
                spMap.put(spId, windows);
            }
        }
        return map;
    }

    /**
     *
     */
    public Map<Long, Position> buildServicePointPositionsMap(){
        Map<Long, Position> map = new HashMap<>();
        List<ServicePointLocation> sps = getServicePointLocations();
        for (ServicePointLocation sp : sps) {
            map.put(sp.getId(), sp.getLocation());
        }

        return map;
    }

    /**
     *
     */
    public boolean isAvailableAtAnyServicePoint(
            Map<Long, List<ServicePointInfo.AvailabilityInfo>> spAvailability,
            LocalDate date,
            LocalTime time
    ){
        if (spAvailability == null || spAvailability.isEmpty()) return false;
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        //Check if available at any sp
        for (List<ServicePointInfo.AvailabilityInfo> windows : spAvailability.values()) {
            if (windows == null){
                continue;
            }
            for (ServicePointInfo.AvailabilityInfo window : windows){
                if (window.getDayOfWeek().equalsIgnoreCase(dayOfWeek.name()) &&
                        !time.isBefore(window.getFrom()) &&
                        !time.isAfter(window.getUntil())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *
     */
    public boolean isAvailableAtServicePoint(
            List<ServicePointInfo.AvailabilityInfo> windows,
            LocalDate date,
            LocalTime time
    ){
        if (windows == null || windows.isEmpty()) return false;

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        for (ServicePointInfo.AvailabilityInfo window : windows) {
            if (window.getDayOfWeek().equalsIgnoreCase(dayOfWeek.name()) &&
                    !time.isBefore(window.getFrom()) &&
                    !time.isAfter(window.getUntil())) {
                return true;
            }
        }
        return false;
    }
}
