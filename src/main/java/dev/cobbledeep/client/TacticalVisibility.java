package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.exploration.CharacterSight;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Current visibility is recomputed from the eyes, never from saved exploration. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalVisibility
{
    private TacticalVisibility() { }

    private static boolean hidden(LivingEntity target, float partialTick)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!TacticalCameraController.isEnabled() || minecraft.player == null
                || minecraft.level == null || minecraft.player.isSpectator()
                || target == minecraft.player || target.level() != minecraft.level) return false;
        return !CharacterSight.seesEntity(minecraft.player, target, partialTick);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeLiving(RenderLivingEvent.Pre<?, ?> event)
    {
        if (hidden(event.getEntity(), event.getPartialTick())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforePlayer(RenderPlayerEvent.Pre event)
    {
        // Runs before Cobbledeep's replacement player renderer.
        if (hidden(event.getEntity(), event.getPartialTick())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeName(RenderNameTagEvent event)
    {
        if (event.getEntity() instanceof LivingEntity living && hidden(living, event.getPartialTick()))
            event.setResult(Event.Result.DENY);
    }
}
