package dev.cobbledeep.exploration;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;

/** One vanilla SavedData file per player UUID in each dimension's data folder. */
public final class ExplorationData extends SavedData
{
    private static final int VERSION = 1;
    private static final Factory<ExplorationData> FACTORY = new Factory<>(
            ExplorationData::new, (tag, lookup) -> load(tag), null);
    private final ExplorationGrid grid = new ExplorationGrid();
    private final Set<ExplorationGrid.Section> pendingSync = new HashSet<>();

    public static ExplorationData get(ServerPlayer player)
    {
        return player.serverLevel().getDataStorage().computeIfAbsent(FACTORY,
                "cobbledeep_exploration_" + player.getUUID());
    }

    public ExplorationGrid grid() { return grid; }

    public void discover(int x, int y, int z)
    {
        if (grid.discover(x, y, z))
        {
            setDirty();
            pendingSync.add(new ExplorationGrid.Section(Math.floorDiv(x, 16),
                    Math.floorDiv(y, 16), Math.floorDiv(z, 16)));
        }
    }

    public Set<ExplorationGrid.Section> drainSync()
    {
        Set<ExplorationGrid.Section> result = Set.copyOf(pendingSync);
        pendingSync.clear();
        return result;
    }

    public void discoverColumn(int x, int z, int minY, int maxY)
    {
        for (int sy = Math.floorDiv(minY, 16); sy <= Math.floorDiv(maxY - 1, 16); sy++)
            if (grid.discoverSectionColumn(x, sy, z))
            {
                setDirty();
                pendingSync.add(new ExplorationGrid.Section(Math.floorDiv(x, 16), sy, Math.floorDiv(z, 16)));
            }
    }

    private static ExplorationData load(CompoundTag tag)
    {
        if (tag.getInt("Version") != VERSION)
            throw new IllegalArgumentException("Unsupported Cobbledeep exploration data version");
        ExplorationData data = new ExplorationData();
        ListTag sections = tag.getList("Sections", Tag.TAG_COMPOUND);
        for (int i = 0; i < sections.size(); i++)
        {
            CompoundTag section = sections.getCompound(i);
            data.grid.restoreSection(new ExplorationGrid.Section(
                    section.getInt("X"), section.getInt("Y"), section.getInt("Z")),
                    section.getLongArray("Cells"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup)
    {
        tag.putInt("Version", VERSION);
        ListTag sections = new ListTag();
        for (ExplorationGrid.Section key : grid.sections())
        {
            CompoundTag section = new CompoundTag();
            section.putInt("X", key.x());
            section.putInt("Y", key.y());
            section.putInt("Z", key.z());
            section.putLongArray("Cells", grid.sectionBits(key));
            sections.add(section);
        }
        tag.put("Sections", sections);
        return tag;
    }
}
