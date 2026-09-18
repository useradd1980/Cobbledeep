package dev.cobbledeep.client;

import java.util.ArrayList;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.screen.CharacterCreationScreen;
import dev.cobbledeep.client.screen.SnapshotLoadScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Cobbledeep.MODID,
        value = Dist.CLIENT
)
public class TitleScreenEvents
{
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event)
    {
        if (!(event.getScreen() instanceof TitleScreen titleScreen))
            return;

        /*
         * Cobbledeep owns the normal game-entry flow. Remove vanilla's
         * Singleplayer and Multiplayer buttons, but leave Mods, Realms,
         * Options and Quit available from the title screen.
         */
        String singleplayerLabel = Component.translatable("menu.singleplayer").getString();
        String multiplayerLabel = Component.translatable("menu.multiplayer").getString();

        for (GuiEventListener listener : new ArrayList<>(event.getListenersList()))
        {
            if (listener instanceof Button button)
            {
                String label = button.getMessage().getString();
                if (label.equals(singleplayerLabel) || label.equals(multiplayerLabel))
                {
                    event.removeListener(button);
                }
            }
        }

        int centerX = titleScreen.width / 2;
        int centerY = titleScreen.height / 2;
        int buttonWidth = Math.min(180, Math.max(150, titleScreen.width - 40));
        int buttonHeight = 20;
        int gap = 4;
        int firstY = centerY - 25;

        Button newGameButton = Button.builder(
                Component.literal("Cobbledeep: New Game"),
                button -> Minecraft.getInstance().setScreen(new CharacterCreationScreen()))
                .bounds(
                        centerX - buttonWidth / 2,
                        firstY,
                        buttonWidth,
                        buttonHeight)
                .build();

        Button loadGameButton = Button.builder(
                Component.literal("Cobbledeep: Load Game"),
                button -> Minecraft.getInstance().setScreen(new SnapshotLoadScreen(titleScreen)))
                .bounds(
                        centerX - buttonWidth / 2,
                        firstY + buttonHeight + gap,
                        buttonWidth,
                        buttonHeight)
                .build();

        event.addListener(newGameButton);
        event.addListener(loadGameButton);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (minecraft.level == null && minecraft.screen instanceof TitleScreen
                && (server == null || server.isStopped())
                && SnapshotLoadScreen.hasPendingRestore())
            SnapshotLoadScreen.restorePendingFromTitleScreen();
    }
}
