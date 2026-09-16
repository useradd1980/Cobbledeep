import dev.cobbledeep.exploration.CellSight;

public final class CellSightTest
{
    private static void check(boolean ok, String message)
    {
        if (!ok) throw new AssertionError(message);
    }

    // Analytic ray/vertical-plane intersection, independent of the sampler.
    // Eye (-4,2,1); an opaque ledge/wall at x=-0.5 extends up to height.
    private static boolean clears(double x, double y, double height)
    {
        double t = 3.5 / (x + 4.0);
        return 2.0 + t * (y - 2.0) > height;
    }

    public static void main(String[] args)
    {
        check(!clears(1, 1, 1.4), "Centre ray is blocked by the terrace");
        check(CellSight.visible(-4, 2, 1, 0, 0, 0, 24,
                (x, y, z) -> clears(x, y, 1.4)), "Exposed upper cell remains visible");
        check(!CellSight.visible(-4, 2, 1, 0, 0, 0, 24,
                (x, y, z) -> clears(x, y, 4)), "Tall wall blocks every sample");
        check(!CellSight.visible(-4, 2, 1, 0, -4, 0, 24,
                (x, y, z) -> clears(x, y, 1.4)), "Surface visibility does not reveal lower cells");
        check(CellSight.visible(0, 0, 0, 22, 0, 0, 22.5,
                (x, y, z) -> true), "Cell overlapping range is checked even with centre outside");
        int[] calls = {0};
        check(!CellSight.visible(0, 0, 0, 100, 0, 0, 24,
                (x, y, z) -> { calls[0]++; return true; }), "Out-of-range cell stays hidden");
        check(calls[0] == 0, "Distant cells require no rays");
        CellSight.visible(-4, 2, 1, 0, 0, 0, 24,
                (x, y, z) -> { calls[0]++; return false; });
        check(calls[0] == 9, "Occluded cell has a bounded nine-ray budget");
        calls[0] = 0;
        CellSight.visible(-4, 2, 1, 0, 0, 0, 24,
                (x, y, z) -> { calls[0]++; return true; });
        check(calls[0] == 1, "Clear centre stops further work");
        check(CellSight.visible(-104, -98, -99, -100, -100, -100, 24,
                (x, y, z) -> clears(x + 100, y + 100, 1.4)), "Translated negative-coordinate slope");
        System.out.println("Cell sight checks passed: terrace, wall, vertical separation, range and ray budget.");
    }
}
