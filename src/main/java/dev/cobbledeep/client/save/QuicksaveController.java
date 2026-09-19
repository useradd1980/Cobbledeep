package dev.cobbledeep.client.save;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Handles the in-world Q quicksave shortcut without also dropping an item. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class QuicksaveController
{
    private QuicksaveController() { }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event)
    {
        if (event.getAction() != GLFW.GLFW_PRESS || event.getKey() != GLFW.GLFW_KEY_Q) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null || minecraft.level == null
                || minecraft.getSingleplayerServer() == null)
            return;

        // The key event is delivered after vanilla updates its own Q binding.
        // Remove that queued click so quicksaving never drops the held item.
        while (minecraft.options.keyDrop.consumeClick()) { }
        minecraft.options.keyDrop.setDown(false);
        SnapshotThumbnailCapture.requestQuicksave(minecraft);
    }
}
