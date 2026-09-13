package dev.cobbledeep.network;

import dev.cobbledeep.client.screen.CharacterCreationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class OpenCharacterCreationPacket
{
    public OpenCharacterCreationPacket()
    {
    }

    public OpenCharacterCreationPacket(FriendlyByteBuf buffer)
    {
        // No data to decode.
    }

    public void encode(FriendlyByteBuf buffer)
    {
        // No data to encode.
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        Minecraft.getInstance().setScreen(
                new CharacterCreationScreen()
        );
    }
}
