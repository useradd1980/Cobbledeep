package dev.cobbledeep.network;

import dev.cobbledeep.client.ClientExploration;
import dev.cobbledeep.exploration.ExplorationGrid;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import java.util.ArrayList;
import java.util.List;

public final class SyncExplorationPacket
{
    public static final int MAX_SECTIONS = 128;
    public record SectionData(ExplorationGrid.Section key, long[] words) { }
    private final ResourceLocation dimension;
    private final boolean reset;
    private final List<SectionData> sections;

    public SyncExplorationPacket(ResourceLocation dimension, boolean reset, List<SectionData> sections)
    {
        this.dimension = dimension;
        this.reset = reset;
        this.sections = List.copyOf(sections);
    }

    public SyncExplorationPacket(FriendlyByteBuf buffer)
    {
        dimension = buffer.readResourceLocation();
        reset = buffer.readBoolean();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SECTIONS) throw new IllegalArgumentException("Invalid exploration batch");
        sections = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            var key = new ExplorationGrid.Section(buffer.readInt(), buffer.readInt(), buffer.readInt());
            int length = buffer.readUnsignedByte();
            if (length > 8) throw new IllegalArgumentException("Invalid exploration section");
            long[] words = new long[length];
            for (int j = 0; j < length; j++) words[j] = buffer.readLong();
            sections.add(new SectionData(key, words));
        }
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(dimension);
        buffer.writeBoolean(reset);
        buffer.writeVarInt(sections.size());
        for (SectionData section : sections)
        {
            buffer.writeInt(section.key().x()); buffer.writeInt(section.key().y()); buffer.writeInt(section.key().z());
            buffer.writeByte(section.words().length);
            for (long word : section.words()) buffer.writeLong(word);
        }
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        ClientExploration.accept(dimension, reset, sections);
    }
}
