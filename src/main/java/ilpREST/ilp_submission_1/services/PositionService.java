package ilpREST.ilp_submission_1.services;
import java.util.List;
import ilpREST.ilp_submission_1.dto.Position;
import org.springframework.stereotype.Service;

@Service
public class PositionService {

    final double CLOSENESS_CONST  = 0.00015;
    final double STEP_CONST = 0.00015;
    final double ANGLE_CONST = 22.5;

    public double distance(Position p1, Position p2){
        double dx = p1.getLng() - p2.getLng();
        double dy = p1.getLat() - p2.getLat();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean isCloseTo(Position p1, Position p2){
        return distance(p1,p2) < CLOSENESS_CONST;
    }


    public Position nextPosition(Position start, double angle){

        if (angle % ANGLE_CONST != 0){
            throw new IllegalArgumentException("Illegal direction");
        }
        double rad = Math.toRadians(angle);

        double newLng = start.getLng() + STEP_CONST * Math.cos(rad);
        double newLat = start.getLat() + STEP_CONST * Math.sin(rad);

        Position res = new Position();
        res.setLng(newLng);
        res.setLat(newLat);
        return res;

    }

    public boolean isInRegion(Position pt, List<Position> vertices){

        if (vertices.isEmpty()){
            throw new IllegalArgumentException("Null/Empty vertices");
        }
        // 3 vertices for triangle + 1 for closing the region (must loop back)
        else if (vertices.size() < 4){
            throw new IllegalArgumentException("Too few vertices");
        }


        boolean in = false;
        int n = vertices.size();

        Position first = vertices.getFirst();
        Position last = vertices.getLast();

        //ensure closed region
        if (!first.getLat().equals(last.getLat()) || !first.getLng().equals(last.getLng())){
            throw new IllegalArgumentException("open polygon");
        }

        for (int i = 0; i < n; i++){
            Position curr =  vertices.get(i);
            Position nxt = vertices.get((i+1)%n); //Wrap to idx 0 on last

            double x1 = curr.getLng();
            double y1 = curr.getLat();

            double x2 = nxt.getLng();
            double y2 = nxt.getLat();

            boolean intersects = (y1 > pt.getLat() != y2 > pt.getLat()) &&
                    (pt.getLng() < (x2 - x1) * (pt.getLat() - y1) / (y2 - y1) + x1);

            if (intersects){
                in = !in;
            }
        }
        return in;
    }
}
