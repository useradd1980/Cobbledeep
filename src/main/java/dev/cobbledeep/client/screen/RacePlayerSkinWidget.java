package dev.cobbledeep.client.screen;

import java.util.function.Supplier;

import dev.cobbledeep.character.CharacterRace;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Player-skin preview that applies race-specific proportions while retaining
 * the vanilla skin widget's click-and-drag rotation behaviour.
 *
 * This is an intermediate step toward a fully custom Cobbledeep player model.
 * It gives each race a distinct silhouette now, while custom geometry such as
 * pointed ears can be layered in later without changing character data.
 */
public class RacePlayerSkinWidget extends PlayerSkinWidget
{
    private final Supplier<CharacterRace> raceSupplier;

    public RacePlayerSkinWidget(
            int width,
            int height,
            EntityModelSet modelSet,
            Supplier<PlayerSkin> skinSupplier,
            Supplier<CharacterRace> raceSupplier)
    {
        super(width, height, modelSet, skinSupplier);
        this.raceSupplier = raceSupplier;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        RaceScale scale = getRaceScale(raceSupplier.get());
        float centerX = getX() + getWidth() / 2.0F;
        float centerY = getY() + getHeight() / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale.widthScale(), scale.heightScale(), 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }

    private static RaceScale getRaceScale(CharacterRace race)
    {
        if (race == null)
        {
            return RaceScale.HUMAN;
        }

        return switch (race)
        {
            case HUMAN -> RaceScale.HUMAN;
            case ELF -> new RaceScale(0.90F, 1.06F);
            case HALF_ELF -> new RaceScale(0.96F, 1.02F);
            case DWARF -> new RaceScale(1.15F, 0.82F);
            case HALFLING -> new RaceScale(0.88F, 0.72F);
            case GNOME -> new RaceScale(0.95F, 0.76F);
        };
    }

    private record RaceScale(float widthScale, float heightScale)
    {
        private static final RaceScale HUMAN = new RaceScale(1.0F, 1.0F);
    }
}
