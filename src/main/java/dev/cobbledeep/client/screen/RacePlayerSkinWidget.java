package dev.cobbledeep.client.screen;

import java.util.function.Supplier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.cobbledeep.character.CharacterRace;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;

/**
 * Player-skin preview that applies Cobbledeep race proportions while retaining
 * the vanilla skin widget's click-and-drag rotation behaviour.
 *
 * Elf and Half-Elf ears are rendered with the exact same pose transform used by
 * the vanilla PlayerSkinWidget so the custom geometry remains attached to the
 * head instead of being positioned independently in screen space.
 */
public class RacePlayerSkinWidget extends PlayerSkinWidget
{
    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float DEFAULT_ROTATION_X = -5.0F;
    private static final float DEFAULT_ROTATION_Y = 30.0F;

    // These mirror the vanilla 1.21.1 PlayerSkinWidget transform constants.
    private static final float MODEL_OFFSET = 0.0625F;
    private static final float MODEL_HEIGHT = 2.125F;
    private static final float Z_OFFSET = 100.0F;
    private static final float ROTATION_PIVOT_Y = -1.0625F;
    private static final float MODEL_TRANSLATE_Y = -1.5F;

    private final Supplier<CharacterRace> raceSupplier;
    private final Supplier<PlayerSkin> skinSupplier;
    private final ModelPart elfEars;
    private final ModelPart halfElfEars;

    private float previewRotationX = DEFAULT_ROTATION_X;
    private float previewRotationY = DEFAULT_ROTATION_Y;

    public RacePlayerSkinWidget(
            int width,
            int height,
            EntityModelSet modelSet,
            Supplier<PlayerSkin> skinSupplier,
            Supplier<CharacterRace> raceSupplier)
    {
        super(width, height, modelSet, skinSupplier);
        this.skinSupplier = skinSupplier;
        this.raceSupplier = raceSupplier;
        this.elfEars = createEars(false);
        this.halfElfEars = createEars(true);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        CharacterRace race = raceSupplier.get();
        RaceScale scale = getRaceScale(race);
        float centerX = getX() + getWidth() / 2.0F;
        float centerY = getY() + getHeight() / 2.0F;

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY, 0.0F);
        graphics.pose().scale(scale.widthScale(), scale.heightScale(), 1.0F);
        graphics.pose().translate(-centerX, -centerY, 0.0F);

        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        renderRaceGeometry(graphics, race);

        graphics.pose().popPose();
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY)
    {
        super.onDrag(mouseX, mouseY, dragX, dragY);

        // Match PlayerSkinWidget exactly.
        previewRotationX = Mth.clamp(
                previewRotationX - (float) dragY * ROTATION_SENSITIVITY,
                -ROTATION_X_LIMIT,
                ROTATION_X_LIMIT);
        previewRotationY += (float) dragX * ROTATION_SENSITIVITY;
    }

    private void renderRaceGeometry(GuiGraphics graphics, CharacterRace race)
    {
        if (race != CharacterRace.ELF && race != CharacterRace.HALF_ELF)
        {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();

        /*
         * This reproduces the 1.21.1 PlayerSkinWidget render transform rather
         * than guessing at a head position in screen coordinates.  The ears are
         * therefore rendered in the same model coordinate system as the vanilla
         * player head.
         */
        pose.translate(
                getX() + getWidth() / 2.0F,
                getY() + getHeight(),
                Z_OFFSET);

        float modelScale = getHeight() / MODEL_HEIGHT;
        pose.scale(modelScale, modelScale, modelScale);
        pose.translate(0.0F, -MODEL_OFFSET, 0.0F);

        // Vanilla rotates X around this pivot, then applies Y rotation.
        pose.translate(0.0F, ROTATION_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(previewRotationX));
        pose.translate(0.0F, -ROTATION_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(previewRotationY));

        // Match PlayerSkinWidget.Model.draw().
        pose.scale(1.0F, 1.0F, -1.0F);
        pose.translate(0.0F, MODEL_TRANSLATE_Y, 0.0F);

        PlayerSkin skin = skinSupplier.get();
        ModelPart ears = race == CharacterRace.ELF ? elfEars : halfElfEars;
        ears.render(
                pose,
                graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(skin.texture())),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF);

        graphics.flush();
        pose.popPose();
    }

    private static ModelPart createEars(boolean halfElf)
    {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder builder = CubeListBuilder.create();

        if (halfElf)
        {
            /*
             * Vanilla head bounds are x=-4..4 and y=-8..0.  Keep every step's
             * vertical centre at y=-4 so the ear extends straight outward
             * instead of climbing diagonally across the head.
             */
            builder
                    .texOffs(0, 0).addBox(-6.25F, -5.20F, -0.75F, 2.25F, 2.40F, 1.50F)
                    .texOffs(0, 0).addBox(-7.75F, -4.85F, -0.55F, 1.50F, 1.70F, 1.10F)
                    .texOffs(0, 0).addBox(-8.50F, -4.45F, -0.35F, 0.75F, 0.90F, 0.70F)
                    .texOffs(0, 0).addBox(4.00F, -5.20F, -0.75F, 2.25F, 2.40F, 1.50F)
                    .texOffs(0, 0).addBox(6.25F, -4.85F, -0.55F, 1.50F, 1.70F, 1.10F)
                    .texOffs(0, 0).addBox(7.75F, -4.45F, -0.35F, 0.75F, 0.90F, 0.70F);
        }
        else
        {
            builder
                    .texOffs(0, 0).addBox(-6.50F, -5.50F, -0.85F, 2.50F, 3.00F, 1.70F)
                    .texOffs(0, 0).addBox(-8.75F, -5.15F, -0.65F, 2.25F, 2.30F, 1.30F)
                    .texOffs(0, 0).addBox(-10.50F, -4.80F, -0.45F, 1.75F, 1.60F, 0.90F)
                    .texOffs(0, 0).addBox(-11.50F, -4.40F, -0.30F, 1.00F, 0.80F, 0.60F)
                    .texOffs(0, 0).addBox(4.00F, -5.50F, -0.85F, 2.50F, 3.00F, 1.70F)
                    .texOffs(0, 0).addBox(6.50F, -5.15F, -0.65F, 2.25F, 2.30F, 1.30F)
                    .texOffs(0, 0).addBox(8.75F, -4.80F, -0.45F, 1.75F, 1.60F, 0.90F)
                    .texOffs(0, 0).addBox(10.50F, -4.40F, -0.30F, 1.00F, 0.80F, 0.60F);
        }

        mesh.getRoot().addOrReplaceChild("ears", builder, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("ears");
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
