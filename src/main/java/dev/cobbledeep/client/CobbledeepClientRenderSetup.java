package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Replaces the vanilla in-world HUD with Cobbledeep's tactical interface. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CobbledeepClientRenderSetup {
    private CobbledeepClientRenderSetup() { }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        CobbledeepPlayerRenderEvents.initializeRenderers(event.getContext());
    }

    @SubscribeEvent
    public static void onAddGuiLayers(AddGuiOverlayLayersEvent event) {
        ForgeLayeredDraw gui = event.getLayeredDraw();
        // Preserve Minecraft's normal HUD outside tactical mode. The built-in
        // layers are conditionally skipped only while our custom UI is active.
        // The sleep overlay is a separate root layer and remains intact.
        for (ResourceLocation layer : ForgeLayeredDraw.PRE_LIST)
            gui.addConditionTo(ForgeLayeredDraw.PRE_SLEEP_STACK, layer,
                    () -> !TacticalCameraController.isEnabled());
        for (ResourceLocation layer : ForgeLayeredDraw.POST_LIST)
            gui.addConditionTo(ForgeLayeredDraw.POST_SLEEP_STACK, layer,
                    () -> !TacticalCameraController.isEnabled());

        // Existing single post-sleep layer draws console, left menu, then party
        // portraits, ensuring their edges meet without duplicate registrations.
        gui.add(ForgeLayeredDraw.POST_SLEEP_STACK,
                ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "tactical_interface"),
                (graphics, partialTick) -> PartyPortraitBar.onHud(graphics));
    }
}
