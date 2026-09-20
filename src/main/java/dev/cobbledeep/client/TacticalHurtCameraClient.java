package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Vanilla applies a damage-tilt effect whenever the camera's player is hit.
 * That is separate from Cobbledeep's explicit critical-hit camera impulse,
 * which should continue to play only when the player lands a critical hit.
 *
 * Disable only vanilla's damage tilt during active tactical gameplay. Keep
 * the player's original accessibility preference intact outside that mode,
 * including in menus and after leaving a world.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalHurtCameraClient {
    private static Double previousDamageTilt;

    private TacticalHurtCameraClient() { }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (TacticalCameraController.isEnabled() && minecraft.player != null
                && minecraft.level != null && minecraft.screen == null) {
            if (previousDamageTilt == null) {
                previousDamageTilt = minecraft.options.damageTiltStrength().get();
            }
            if (minecraft.options.damageTiltStrength().get() != 0.0D) {
                minecraft.options.damageTiltStrength().set(0.0D);
            }
        } else {
            restore(minecraft);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        restore(Minecraft.getInstance());
    }

    private static void restore(Minecraft minecraft) {
        if (previousDamageTilt == null) return;
        minecraft.options.damageTiltStrength().set(previousDamageTilt);
        previousDamageTilt = null;
    }
}
