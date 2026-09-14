package dev.cobbledeep.network;

import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Synchronizes the complete persisted Cobbledeep character to the client. */
public class SyncCharacterDataPacket
{
    private final CharacterData syncedData = new CharacterData();

    public SyncCharacterDataPacket(CharacterData data)
    {
        syncedData.copyFrom(data);
    }

    public SyncCharacterDataPacket(FriendlyByteBuf buffer)
    {
        syncedData.readNetwork(buffer);
    }

    public void encode(FriendlyByteBuf buffer)
    {
        syncedData.writeNetwork(buffer);
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        minecraft.player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
                data.copyFrom(syncedData));
    }
}
