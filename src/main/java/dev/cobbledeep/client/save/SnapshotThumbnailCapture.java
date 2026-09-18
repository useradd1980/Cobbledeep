package dev.cobbledeep.client.save;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.save.GameSnapshotManager.Snapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Captures a clean world frame before the HUD is drawn, then starts the save. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class SnapshotThumbnailCapture
{
    public static final int WIDTH = 160;
    public static final int HEIGHT = 90;

    private record Request(String name, Snapshot overwrite, boolean previousHideGui) { }
    private static Request pending;

    private SnapshotThumbnailCapture() { }

    public static boolean request(Minecraft minecraft, String name, Snapshot overwrite)
    {
        if (pending != null)
        {
            if (minecraft.player != null)
                minecraft.player.displayClientMessage(Component.literal(
                        "A snapshot capture is already in progress."), true);
            return false;
        }
        if (minecraft.level == null || minecraft.player == null)
            return false;

        pending = new Request(name, overwrite, minecraft.options.hideGui);
        minecraft.options.hideGui = true;
        minecraft.setScreen(null);
        return true;
    }

    @SubscribeEvent
    public static void afterLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || pending == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        Request request = pending;
        pending = null;
        NativeImage thumbnail = null;
        try
        {
            NativeImage frame = Screenshot.takeScreenshot(minecraft.getMainRenderTarget());
            try
            {
                thumbnail = resizeAndCrop(frame);
            }
            finally
            {
                frame.close();
            }
        }
        catch (Exception exception)
        {
            Cobbledeep.LOGGER.warn("Unable to capture snapshot thumbnail", exception);
        }
        finally
        {
            minecraft.options.hideGui = request.previousHideGui();
        }

        GameSnapshotManager.saveCurrentWorld(minecraft, request.name(), request.overwrite(),
                thumbnail, (success, message) -> { });
    }

    private static NativeImage resizeAndCrop(NativeImage source)
    {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double targetAspect = (double) WIDTH / HEIGHT;
        int cropWidth = sourceWidth;
        int cropHeight = sourceHeight;
        if ((double) sourceWidth / sourceHeight > targetAspect)
            cropWidth = Math.max(1, (int) Math.round(sourceHeight * targetAspect));
        else
            cropHeight = Math.max(1, (int) Math.round(sourceWidth / targetAspect));
        int cropX = (sourceWidth - cropWidth) / 2;
        int cropY = (sourceHeight - cropHeight) / 2;

        NativeImage result = new NativeImage(WIDTH, HEIGHT, false);
        for (int y = 0; y < HEIGHT; y++)
        {
            int sourceY = cropY + Math.min(cropHeight - 1, y * cropHeight / HEIGHT);
            for (int x = 0; x < WIDTH; x++)
            {
                int sourceX = cropX + Math.min(cropWidth - 1, x * cropWidth / WIDTH);
                result.setPixelRGBA(x, y, source.getPixelRGBA(sourceX, sourceY));
            }
        }
        return result;
    }
}
