package dev.cobbledeep.client.save;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.save.GameSnapshotManager.Snapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Captures a clean world frame before the HUD is drawn, then starts the save. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class SnapshotThumbnailCapture
{
    // Keep substantially more source detail than the on-screen save-list size.
    // GUI scaling can make a 96 x 54 logical thumbnail several times larger in
    // physical pixels, so a 160 x 90 source appears visibly blocky.
    public static final int WIDTH = 480;
    public static final int HEIGHT = 270;

    private record Request(String name, Snapshot overwrite, boolean quicksave,
            boolean previousHideGui) { }
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

        pending = new Request(name, overwrite, false, minecraft.options.hideGui);
        minecraft.options.hideGui = true;
        minecraft.setScreen(null);
        return true;
    }

    public static boolean requestQuicksave(Minecraft minecraft)
    {
        if (pending != null)
        {
            if (minecraft.player != null)
                minecraft.player.displayClientMessage(Component.literal(
                        "A snapshot capture is already in progress."), true);
            return false;
        }
        if (minecraft.level == null || minecraft.player == null
                || minecraft.getSingleplayerServer() == null)
            return false;

        pending = new Request("Quicksave-1", null, true, minecraft.options.hideGui);
        minecraft.options.hideGui = true;
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

        if (request.quicksave())
            GameSnapshotManager.saveQuicksave(minecraft, thumbnail, (success, message) -> { });
        else
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
            double sampleY = cropY + ((y + 0.5) * cropHeight / HEIGHT) - 0.5;
            int y0 = Mth.clamp((int) Math.floor(sampleY), cropY, cropY + cropHeight - 1);
            int y1 = Math.min(cropY + cropHeight - 1, y0 + 1);
            double fy = sampleY - Math.floor(sampleY);
            for (int x = 0; x < WIDTH; x++)
            {
                double sampleX = cropX + ((x + 0.5) * cropWidth / WIDTH) - 0.5;
                int x0 = Mth.clamp((int) Math.floor(sampleX), cropX, cropX + cropWidth - 1);
                int x1 = Math.min(cropX + cropWidth - 1, x0 + 1);
                double fx = sampleX - Math.floor(sampleX);
                result.setPixelRGBA(x, y, bilinear(
                        source.getPixelRGBA(x0, y0), source.getPixelRGBA(x1, y0),
                        source.getPixelRGBA(x0, y1), source.getPixelRGBA(x1, y1), fx, fy));
            }
        }
        return result;
    }

    private static int bilinear(int topLeft, int topRight, int bottomLeft, int bottomRight,
            double fx, double fy)
    {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8)
        {
            double top = ((topLeft >>> shift) & 0xFF) * (1.0 - fx)
                    + ((topRight >>> shift) & 0xFF) * fx;
            double bottom = ((bottomLeft >>> shift) & 0xFF) * (1.0 - fx)
                    + ((bottomRight >>> shift) & 0xFF) * fx;
            int channel = Mth.clamp((int) Math.round(top * (1.0 - fy) + bottom * fy), 0, 255);
            result |= channel << shift;
        }
        return result;
    }
}
