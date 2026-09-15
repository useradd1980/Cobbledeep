package dev.cobbledeep.exploration;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/** One vanilla SavedData file per player UUID in each dimension's data folder. */
public final class ExplorationData extends SavedData
{
    private static final int VERSION = 1;
    private static final Factory<ExplorationData> FACTORY = new Factory<>(
            ExplorationData::new, (tag, lookup) -> load(tag), null);
    private final ExplorationGrid grid = new ExplorationGrid();

    public static ExplorationData get(ServerPlayer player)
    {
        return player.serverLevel().getDataStorage().computeIfAbsent(FACTORY,
                "cobbledeep_exploration_" + player.getUUID());
    }

    public ExplorationGrid grid() { return grid; }

    public void discover(int x, int y, int z)
    {
        if (grid.discover(x, y, z)) setDirty();
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
