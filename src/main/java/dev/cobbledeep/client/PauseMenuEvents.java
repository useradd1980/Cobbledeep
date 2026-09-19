package dev.cobbledeep.client;

import java.util.ArrayList;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.screen.SnapshotLoadScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Adds Cobbledeep's snapshot manager to the in-game Escape menu. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class PauseMenuEvents
{
    private PauseMenuEvents() { }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event)
    {
        if (!(event.getScreen() instanceof PauseScreen pauseScreen)) return;

        String returnLabel = Component.translatable("menu.returnToGame").getString();
        for (GuiEventListener listener : new ArrayList<>(event.getListenersList()))
        {
            if (!(listener instanceof Button returnButton)
                    || !returnButton.getMessage().getString().equals(returnLabel))
                continue;

            Button saveLoad = Button.builder(Component.literal("Load / Save Game"), button ->
                    Minecraft.getInstance().setScreen(new SnapshotLoadScreen(pauseScreen, true)))
                    .bounds(returnButton.getX(), Math.max(8, returnButton.getY() - 24),
                            returnButton.getWidth(), returnButton.getHeight())
                    .build();
            event.addListener(saveLoad);
            return;
        }
    }
}
