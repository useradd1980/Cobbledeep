package dev.cobbledeep.pathfinding;

import java.util.*;

/** Incremental A*: the caller owns collision rules and the per-tick work budget. */
public final class GridPathfinder
{
    public record Node(int x, int y16, int z) {
        public double y() { return y16 / 16.0; }
    }
    @FunctionalInterface public interface World { List<Node> neighbours(Node node); }
    public enum Status { SEARCHING, FOUND, NO_PATH, LIMIT }
    private record Entry(Node node, double cost, double score, long order) { }

    public static final class Search
    {
        private final World world;
        private final Node start, goal;
        private final int maxExpanded, radius;
        private final PriorityQueue<Entry> open = new PriorityQueue<>(Comparator
                .comparingDouble(Entry::score).thenComparingLong(Entry::order));
        private final Map<Node, Double> costs = new HashMap<>();
        private final Map<Node, Node> parents = new HashMap<>();
        private Status status = Status.SEARCHING;
        private List<Node> path = List.of();
        private int expanded;
        private long order;

        public Search(World world, Node start, Node goal, int maxExpanded, int radius)
        {
            this.world = world; this.start = start; this.goal = goal;
            this.maxExpanded = maxExpanded; this.radius = radius;
            costs.put(start, 0.0);
            open.add(new Entry(start, 0, distance(start, goal), order++));
            if (!inBounds(goal)) status = Status.LIMIT;
        }

        public Status step(int budget)
        {
            // Count stale queue entries too: no unbounded cleanup in one tick.
            for (int i = 0; i < budget && status == Status.SEARCHING; i++)
            {
                if (open.isEmpty()) { status = Status.NO_PATH; break; }
                Entry entry = open.remove();
                if (entry.cost() > costs.getOrDefault(entry.node(), Double.POSITIVE_INFINITY)) continue;
                if (entry.node().equals(goal))
                {
                    ArrayList<Node> result = new ArrayList<>();
                    for (Node n = goal; n != null; n = parents.get(n)) result.add(n);
                    Collections.reverse(result);
                    path = List.copyOf(result);
                    status = Status.FOUND;
                    break;
                }
                if (expanded++ >= maxExpanded) { status = Status.LIMIT; break; }
                for (Node next : world.neighbours(entry.node()))
                {
                    if (!inBounds(next)) continue;
                    double cost = entry.cost() + distance(entry.node(), next);
                    if (cost >= costs.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                    costs.put(next, cost);
                    parents.put(next, entry.node());
                    open.add(new Entry(next, cost, cost + distance(next, goal), order++));
                }
            }
            return status;
        }

        private boolean inBounds(Node n)
        {
            return Math.abs((long)n.x() - start.x()) <= radius
                    && Math.abs((long)n.z() - start.z()) <= radius
                    && Math.abs((long)n.y16() - start.y16()) <= radius * 16L;
        }

        public List<Node> path() { return path; }
        public Status status() { return status; }
    }

    private static double distance(Node a, Node b)
    {
        return Math.hypot((double)a.x() - b.x(), (double)a.z() - b.z())
                + Math.abs(a.y() - b.y()) * 0.3;
    }
}
