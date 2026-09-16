import dev.cobbledeep.pathfinding.GridPathfinder;
import dev.cobbledeep.pathfinding.GridPathfinder.Node;
import dev.cobbledeep.pathfinding.WalkStepRules;
import java.util.ArrayList;
import java.util.List;

public final class WalkStepRulesTest
{
    private static int checks;
    private static void check(boolean ok, String message)
    { checks++; if (!ok) throw new AssertionError(message); }

    public static void main(String[] args)
    {
        for (int dx : new int[] {-1, 1}) for (int dz : new int[] {-1, 1})
            for (int height : new int[] {0, 8, 16})
            {
                Node from = new Node(-10, -32, -10);
                Node to = new Node(-10 + dx, -32 + height, -10 + dz);
                Node sideX = new Node(to.x(), from.y16(), from.z());
                Node sideZ = new Node(from.x(), to.y16(), to.z());
                check(WalkStepRules.supportedDiagonal(from, to, List.of(sideX,sideZ)), "Mixed supported heights allow diagonal ascent");
                check(!WalkStepRules.supportedDiagonal(from, to, List.of(sideX)), "Missing side blocks corner cutting");
                check(!WalkStepRules.supportedDiagonal(from, to,
                        List.of(sideX, new Node(sideZ.x(), from.y16()-16, sideZ.z()))), "Pit beside edge stays blocked");
                check(!WalkStepRules.supportedDiagonal(from, to,
                        List.of(sideX, new Node(sideZ.x(), to.y16()+16, sideZ.z()))), "High wall beside edge stays blocked");
            }
        Node start = new Node(0,0,0);
        check(!WalkStepRules.supportedDiagonal(start,new Node(1,32,1),
                List.of(new Node(1,0,0),new Node(0,0,1))), "Two-block diagonal climb rejected");
        check(!WalkStepRules.supportedDiagonal(start,new Node(1,-16,1),
                List.of(new Node(1,0,0),new Node(0,0,1))), "Descending-diagonal rules remain unchanged");
        check(WalkStepRules.shouldJump(1,Math.sqrt(2),true), "Jump starts at diagonal edge entry");
        check(WalkStepRules.shouldJump(1,1,true), "Cardinal jump preserved");
        check(!WalkStepRules.shouldJump(.5,1,true), "Half slab uses normal stepping");
        check(!WalkStepRules.shouldJump(1,Math.sqrt(2),false), "No repeated airborne jump input");
        check(!WalkStepRules.shouldJump(-1,1,true), "No jumping while descending");
        check(!WalkStepRules.shouldJump(1,2,true), "No premature distant jumps");

        // Height map of a terraced hill: both adjoining columns support each
        // rising diagonal. A* should preserve one straight diagonal stair run.
        GridPathfinder.World hill = from -> {
            List<Node> sides = new ArrayList<>();
            for (int[] d : new int[][] {{1,0},{-1,0},{0,1},{0,-1}})
            {
                int x=from.x()+d[0], z=from.z()+d[1];
                if (x>=0 && x<=4 && z>=0 && z<=4) sides.add(new Node(x, Math.max(x,z)*16,z));
            }
            List<Node> result = new ArrayList<>(sides);
            for (int dx : new int[] {-1,1}) for (int dz : new int[] {-1,1})
            {
                int x=from.x()+dx, z=from.z()+dz;
                if (x<0 || x>4 || z<0 || z>4) continue;
                Node to = new Node(x,Math.max(x,z)*16,z);
                if (WalkStepRules.supportedDiagonal(from,to,sides)) result.add(to);
            }
            return result;
        };
        var search = new GridPathfinder.Search(hill,start,new Node(4,64,4),4096,64);
        for (int tick=0; tick<100 && search.status()==GridPathfinder.Status.SEARCHING; tick++) search.step(8);
        check(search.status()==GridPathfinder.Status.FOUND, "Diagonal hill is reachable");
        check(search.path().size()==5, "Hill route does not zigzag into cardinal turns");
        for (Node node : search.path()) check(node.x()==node.z(), "Straight diagonal ascent maintained");
        System.out.println("Passed " + checks + " diagonal step checks.");
    }
}
