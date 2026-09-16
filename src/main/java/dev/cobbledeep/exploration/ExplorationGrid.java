package dev.cobbledeep.exploration;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Persistent discovery only: never use these bits as current creature visibility. */
public final class ExplorationGrid
{
    public static final int CELL_SIZE = 2;
    public static final int CELLS_PER_SECTION = 512;
    public record Section(int x, int y, int z) { }

    private final Map<Section, BitSet> sections = new HashMap<>();
    private long discoveredCount;

    public boolean discover(int x, int y, int z)
    {
        BitSet bits = sections.computeIfAbsent(section(x, y, z), ignored -> new BitSet());
        int index = index(x, y, z);
        if (bits.get(index)) return false;
        bits.set(index);
        discoveredCount++;
        return true;
    }

    public boolean isExplored(int x, int y, int z)
    {
        BitSet bits = sections.get(section(x, y, z));
        return bits != null && bits.get(index(x, y, z));
    }

    /** Discover one two-block-wide column within a 16-block-tall section. */
    public boolean discoverSectionColumn(int x, int sectionY, int z)
    {
        Section key = new Section(Math.floorDiv(x, 16), sectionY, Math.floorDiv(z, 16));
        BitSet bits = sections.computeIfAbsent(key, ignored -> new BitSet());
        int base = index(x, 0, z);
        boolean changed = false;
        for (int y = 0; y < 8; y++)
        {
            int bit = base + y * 64;
            if (!bits.get(bit))
            {
                bits.set(bit);
                discoveredCount++;
                changed = true;
            }
        }
        return changed;
    }

    public long discoveredCount() { return discoveredCount; }
    public Set<Section> sections() { return Collections.unmodifiableSet(sections.keySet()); }

    public long[] sectionBits(Section section)
    {
        BitSet bits = sections.get(section);
        return bits == null ? new long[0] : bits.toLongArray();
    }

    public void restoreSection(Section section, long[] words)
    {
        if (words.length > CELLS_PER_SECTION / Long.SIZE)
            throw new IllegalArgumentException("Exploration section exceeds 512 cells");
        BitSet restored = BitSet.valueOf(words);
        if (restored.isEmpty()) return;
        BitSet bits = sections.computeIfAbsent(section, ignored -> new BitSet());
        int before = bits.cardinality();
        bits.or(restored);
        discoveredCount += bits.cardinality() - before;
    }

    private static Section section(int x, int y, int z)
    {
        return new Section(Math.floorDiv(x, 16), Math.floorDiv(y, 16), Math.floorDiv(z, 16));
    }

    private static int index(int x, int y, int z)
    {
        return (Math.floorMod(y, 16) / CELL_SIZE) * 64
                + (Math.floorMod(z, 16) / CELL_SIZE) * 8 + Math.floorMod(x, 16) / CELL_SIZE;
    }
}
