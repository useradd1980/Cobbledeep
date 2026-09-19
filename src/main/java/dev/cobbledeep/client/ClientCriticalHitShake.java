package dev.cobbledeep.client;

/** Short, damped camera impulse played when the server confirms a critical hit. */
public final class ClientCriticalHitShake
{
    private static final long DURATION_NANOS = 360_000_000L;
    private static long startedAt;

    private ClientCriticalHitShake() { }

    public static void trigger()
    {
        startedAt = System.nanoTime();
    }

    static Offset sample()
    {
        if (startedAt == 0L) return Offset.NONE;
        double progress = (System.nanoTime() - startedAt) / (double)DURATION_NANOS;
        if (progress >= 1.0)
        {
            startedAt = 0L;
            return Offset.NONE;
        }

        double strength = (1.0 - progress) * (1.0 - progress);
        double phase = progress * Math.PI * 2.0;
        return new Offset(
                (float)(Math.sin(phase * 5.0) * 1.8 * strength),
                (float)(Math.sin(phase * 7.0 + 0.8) * 1.25 * strength),
                (float)(Math.sin(phase * 6.0 + 1.7) * 0.75 * strength));
    }

    record Offset(float yaw, float pitch, float roll)
    {
        private static final Offset NONE = new Offset(0.0F, 0.0F, 0.0F);
    }
}
