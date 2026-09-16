import dev.cobbledeep.pathfinding.GridPathfinder;
import dev.cobbledeep.pathfinding.GridPathfinder.Node;
import dev.cobbledeep.pathfinding.GridPathfinder.Search;
import dev.cobbledeep.pathfinding.GridPathfinder.Status;
import java.util.*;

public final class GridPathfinderTest
{
    private static int checks;
    private static void check(boolean ok, String text) { checks++; if (!ok) throw new AssertionError(text); }
    private static Node n(int x, int z) { return new Node(x, 0, z); }
    private static List<Node> adjacent(Set<Node> walkable, Node from)
    {
        List<Node> result = new ArrayList<>();
        for (Node to : walkable)
            if (Math.abs(to.x() - from.x()) + Math.abs(to.z() - from.z()) == 1
                    && Math.abs(to.y16() - from.y16()) <= 16) result.add(to);
        return result;
    }
    private static Status finish(Search search)
    {
        for (int tick = 0; tick < 10000 && search.status() == Status.SEARCHING; tick++) search.step(7);
        check(search.status() != Status.SEARCHING, "Search terminates within budget");
        return search.status();
    }
    private static void valid(List<Node> path, Set<Node> ground)
    {
        for (int i = 0; i < path.size(); i++)
        {
            check(ground.contains(path.get(i)), "Every waypoint belongs to traversable graph");
            if (i > 0) check(adjacent(ground, path.get(i - 1)).contains(path.get(i)), "No wall/gap/vertical teleport");
        }
    }
    public static void main(String[] args)
    {
        Set<Node> ground = new HashSet<>();
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            if (x != 0 || z == 3) ground.add(n(x, z));
        int[] calls = {0};
        Search doorway = new Search(p -> { calls[0]++; return adjacent(ground, p); }, n(-3,0), n(3,0), 4096, 64);
        doorway.step(2);
        check(calls[0] <= 2, "Incremental tick limits neighbour expansions");
        check(finish(doorway) == Status.FOUND, "Route through open doorway");
        valid(doorway.path(), ground);
        check(doorway.path().contains(n(0,3)), "Detour reaches actual doorway instead of wall");
        ground.remove(n(0,3));
        Search closed = new Search(p -> adjacent(ground,p), n(-3,0), n(3,0), 4096, 64);
        check(finish(closed) == Status.NO_PATH, "Closed door blocks room");
        ground.add(n(0,-3));
        Search changed = new Search(p -> adjacent(ground,p), n(-3,0), n(3,0), 4096, 64);
        check(finish(changed) == Status.FOUND && changed.path().contains(n(0,-3)), "Fresh search uses changed opening");

        Set<Node> stairs = Set.of(new Node(0,0,0), new Node(1,8,0), new Node(2,16,0), new Node(3,32,0));
        Search climb = new Search(p -> adjacent(stairs,p), new Node(0,0,0), new Node(3,32,0), 64, 16);
        check(finish(climb) == Status.FOUND, "Half and full-block elevations preserved");
        valid(climb.path(), stairs);
        Set<Node> cliff = Set.of(new Node(0,0,0), new Node(1,-48,0));
        check(finish(new Search(p -> adjacent(cliff,p), new Node(0,0,0), new Node(1,-48,0), 64,16)) == Status.NO_PATH,
                "No route invents an unsupported large drop");
        Set<Node> floors = Set.of(new Node(0,0,0), new Node(0,48,0));
        check(finish(new Search(p -> adjacent(floors,p), new Node(0,0,0), new Node(0,48,0),64,16)) == Status.NO_PATH,
                "Same X/Z on another floor is a distinct destination");
        check(finish(new Search(p -> List.of(), n(0,0), n(0,0),1,16)) == Status.FOUND, "Already at destination");
        check(new Search(p -> List.of(), n(0,0), n(65,0),4096,64).status() == Status.LIMIT, "Distant goal rejected");
        check(finish(new Search(p -> List.of(n(p.x()+1,0)), n(0,0),n(20,0),2,64)) == Status.LIMIT, "Expansion cap enforced");

        // Compare A* against an independent BFS on random equal-cost maps.
        Random random = new Random(8402);
        for (int trial = 0; trial < 60; trial++)
        {
            Set<Node> map = new HashSet<>();
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++)
                if (random.nextDouble() > .3) map.add(n(x,z));
            Node start = n(-5,-5), goal = n(5,5); map.add(start); map.add(goal);
            Map<Node,Integer> distance = new HashMap<>(); ArrayDeque<Node> queue = new ArrayDeque<>();
            distance.put(start,0); queue.add(start);
            while (!queue.isEmpty())
            {
                Node p = queue.remove();
                for (Node next : adjacent(map,p)) if (!distance.containsKey(next))
                { distance.put(next,distance.get(p)+1); queue.add(next); }
            }
            Search search = new Search(p -> adjacent(map,p), start,goal,4096,64);
            Status result = finish(search);
            check((result == Status.FOUND) == distance.containsKey(goal), "A* reachability agrees with BFS");
            if (result == Status.FOUND)
            {
                check(search.path().size()-1 == distance.get(goal), "Shortest route agrees with BFS");
                valid(search.path(),map);
            }
        }
        System.out.println("Passed " + checks + " pathfinding checks.");
    }
}
