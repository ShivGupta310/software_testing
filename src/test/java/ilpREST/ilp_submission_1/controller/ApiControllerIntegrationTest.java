package ilpREST.ilp_submission_1.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import ilpREST.ilp_submission_1.dto.*;
import org.apache.coyote.Request;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ApiControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void testHealthUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void testUid() throws Exception {
        mockMvc.perform(get("/api/v1/uid"))
                .andExpect(status().isOk())
                .andExpect(content().string("s2484890"));
    }

    @Test
    void  testDistanceToFar() throws Exception {
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(0.0); p1.setLng(0.0);
        Position p2 = new Position(); p2.setLat(5.0); p2.setLng(12.0);
        req.setPosition1(p1);
        req.setPosition2(p2);

        MvcResult res = mockMvc.perform(post("/api/v1/distanceTo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        double dist = objectMapper.readValue(res.getResponse().getContentAsString(), Double.class);
        assertEquals(13.0,dist,1e-9);

    }

    @Test
    void testDistanceToClose() throws Exception {
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(55.946233); p1.setLng(-3.192473);
        Position p2 = new Position(); p2.setLat(55.942617); p2.setLng(-3.192473);
        req.setPosition1(p1);
        req.setPosition2(p2);

        MvcResult res = mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        double dist = objectMapper.readValue(res.getResponse().getContentAsString(), Double.class);
        assertEquals(0.003616,dist,1e-9);

    }

    @Test
    //one of the two positions in request is null
    void testDistanceToNullPosition() throws Exception {
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(0.0); p1.setLng(0.0);
        req.setPosition1(p1);
        // do not set p2

        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    //both positions are initialised in req but the lat/lng of one position is null
    void testDistanceToNullCoordinates() throws Exception{
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(0.0); p1.setLng(0.0);
        Position p2 = new Position(); p1.setLat(1.0); // do not set lng for p2
        req.setPosition1(p1);
        req.setPosition2(p2);


        mockMvc.perform(post("/api/v1/distanceTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void testIsCloseToTrue() throws Exception {
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(1.0); p1.setLng(0.0001);
        Position p2 = new Position(); p2.setLat(1.0); p2.setLng(0.0);
        req.setPosition1(p1);
        req.setPosition2(p2);

        MvcResult res = mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Boolean close = objectMapper.readValue(res.getResponse().getContentAsString(), Boolean.class);
        assertTrue(close);

    }

    @Test
    void testIsCloseToFalse() throws Exception{
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(0.0); p1.setLng(0.0);
        Position p2 = new Position(); p2.setLat(1.0); p2.setLng(0.0);
        req.setPosition1(p1);
        req.setPosition2(p2);

        MvcResult res = mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Boolean close = objectMapper.readValue(res.getResponse().getContentAsString(), Boolean.class);
        assertFalse(close);
    }

    @Test
    void testIsCloseToNullPosition() throws Exception{
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(0.0); p1.setLng(0.0);
        req.setPosition1(p1);
        // do not set p2

        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

    }

    @Test
    void testIsCloseToNullCoordinates() throws Exception{
        RequestDistance req = new RequestDistance();
        Position p1 = new Position(); p1.setLat(0.0); p1.setLng(0.0);
        Position p2 = new Position(); p1.setLat(1.0); // do not set lng for p2
        req.setPosition1(p1);
        req.setPosition2(p2);


        mockMvc.perform(post("/api/v1/isCloseTo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testNextPositionNorth() throws Exception{
        RequestNextPosition req = new  RequestNextPosition();
        Position s = new Position(); s.setLat(1.0); s.setLng(0.0);
        double angle = 90.0;
        req.setAngle(angle); req.setStart(s);

        MvcResult res = mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();


        Position end = objectMapper.readValue(res.getResponse().getContentAsString(), Position.class);
        assertEquals(0.0, end.getLng(), 1e-9);
        assertEquals(1.00015, end.getLat(), 1e-9);
    }

    @Test
    void testNextPositionWSW() throws Exception{
        RequestNextPosition req = new  RequestNextPosition();
        Position s = new Position(); s.setLat(55.0); s.setLng(55.0);
        double angle = 247.5;
        req.setAngle(angle); req.setStart(s);

        MvcResult res = mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();


        Position end = objectMapper.readValue(res.getResponse().getContentAsString(), Position.class);
        assertEquals(54.99994259749, end.getLng(), 1e-9);
        assertEquals(54.99986141807, end.getLat(), 1e-9);

    }

    @Test
    void testNextPositionNullStart() throws Exception{
        RequestNextPosition req = new  RequestNextPosition();
        req.setAngle(90.0);

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    //No/null angle sent in req
    void testNextPositionNullAngle() throws Exception{
        RequestNextPosition req = new  RequestNextPosition();
        Position s = new  Position(); s.setLat(0.0); s.setLng(0.0);
        req.setStart(s);

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    //angle that is not a multiple of 22.5 deg (not one of 16 cardinal dirs)
    void testNextPositionIllegalAngle() throws Exception{
        RequestNextPosition req = new  RequestNextPosition();
        Position s = new  Position(); s.setLat(0.0); s.setLng(0.0);
        req.setStart(s);
        req.setAngle(36.7);

        mockMvc.perform(post("/api/v1/nextPosition")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInRegionClosedPolygonTrue() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(0.5); p.setLng(0.5);
        req.setPosition(p);

        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setName("quad");
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 1.0),
                make(0.0,0.0)
        ));
        req.setRegion(reg);

        MvcResult r = mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Boolean in = objectMapper.readValue(r.getResponse().getContentAsString(), Boolean.class);
        assertTrue(in);

    }

    @Test
    void testInRegionClosedPolygonFalse() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(1.5); p.setLng(1.0);
        req.setPosition(p);

        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setName("quad");
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 1.0),
                make(0.0,0.0)
        ));
        req.setRegion(reg);

        MvcResult r = mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Boolean in = objectMapper.readValue(r.getResponse().getContentAsString(), Boolean.class);
        assertFalse(in);
    }

    @Test
    //vertex list >= 4 but vertex[0] != vertex[-1]
    void testInRegionOpenPolygon() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(1.5); p.setLng(1.0);
        req.setPosition(p);

        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setName("quad");
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 1.0)
        ));
        req.setRegion(reg);

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInRegionNullPoint() throws Exception{
        RequestRegion req = new RequestRegion();
        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setName("quad");
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 1.0),
                make(0.0, 0.0)
        ));
        req.setRegion(reg);

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInRegionNullVertices() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(1.5); p.setLng(1.0);
        req.setPosition(p);

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInRegionTooFewVertices() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(1.5); p.setLng(1.0);
        req.setPosition(p);

        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setName("quad");
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 0.0)
        ));
        req.setRegion(reg);

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    //Null name --> syntactically invalid, don't allow
    void testInRegionNullName() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(1.5); p.setLng(1.0);
        req.setPosition(p);

        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 1.0),
                make(0.0,0.0)
        ));
        req.setRegion(reg);

        mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    //Empty name --> syntax is valid
    void testInRegionEmptyName() throws Exception{
        RequestRegion req = new RequestRegion();
        Position p = new Position(); p.setLat(1.5); p.setLng(1.0);
        req.setPosition(p);

        RequestRegion.Region reg = new RequestRegion.Region();
        reg.setName("");
        reg.setVertices(List.of(
                make(0.0, 0.0),
                make(1.0, 0.0),
                make(1.0, 1.0),
                make(0.0, 1.0),
                make(0.0,0.0)
        ));
        req.setRegion(reg);

        MvcResult r = mockMvc.perform(post("/api/v1/isInRegion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        Boolean in = objectMapper.readValue(r.getResponse().getContentAsString(), Boolean.class);
        assertFalse(in);
    }

    //Helper for making vertices for inRegion
    private static Position make(double lng, double lat) {
        Position p = new Position();
        p.setLng(lng);
        p.setLat(lat);
        return p;
    }
}
