package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.PendingCharacter;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Cobbledeep appearance rendering with a local-only Blender humanoid preview. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CobbledeepPlayerRenderEvents
{
    private static CobbledeepPlayerRenderer wideRenderer;
    private static CobbledeepPlayerRenderer slimRenderer;
    private static boolean cobbledeepRenderPass;

    private CobbledeepPlayerRenderEvents() { }

    static void initializeRenderers(EntityRendererProvider.Context context)
    {
        wideRenderer = new CobbledeepPlayerRenderer(context, false);
        slimRenderer = new CobbledeepPlayerRenderer(context, true);
    }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event)
    {
        if (cobbledeepRenderPass) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (wideRenderer == null || slimRenderer == null) return;

        player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!data.isCharacterCreated() || data.getRace() == null) return;

            // First integration milestone: only the local completed Cobbledeep
            // character receives the GLB preview. On disabled/missing/invalid
            // assets, preserve the existing race-aware vanilla render path.
            if (CobbledeepHumanoidPreview.renderIfEnabled(player, event.getPartialTick(),
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight())) {
                event.setCanceled(true);
                return;
            }

            CobbledeepPlayerRenderer renderer = getRenderer(data);
            RaceScale scale = getRaceScale(data.getRace());
            event.setCanceled(true);
            event.getPoseStack().pushPose();
            event.getPoseStack().scale(scale.widthScale(), scale.heightScale(), scale.widthScale());

            cobbledeepRenderPass = true;
            try
            {
                renderer.render(
                        player,
                        player.getYRot(),
                        event.getPartialTick(),
                        event.getPoseStack(),
                        event.getMultiBufferSource(),
                        event.getPackedLight());
            }
            finally
            {
                cobbledeepRenderPass = false;
                event.getPoseStack().popPose();
            }
        });
    }

    private static CobbledeepPlayerRenderer getRenderer(CharacterData data)
    {
        return data.getGender() == PendingCharacter.Gender.FEMALE
                ? slimRenderer
                : wideRenderer;
    }

    private static RaceScale getRaceScale(CharacterRace race)
    {
        return switch (race)
        {
            case HUMAN -> RaceScale.HUMAN;
            case ELF -> new RaceScale(0.90F, 1.06F);
            case HALF_ELF -> new RaceScale(0.96F, 1.02F);
            case DWARF -> new RaceScale(1.22F, 0.78F);
            case HALFLING -> new RaceScale(0.84F, 0.68F);
            case GNOME -> new RaceScale(0.90F, 0.72F);
        };
    }

    private record RaceScale(float widthScale, float heightScale)
    {
        private static final RaceScale HUMAN = new RaceScale(1.0F, 1.0F);
    }
}
