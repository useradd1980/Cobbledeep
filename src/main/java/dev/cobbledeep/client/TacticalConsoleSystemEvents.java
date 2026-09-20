package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.SystemMessageReceivedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Keep useful system feedback accessible after the vanilla HUD is suppressed. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalConsoleSystemEvents {
    private static Object observedWorld;
    private static Boolean previousFreezeState;

    private TacticalConsoleSystemEvents() { }

    @SubscribeEvent
    public static void onSystemMessage(SystemMessageReceivedEvent event) {
        if (!TacticalCameraController.isEnabled()) return;
        String text = event.getMessage().getString();
        // Space sends an immediate PAUSED/PLAYING action-bar notification, while
        // onClientTick below logs the corresponding actual freeze-state change.
        // Only the state-change entry belongs in the console; otherwise each
        // space-bar press produces the same message twice.
        boolean redundantPauseOverlay = event.isOverlay()
                && ("PAUSED".equals(text) || "PLAYING".equals(text));
        // Progress updates are transient and recur on nearly every movement
        // request. Preserve actual route failures and other useful messages.
        if (!redundantPauseOverlay && !text.startsWith("Finding route")
                && !text.startsWith("Updating route"))
            TacticalConsoleOverlay.addMessage(TacticalConsoleOverlay.Category.SYSTEM, event.getMessage());
        // The vanilla chat/overlay layers are suppressed while in tactical mode.
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            observedWorld = null;
            previousFreezeState = null;
            return;
        }
        var server = mc.getSingleplayerServer();
        if (!TacticalCameraController.isEnabled() || server == null) {
            observedWorld = mc.level;
            previousFreezeState = null;
            return;
        }
        boolean frozen = server.tickRateManager().isFrozen();
        if (observedWorld != mc.level || previousFreezeState == null) {
            observedWorld = mc.level;
            previousFreezeState = frozen;
            return;
        }
        if (previousFreezeState != frozen) {
            previousFreezeState = frozen;
            TacticalConsoleOverlay.addMessage(TacticalConsoleOverlay.Category.SYSTEM,
                    Component.literal(frozen ? "PAUSED" : "PLAYING"));
        }
    }
}
