package dev.cobbledeep.exploration;

import java.util.Arrays;

/** CPU layout mirrored by tactical_fog.fsh: x + SIZE * (z + SIZE * y). */
public final class FogVolume
{
    public static final int SIZE = 128;
    public static final int WIDTH = 2048;
    public static final int HEIGHT = SIZE * SIZE * SIZE / WIDTH;
    public static final int LENGTH = WIDTH * HEIGHT;
    @FunctionalInterface
    public interface Sight { boolean visible(int blockX, int blockY, int blockZ); }

    private FogVolume() { }

    public static int index(int x, int y, int z)
    {
        return x + SIZE * (z + SIZE * y);
    }

    /** Origin is in two-block cells, not blocks. 0 = unknown, 1 = memory, 2 = visible. */
    public static void fill(byte[] pixels, ExplorationGrid grid, int ox, int oy, int oz, Sight sight)
    {
        if (pixels.length != LENGTH) throw new IllegalArgumentException("Wrong fog texture size");
        Arrays.fill(pixels, (byte) 0);
        if (grid == null) return;
        for (var section : grid.sections())
        {
            int sx = section.x() * 8 - ox, sy = section.y() * 8 - oy, sz = section.z() * 8 - oz;
            if (sx >= SIZE || sy >= SIZE || sz >= SIZE || sx + 8 <= 0 || sy + 8 <= 0 || sz + 8 <= 0) continue;
            long[] words = grid.sectionBits(section);
            for (int wordIndex = 0; wordIndex < words.length; wordIndex++)
            {
                long word = words[wordIndex];
                while (word != 0)
                {
                    int bit = wordIndex * 64 + Long.numberOfTrailingZeros(word);
                    word &= word - 1;
                    int x = sx + (bit & 7), y = sy + (bit >>> 6), z = sz + ((bit >>> 3) & 7);
                    if (x < 0 || y < 0 || z < 0 || x >= SIZE || y >= SIZE || z >= SIZE) continue;
                    pixels[index(x, y, z)] = (byte) (sight.visible((ox + x) * 2,
                            (oy + y) * 2, (oz + z) * 2) ? 2 : 1);
                }
            }
        }
    }
}
