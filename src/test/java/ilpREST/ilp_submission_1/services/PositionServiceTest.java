package ilpREST.ilp_submission_1.services;
import ilpREST.ilp_submission_1.dto.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;



public class PositionServiceTest {

    private PositionService service;

    @BeforeEach
    void setUp() {
        this.service = new PositionService();
    }

    @Test
    void testDistanceTo(){
        Position p1 = new Position();
        Position p2 = new Position();
        p1.setLat(0.0); p1.setLng(0.0);
        p2.setLat(5.0); p2.setLng(12.0);
        double d = service.distance(p1, p2);
        assertEquals(13.0, d, 1e-9);

    }

    @Test
    void testIsCloseTrue(){
        Position p1 = new Position();
        Position p2 = new Position();
        p1.setLat(1.0); p1.setLng(1.0);
        p2.setLat(1.00005); p2.setLng(1.00002);
        assertTrue(service.isCloseTo(p1, p2));
    }

    @Test
    void testIsCloseFalse(){
        Position p1 = new Position();
        Position p2 = new Position();
        p1.setLat(1.0); p1.setLng(1.0);
        p2.setLat(1.00016); p2.setLng(1.0);
        assertFalse(service.isCloseTo(p1, p2));
    }

    @Test
    void testNextPosition(){
        Position start = new Position();
        start.setLat(0.0); start.setLng(0.0);
        double angle = 90.0;
        Position res = service.nextPosition(start, angle);
        assertEquals(0.00015, res.getLat(), 1e-9);
        assertEquals(0.0, res.getLng(), 1e-9);
    }

    @Test
    void testNextPositionIllegalAngle(){
        Position start = new Position();
        start.setLat(0.0); start.setLng(0.0);
        double angle = 25.0;

        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.nextPosition(start, angle);
        }, "Expected nextPosition() to throw IllegalArgumentException"
        );
    }


    @Test
    void testInRegionEmptyVertices(){
        Position pt = new Position();
        pt.setLat(0.0); pt.setLng(0.0);
        List<Position> vertices = new ArrayList<>();

        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.isInRegion(pt, vertices);
                }, "Expected isInRegion() to throw IllegalArgumentException"
        );



    }

    @Test
    void testInRegionTwoVertices(){
        Position pt = new Position();
        pt.setLat(0.0); pt.setLng(0.5);

        List<Position> vertices = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(0.0); vtx2.setLng(1.0);
        vertices.add(vtx1); vertices.add(vtx2);

        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.isInRegion(pt, vertices);
                }
        );
    }

    @Test
    void testInRegionTriangleClosedTrue(){
        Position pt = new Position();
        pt.setLat(0.5);  pt.setLng(0.5);

        List<Position> triangle = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(0.0); vtx2.setLng(1.0);
        Position vtx3 = new Position();
        vtx3.setLat(0.75); vtx3.setLng(0.5);
        triangle.add(vtx1); triangle.add(vtx2); triangle.add(vtx3); triangle.add(vtx1);

        assertTrue(service.isInRegion(pt, triangle));
    }
    @Test
    void testInRegionTriangleOpenFalse(){
        Position pt = new Position();
        pt.setLat(0.5);  pt.setLng(0.5);

        List<Position> triangle = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(0.0); vtx2.setLng(1.0);
        Position vtx3 = new Position();
        vtx3.setLat(0.75); vtx3.setLng(0.5);
        triangle.add(vtx1); triangle.add(vtx2); triangle.add(vtx3);

        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.isInRegion(pt, triangle);
                }
        );
    }


    @Test
    void testInRegionTriangleClosedFalse(){
        Position pt = new Position();
        pt.setLat(1.5);  pt.setLng(1.5);

        List<Position> triangle = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(0.0); vtx2.setLng(1.0);
        Position vtx3 = new Position();
        vtx3.setLat(0.75); vtx3.setLng(0.5);
        triangle.add(vtx1); triangle.add(vtx2); triangle.add(vtx3); triangle.add(vtx1);

        assertFalse(service.isInRegion(pt, triangle));
    }

    @Test
    void testInRegionQuadClosedTrue(){
        Position pt = new Position();
        pt.setLat(0.5);  pt.setLng(0.5);

        List<Position> quadrilateral = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(0.0); vtx2.setLng(1.0);
        Position vtx3 = new Position();
        vtx3.setLat(1.0); vtx3.setLng(1.0);
        Position vtx4 = new Position();
        vtx4.setLat(1.0); vtx4.setLng(0.0);

        quadrilateral.add(vtx1); quadrilateral.add(vtx2); quadrilateral.add(vtx3);
        quadrilateral.add(vtx4); quadrilateral.add(vtx1);

        assertTrue(service.isInRegion(pt, quadrilateral));
    }

    @Test
    void testInRegionQuadClosedFalse(){
        Position pt = new Position();
        pt.setLat(2.5);  pt.setLng(2.5);

        List<Position> quadrilateral = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(0.0); vtx2.setLng(1.0);
        Position vtx3 = new Position();
        vtx3.setLat(1.0); vtx3.setLng(1.0);
        Position vtx4 = new Position();
        vtx4.setLat(1.0); vtx4.setLng(0.0);

        quadrilateral.add(vtx1); quadrilateral.add(vtx2); quadrilateral.add(vtx3);
        quadrilateral.add(vtx4); quadrilateral.add(vtx1);

        assertFalse(service.isInRegion(pt, quadrilateral));
    }

    @Test
    void testInRegionQuadOpenFalse1(){
        Position pt = new Position();
        pt.setLat(0.5);  pt.setLng(0.5);

        List<Position> quadrilateral = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(0.0);
        Position vtx2 = new Position();
        vtx2.setLat(1.0); vtx2.setLng(0.0);
        Position vtx3 = new Position();
        vtx3.setLat(1.0); vtx3.setLng(1.0);
        Position vtx4 = new Position();
        vtx4.setLat(0.0); vtx4.setLng(1.0);

        quadrilateral.add(vtx1); quadrilateral.add(vtx2); quadrilateral.add(vtx3);
        quadrilateral.add(vtx4);

        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.isInRegion(pt, quadrilateral);
                }
        );
    }

    @Test
    void testInRegionQuadOpenFalse2(){

        Position pt = new Position();
        pt.setLat(0.5);  pt.setLng(0.5);

        List<Position> quadrilateral = new ArrayList<>();
        Position vtx1 = new Position();
        vtx1.setLat(0.0); vtx1.setLng(1.0);
        Position vtx2 = new Position();
        vtx2.setLat(1.0); vtx2.setLng(1.0);
        Position vtx3 = new Position();
        vtx3.setLat(1.0); vtx3.setLng(0.0);
        Position vtx4 = new Position();
        vtx4.setLat(1.0); vtx4.setLng(0.0);

        quadrilateral.add(vtx1); quadrilateral.add(vtx2); quadrilateral.add(vtx3);
        quadrilateral.add(vtx4);

        IllegalArgumentException thrown = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    service.isInRegion(pt, quadrilateral);
                }
        );

    }
}
