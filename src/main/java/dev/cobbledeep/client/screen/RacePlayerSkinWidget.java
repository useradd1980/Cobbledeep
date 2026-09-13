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
 * geometry. The extra geometry is deliberately kept separate from character
 * data so later hair, beard, and other model parts can use the same approach.
 */
public class RacePlayerSkinWidget extends PlayerSkinWidget
{
    private static final float EAR_ROTATION_SENSITIVITY = 2.5F;
    private static final float EAR_PITCH_LIMIT = 30.0F;

    private final Supplier<CharacterRace> raceSupplier;
    private final Supplier<PlayerSkin> skinSupplier;
    private final ModelPart elfEars;
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
        this.elfEars = createElfEars();
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
        previewYaw += (float) dragX * EAR_ROTATION_SENSITIVITY;
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

        float earScale = race == CharacterRace.ELF ? 1.0F : 0.68F;
        float modelScale = Math.max(18.0F, getHeight() / 5.5F);
        float headCenterX = getX() + getWidth() / 2.0F;
        float headCenterY = getY() + getHeight() * 0.205F;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(headCenterX, headCenterY, 180.0F);
        pose.scale(modelScale * earScale, modelScale, modelScale);
        pose.mulPose(Axis.XP.rotationDegrees(previewPitch));
        pose.mulPose(Axis.YP.rotationDegrees(previewYaw));

        PlayerSkin skin = skinSupplier.get();
        elfEars.render(
                pose,
                graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(skin.texture())),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF);
        graphics.flush();
        pose.popPose();
    }

    private static ModelPart createElfEars()
    {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(
                "ears",
                CubeListBuilder.create()
                        // Left ear: three stepped cuboids taper to a blocky point.
                        .texOffs(0, 0).addBox(-7.0F, -1.75F, -1.0F, 3.0F, 3.5F, 2.0F)
                        .texOffs(0, 0).addBox(-9.0F, -1.25F, -0.75F, 2.0F, 2.5F, 1.5F)
                        .texOffs(0, 0).addBox(-10.0F, -0.75F, -0.5F, 1.0F, 1.5F, 1.0F)
                        // Right ear mirrors the same stepped profile.
                        .texOffs(0, 0).addBox(4.0F, -1.75F, -1.0F, 3.0F, 3.5F, 2.0F)
                        .texOffs(0, 0).addBox(7.0F, -1.25F, -0.75F, 2.0F, 2.5F, 1.5F)
                        .texOffs(0, 0).addBox(9.0F, -0.75F, -0.5F, 1.0F, 1.5F, 1.0F),
                PartPose.ZERO);

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
