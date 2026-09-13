package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.PendingCharacter;
import dev.cobbledeep.network.RPGNetwork;
import dev.cobbledeep.network.SubmitCharacterPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Holds the character completed on the title screen until a play connection
 * exists, then submits it to the authoritative server-side capability.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class ClientCharacterTransfer
{
    private static SubmitCharacterPacket pendingSubmission;

    private ClientCharacterTransfer() { }

    public static void stage(PendingCharacter pendingCharacter)
    {
        pendingSubmission = new SubmitCharacterPacket(pendingCharacter);
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        if (pendingSubmission == null) return;
        RPGNetwork.CHANNEL.sendToServer(pendingSubmission);
        pendingSubmission = null;
    }
}
