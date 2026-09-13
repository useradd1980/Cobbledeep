package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.PendingCharacter;
import dev.cobbledeep.client.screen.CharacterCreationScreen;
import dev.cobbledeep.network.RPGNetwork;
import dev.cobbledeep.network.SubmitCharacterPacket;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Carries the completed title-screen character across the gap before a play
 * connection exists. The packet is only staged when the wizard actually opens
 * world creation, then sent once the client player logs in.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class ClientCharacterTransfer
{
    private static SubmitCharacterPacket pendingSubmission;

    private ClientCharacterTransfer() { }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event)
    {
        if (event.getCurrentScreen() instanceof CharacterCreationScreen
                && event.getNewScreen() instanceof CreateWorldScreen)
        {
            PendingCharacter pending = PendingCharacter.getLatest();
            if (pending != null && pending.hasValidName())
            {
                pendingSubmission = new SubmitCharacterPacket(pending);
                Cobbledeep.LOGGER.info(
                        "Staged Cobbledeep character '{}' for world creation",
                        pending.getName());
            }
            return;
        }

        // Cancelling world creation or returning to title must not allow a
        // staged new character to overwrite an existing saved character later.
        if (event.getNewScreen() instanceof TitleScreen
                || event.getCurrentScreen() instanceof CreateWorldScreen
                        && event.getNewScreen() instanceof CharacterCreationScreen)
        {
            pendingSubmission = null;
        }
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        if (pendingSubmission == null) return;

        Cobbledeep.LOGGER.info("Submitting staged Cobbledeep character to server");
        RPGNetwork.CHANNEL.send(
                pendingSubmission,
                PacketDistributor.SERVER.noArg());
        pendingSubmission = null;
    }
}
