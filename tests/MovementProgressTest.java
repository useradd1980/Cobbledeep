import dev.cobbledeep.pathfinding.MovementProgress;

public final class MovementProgressTest
{
    private static int checks;

    public static void main(String[] args)
    {
        MovementProgress progress = new MovementProgress();
        check(progress.update(10, 64, 10) == 0, "first sample establishes the anchor");
        check(progress.update(10, 64, 10) == 1, "stationary tick is counted");
        check(progress.update(10, 64, 10) == 2, "stationary ticks accumulate");
        check(progress.update(10.02, 64, 10) == 3, "tiny jitter does not look like progress");
        check(progress.update(10.04, 64, 10) == 0, "cumulative horizontal movement resets the counter");
        check(progress.update(10.04, 64.04, 10) == 0, "vertical movement also counts");

        for (int i = 1; i <= 100; i++)
            check(progress.update(10.04 + i * 0.08, 64.04, 10) == 0,
                    "ordinary forward movement never appears stalled");

        progress.update(18.04, 64.04, 10);
        progress.reset();
        check(progress.update(200, 80, -50) == 0, "reset accepts a new route position");
        check(progress.update(200, 80, -50) == 1, "new position can subsequently become stationary");
        System.out.println("Passed " + checks + " movement-progress checks.");
    }

    private static void check(boolean condition, String message)
    {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
