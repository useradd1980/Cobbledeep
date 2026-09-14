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

/**
 * In-game rendering bridge for completed Cobbledeep characters.
 *
 * The outer vanilla player render is replaced with a normal PlayerRenderer that
 * uses Cobbledeep's generated skin texture and the character's selected body
 * model. A guarded nested render preserves vanilla animations, armor, held items
 * and layers without recursively replacing itself.
 *
 * Race proportions remain visual only; player hitboxes are unchanged.
 */
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
        // The custom renderer itself fires RenderPlayerEvent. Let that nested
        // render continue normally instead of replacing it a second time.
        if (cobbledeepRenderPass) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
        if (wideRenderer == null || slimRenderer == null) return;

        player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!data.isCharacterCreated() || data.getRace() == null) return;

            CobbledeepPlayerRenderer renderer = getRenderer(data);
            RaceScale scale = getRaceScale(data.getRace());

            // Suppress the original vanilla renderer. We immediately replace it
            // with our PlayerRenderer subclass using the same pose/buffer/light.
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

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event)
    {
        // Only the nested Cobbledeep renderer reaches this point. The original
        // outer render was canceled in Pre, so this attaches the extra geometry
        // exactly once and inside the same race-scaling pose.
        if (!cobbledeepRenderPass) return;
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;

        player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!data.isCharacterCreated() || data.getRace() == null) return;
            CobbledeepPlayerGeometry.render(
                    data,
                    event.getRenderer().getModel(),
                    event.getPoseStack(),
                    event.getMultiBufferSource(),
                    event.getPackedLight());
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
