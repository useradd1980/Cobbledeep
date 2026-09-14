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
 * connection exists, then submits it once the client player logs in.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class ClientCharacterTransfer
{
    private static SubmitCharacterPacket pendingSubmission;

    private ClientCharacterTransfer() { }

    /**
     * Stage the character immediately before the Character Generation screen's
     * Finish button opens world creation. Doing this from the click itself avoids
     * relying on the exact screen-transition ordering used by Minecraft.
     */
    @SubscribeEvent
    public static void onCharacterCreationClick(ScreenEvent.MouseButtonPressed.Pre event)
    {
        if (!(event.getScreen() instanceof CharacterCreationScreen screen)) return;
        if (event.getButton() != 0) return;

        PendingCharacter pending = PendingCharacter.getLatest();
        if (pending == null || !pending.hasValidName()) return;

        int width = screen.width;
        int height = screen.height;
        boolean compact = height < 360 || width < 560;
        int centerX = width / 2;
        int bottomY = height - (compact ? 28 : 40);
        int gap = 10;
        int buttonWidth = Math.min(100, Math.max(70, (width - 30 - gap) / 2));
        int buttonHeight = compact ? 18 : 20;
        int finishX = centerX + gap / 2;

        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        boolean overFinish = mouseX >= finishX
                && mouseX < finishX + buttonWidth
                && mouseY >= bottomY
                && mouseY < bottomY + buttonHeight;

        if (!overFinish) return;

        pendingSubmission = new SubmitCharacterPacket(pending);
        Cobbledeep.LOGGER.info(
                "Staged Cobbledeep character '{}' from Finish button",
                pending.getName());
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event)
    {
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
