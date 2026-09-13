package dev.cobbledeep.client.screen;

import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterRace;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Player-skin preview that applies Cobbledeep race proportions while retaining
 * the vanilla skin widget's click-and-drag rotation behaviour.
 *
 * Elf and Half-Elf ears are rendered in the same model coordinate system as
 * the vanilla player. Each ear is attached to the side of the head and given
 * its own slight upward angle so it reads as part of the head instead of a
 * horizontal bar.
 */
public class RacePlayerSkinWidget extends PlayerSkinWidget
{
    private static final float ROTATION_SENSITIVITY = 2.5F;
    private static final float ROTATION_X_LIMIT = 50.0F;
    private static final float DEFAULT_ROTATION_X = -5.0F;
    private static final float DEFAULT_ROTATION_Y = 30.0F;

    private static final float MODEL_OFFSET = 0.0625F;
    private static final float MODEL_HEIGHT = 2.125F;
    private static final float Z_OFFSET = 100.0F;
    private static final float ROTATION_PIVOT_Y = -1.0625F;
    private static final float MODEL_TRANSLATE_Y = -1.5F;

    private static final float ELF_EAR_ANGLE = 12.0F;
    private static final float HALF_ELF_EAR_ANGLE = 8.0F;

    // Approximate exposed-skin colour used by the current vanilla preview skin.
    // Once the whole preview skin is generated from CharacterAppearance, the
    // six-argument constructor below will supply the selected tone instead.
    private static final int DEFAULT_PREVIEW_SKIN_COLOR = 0xFFB47A60;

    private static ResourceLocation earWhiteTexture;

    private final Supplier<CharacterRace> raceSupplier;
    private final Supplier<CharacterAppearance> appearanceSupplier;
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
        this(width, height, modelSet, skinSupplier, raceSupplier, () -> null);
    }

    public RacePlayerSkinWidget(
            int width,
            int height,
            EntityModelSet modelSet,
            Supplier<PlayerSkin> skinSupplier,
            Supplier<CharacterRace> raceSupplier,
            Supplier<CharacterAppearance> appearanceSupplier)
    {
        super(width, height, modelSet, skinSupplier);
        this.raceSupplier = raceSupplier;
        this.appearanceSupplier = appearanceSupplier;
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

        pose.translate(
                getX() + getWidth() / 2.0F,
                getY() + getHeight(),
                Z_OFFSET);

        float modelScale = getHeight() / MODEL_HEIGHT;
        pose.scale(modelScale, modelScale, modelScale);
        pose.translate(0.0F, -MODEL_OFFSET, 0.0F);

        pose.translate(0.0F, ROTATION_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(previewRotationX));
        pose.translate(0.0F, -ROTATION_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(previewRotationY));

        pose.scale(1.0F, 1.0F, -1.0F);
        pose.translate(0.0F, MODEL_TRANSLATE_Y, 0.0F);

        ModelPart ears = race == CharacterRace.ELF ? elfEars : halfElfEars;
        CharacterAppearance appearance = appearanceSupplier.get();
        int earColor = DEFAULT_PREVIEW_SKIN_COLOR;
        if (appearance != null && appearance.getSkinTone() != null)
        {
            earColor = 0xFF000000 | appearance.getSkinTone().getRgb();
        }

        ears.render(
                pose,
                graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getEarWhiteTexture())),
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                earColor);

        graphics.flush();
        pose.popPose();
    }

    private static ResourceLocation getEarWhiteTexture()
    {
        if (earWhiteTexture == null)
        {
            DynamicTexture texture = new DynamicTexture(1, 1, false);
            NativeImage pixels = texture.getPixels();
            if (pixels != null)
            {
                pixels.setPixelRGBA(0, 0, 0xFFFFFFFF);
                texture.upload();
            }
            earWhiteTexture = Minecraft.getInstance().getTextureManager()
                    .register("cobbledeep_ear_white", texture);
        }
        return earWhiteTexture;
    }

    private static ModelPart createEars(boolean halfElf)
    {
        MeshDefinition mesh = new MeshDefinition();
        float angle = (float) Math.toRadians(halfElf ? HALF_ELF_EAR_ANGLE : ELF_EAR_ANGLE);

        CubeListBuilder left = CubeListBuilder.create();
        CubeListBuilder right = CubeListBuilder.create();

        if (halfElf)
        {
            left
                    .texOffs(0, 0).addBox(-1.20F, -0.75F, -0.55F, 1.20F, 1.50F, 1.10F)
                    .texOffs(0, 0).addBox(-2.10F, -0.50F, -0.40F, 0.90F, 1.00F, 0.80F)
                    .texOffs(0, 0).addBox(-2.55F, -0.25F, -0.25F, 0.45F, 0.50F, 0.50F);
            right
                    .texOffs(0, 0).addBox(0.00F, -0.75F, -0.55F, 1.20F, 1.50F, 1.10F)
                    .texOffs(0, 0).addBox(1.20F, -0.50F, -0.40F, 0.90F, 1.00F, 0.80F)
                    .texOffs(0, 0).addBox(2.10F, -0.25F, -0.25F, 0.45F, 0.50F, 0.50F);
        }
        else
        {
            left
                    .texOffs(0, 0).addBox(-1.45F, -0.90F, -0.65F, 1.45F, 1.80F, 1.30F)
                    .texOffs(0, 0).addBox(-2.65F, -0.65F, -0.50F, 1.20F, 1.30F, 1.00F)
                    .texOffs(0, 0).addBox(-3.45F, -0.40F, -0.35F, 0.80F, 0.80F, 0.70F)
                    .texOffs(0, 0).addBox(-3.85F, -0.20F, -0.20F, 0.40F, 0.40F, 0.40F);
            right
                    .texOffs(0, 0).addBox(0.00F, -0.90F, -0.65F, 1.45F, 1.80F, 1.30F)
                    .texOffs(0, 0).addBox(1.45F, -0.65F, -0.50F, 1.20F, 1.30F, 1.00F)
                    .texOffs(0, 0).addBox(2.65F, -0.40F, -0.35F, 0.80F, 0.80F, 0.70F)
                    .texOffs(0, 0).addBox(3.45F, -0.20F, -0.20F, 0.40F, 0.40F, 0.40F);
        }

        mesh.getRoot().addOrReplaceChild(
                "left_ear",
                left,
                PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, 0.0F, 0.0F, angle));
        mesh.getRoot().addOrReplaceChild(
                "right_ear",
                right,
                PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, 0.0F, 0.0F, -angle));

        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
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
