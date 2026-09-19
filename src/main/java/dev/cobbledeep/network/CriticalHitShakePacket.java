package dev.cobbledeep.network;

import dev.cobbledeep.client.ClientCriticalHitShake;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Tells the attacking player's client to play the critical-hit camera impulse. */
public final class CriticalHitShakePacket
{
    public CriticalHitShakePacket() { }

    public CriticalHitShakePacket(FriendlyByteBuf buffer) { }

    public void encode(FriendlyByteBuf buffer) { }

    public void handle(CustomPayloadEvent.Context context)
    {
        ClientCriticalHitShake.trigger();
    }
}
