package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterRace;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * First in-game rendering bridge for Cobbledeep characters.
 *
 * The vanilla player renderer already uses a pose stack rooted at the player's
 * feet, so scaling here keeps the character planted on the ground while giving
 * each race the same broad silhouette used by the character-creation preview.
 * Hitboxes remain vanilla-sized for now; this is visual only.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CobbledeepPlayerRenderEvents
{
    private CobbledeepPlayerRenderEvents() { }

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event)
    {
        event.getEntity().getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!data.isCharacterCreated() || data.getRace() == null) return;

            RaceScale scale = getRaceScale(data.getRace());
            if (scale == RaceScale.HUMAN) return;

            event.getPoseStack().pushPose();
            event.getPoseStack().scale(scale.widthScale(), scale.heightScale(), scale.widthScale());
        });
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event)
    {
        event.getEntity().getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!data.isCharacterCreated() || data.getRace() == null) return;

            RaceScale scale = getRaceScale(data.getRace());
            if (scale == RaceScale.HUMAN) return;

            event.getPoseStack().popPose();
        });
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
