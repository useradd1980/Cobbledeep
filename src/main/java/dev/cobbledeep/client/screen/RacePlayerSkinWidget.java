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
 * Elf and Half-Elf previews also render simple three-dimensional pointed-ear
 * geometry. The ears use the same preview rotation direction as the vanilla
 * player and are positioned relative to the sides of the head.
 */
public class RacePlayerSkinWidget extends PlayerSkinWidget
{
    private static final float EAR_ROTATION_SENSITIVITY = 2.5F;
    private static final float EAR_PITCH_LIMIT = 30.0F;

    private final Supplier<CharacterRace> raceSupplier;
    private final Supplier<PlayerSkin> skinSupplier;
    private final ModelPart elfEars;
    private final ModelPart halfElfEars;
    private float previewYaw = 30.0F;
    private float previewPitch = -5.0F;

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
        previewYaw -= (float) dragX * EAR_ROTATION_SENSITIVITY;
        previewPitch = Mth.clamp(
                previewPitch + (float) dragY * EAR_ROTATION_SENSITIVITY,
                -EAR_PITCH_LIMIT,
                EAR_PITCH_LIMIT);
    }

    private void renderRaceGeometry(GuiGraphics graphics, CharacterRace race)
    {
        if (race != CharacterRace.ELF && race != CharacterRace.HALF_ELF)
        {
            return;
        }

        /*
         * The vanilla preview's model origin is below the head.  The custom ear
         * mesh is authored around the head centre, so place that origin at the
         * visible head and then apply the same yaw/pitch as the player preview.
         * Keeping X/Y/Z scale identical prevents the ears from becoming skewed
         * diagonally as the model rotates.
         */
        float modelScale = Math.max(22.0F, getHeight() / 4.35F);
        float headCenterX = getX() + getWidth() / 2.0F;
        float headCenterY = getY() + getHeight() * 0.185F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(headCenterX, headCenterY, 180.0F);
        pose.scale(modelScale, modelScale, modelScale);
        pose.mulPose(Axis.XP.rotationDegrees(previewPitch));
        pose.mulPose(Axis.YP.rotationDegrees(previewYaw));

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
             * Vanilla head width is eight model units (-4..4).  Start the ear
             * roots slightly outside that boundary so the visible portions sit
             * beside the head rather than over the face.  The stepped cuboids
             * taper upward only mildly; most of the point travels horizontally.
             */
            builder
                    .texOffs(0, 0).addBox(-6.00F, -0.30F, -0.70F, 2.00F, 2.10F, 1.4F)
                    .texOffs(0, 0).addBox(-7.35F, -0.05F, -0.50F, 1.35F, 1.55F, 1.0F)
                    .texOffs(0, 0).addBox(-8.10F, 0.25F, -0.35F, 0.75F, 0.90F, 0.7F)
                    .texOffs(0, 0).addBox(4.00F, -0.30F, -0.70F, 2.00F, 2.10F, 1.4F)
                    .texOffs(0, 0).addBox(6.00F, -0.05F, -0.50F, 1.35F, 1.55F, 1.0F)
                    .texOffs(0, 0).addBox(7.35F, 0.25F, -0.35F, 0.75F, 0.90F, 0.7F);
        }
        else
        {
            /*
             * Full Elf ears are intentionally prominent.  They begin flush at
             * x=+/-4 and extend roughly five model units beyond the head.  The
             * vertical offsets are small so the overall silhouette reads as a
             * mostly horizontal pointed ear instead of a diagonal horn.
             */
            builder
                    .texOffs(0, 0).addBox(-6.25F, -0.45F, -0.80F, 2.25F, 2.60F, 1.6F)
                    .texOffs(0, 0).addBox(-8.25F, -0.15F, -0.60F, 2.00F, 2.00F, 1.2F)
                    .texOffs(0, 0).addBox(-9.60F, 0.20F, -0.40F, 1.35F, 1.25F, 0.8F)
                    .texOffs(0, 0).addBox(-10.25F, 0.45F, -0.25F, 0.65F, 0.70F, 0.5F)
                    .texOffs(0, 0).addBox(4.00F, -0.45F, -0.80F, 2.25F, 2.60F, 1.6F)
                    .texOffs(0, 0).addBox(6.25F, -0.15F, -0.60F, 2.00F, 2.00F, 1.2F)
                    .texOffs(0, 0).addBox(8.25F, 0.20F, -0.40F, 1.35F, 1.25F, 0.8F)
                    .texOffs(0, 0).addBox(9.60F, 0.45F, -0.25F, 0.65F, 0.70F, 0.5F);
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
