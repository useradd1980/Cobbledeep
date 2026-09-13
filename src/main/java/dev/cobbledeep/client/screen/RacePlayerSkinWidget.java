package dev.cobbledeep.client.screen;

import java.util.function.Supplier;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.PendingCharacter;
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
 * Race scaling is anchored at the character's feet so shorter races remain
 * planted at the same baseline instead of shrinking toward the widget centre.
 * Elf and Half-Elf ears are rendered in the same model coordinate system as
 * the vanilla player. Dwarves receive stout torso and arm geometry, while male
 * Dwarves also receive a prominent beard.
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

    private static final float ELF_EAR_UP_ANGLE = 32.0F;
    private static final float HALF_ELF_EAR_UP_ANGLE = 22.0F;
    private static final float ELF_EAR_BACK_ANGLE = 30.0F;
    private static final float HALF_ELF_EAR_BACK_ANGLE = 20.0F;

    private static final int DEFAULT_PREVIEW_SKIN_COLOR = 0xFFB47A60;
    private static final int DEFAULT_DWARF_BEARD_COLOR = 0xFF5B3825;
    private static final int DWARF_TUNIC_COLOR = 0xFF3E6F6A;

    private static ResourceLocation whiteTexture;
    private static float savedRotationX = DEFAULT_ROTATION_X;
    private static float savedRotationY = DEFAULT_ROTATION_Y;

    private final Supplier<CharacterRace> raceSupplier;
    private final Supplier<CharacterAppearance> appearanceSupplier;
    private final Supplier<PendingCharacter.Gender> genderSupplier;
    private final ModelPart elfEars;
    private final ModelPart halfElfEars;
    private final ModelPart dwarfMaleBuild;
    private final ModelPart dwarfFemaleBuild;
    private final ModelPart dwarfArms;
    private final ModelPart dwarfBeard;
    private final ModelPart dwarfBeardHighlights;
    private final ModelPart dwarfBeardShadows;

    private float previewRotationX = savedRotationX;
    private float previewRotationY = savedRotationY;

    public RacePlayerSkinWidget(
            int width,
            int height,
            EntityModelSet modelSet,
            Supplier<PlayerSkin> skinSupplier,
            Supplier<CharacterRace> raceSupplier)
    {
        this(width, height, modelSet, skinSupplier, raceSupplier, () -> null, () -> PendingCharacter.Gender.MALE);
    }

    public RacePlayerSkinWidget(
            int width,
            int height,
            EntityModelSet modelSet,
            Supplier<PlayerSkin> skinSupplier,
            Supplier<CharacterRace> raceSupplier,
            Supplier<CharacterAppearance> appearanceSupplier)
    {
        this(width, height, modelSet, skinSupplier, raceSupplier, appearanceSupplier, () -> PendingCharacter.Gender.MALE);
    }

    public RacePlayerSkinWidget(
            int width,
            int height,
            EntityModelSet modelSet,
            Supplier<PlayerSkin> skinSupplier,
            Supplier<CharacterRace> raceSupplier,
            Supplier<CharacterAppearance> appearanceSupplier,
            Supplier<PendingCharacter.Gender> genderSupplier)
    {
        super(width, height, modelSet,
                () -> buildAppearanceSkin(skinSupplier, appearanceSupplier, genderSupplier));
        this.raceSupplier = raceSupplier;
        this.appearanceSupplier = appearanceSupplier;
        this.genderSupplier = genderSupplier;
        this.elfEars = createEars(false);
        this.halfElfEars = createEars(true);
        this.dwarfMaleBuild = createDwarfMaleBuild();
        this.dwarfFemaleBuild = createDwarfFemaleBuild();
        this.dwarfArms = createDwarfArms();
        this.dwarfBeard = createDwarfBeard();
        this.dwarfBeardHighlights = createDwarfBeardHighlights();
        this.dwarfBeardShadows = createDwarfBeardShadows();

        // Appearance controls rebuild the page and therefore create a fresh
        // widget. Reapply the previous drag rotation to both the vanilla model
        // and Cobbledeep's overlay geometry so the preview does not snap home.
        float dragX = (savedRotationY - DEFAULT_ROTATION_Y) / ROTATION_SENSITIVITY;
        float dragY = (DEFAULT_ROTATION_X - savedRotationX) / ROTATION_SENSITIVITY;
        if (dragX != 0.0F || dragY != 0.0F)
        {
            super.onDrag(0.0, 0.0, dragX, dragY);
        }
        previewRotationX = savedRotationX;
        previewRotationY = savedRotationY;
    }

    private static PlayerSkin buildAppearanceSkin(
            Supplier<PlayerSkin> fallbackSupplier,
            Supplier<CharacterAppearance> appearanceSupplier,
            Supplier<PendingCharacter.Gender> genderSupplier)
    {
        PlayerSkin fallback = fallbackSupplier.get();
        CharacterAppearance appearance = appearanceSupplier.get();
        if (appearance == null)
        {
            return fallback;
        }

        PlayerSkin.Model model = genderSupplier.get() == PendingCharacter.Gender.FEMALE
                ? PlayerSkin.Model.SLIM
                : PlayerSkin.Model.WIDE;

        return new PlayerSkin(
                AppearanceSkinTexture.get(appearance),
                fallback.textureUrl(),
                fallback.capeTexture(),
                fallback.elytraTexture(),
                model,
                false);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        CharacterRace race = raceSupplier.get();
        RaceScale scale = getRaceScale(race);
        float centerX = getX() + getWidth() / 2.0F;
        float feetY = getY() + getHeight();

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, feetY, 0.0F);
        graphics.pose().scale(scale.widthScale(), scale.heightScale(), 1.0F);
        graphics.pose().translate(-centerX, -feetY, 0.0F);

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
        savedRotationX = previewRotationX;
        savedRotationY = previewRotationY;
    }

    private void renderRaceGeometry(GuiGraphics graphics, CharacterRace race)
    {
        if (race != CharacterRace.ELF && race != CharacterRace.HALF_ELF && race != CharacterRace.DWARF)
        {
            return;
        }

        PoseStack pose = graphics.pose();
        pose.pushPose();

        pose.translate(getX() + getWidth() / 2.0F, getY() + getHeight(), Z_OFFSET);

        float modelScale = getHeight() / MODEL_HEIGHT;
        pose.scale(modelScale, modelScale, modelScale);
        pose.translate(0.0F, -MODEL_OFFSET, 0.0F);

        pose.translate(0.0F, ROTATION_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(previewRotationX));
        pose.translate(0.0F, -ROTATION_PIVOT_Y, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(previewRotationY));

        pose.scale(1.0F, 1.0F, -1.0F);
        pose.translate(0.0F, MODEL_TRANSLATE_Y, 0.0F);

        if (race == CharacterRace.DWARF)
        {
            renderDwarfBuild(graphics, pose);
            if (genderSupplier.get() != PendingCharacter.Gender.FEMALE)
            {
                renderDwarfBeard(graphics, pose);
            }
        }
        else
        {
            renderElvenEars(graphics, pose, race);
        }

        graphics.flush();
        pose.popPose();
    }

    private void renderElvenEars(GuiGraphics graphics, PoseStack pose, CharacterRace race)
    {
        ModelPart ears = race == CharacterRace.ELF ? elfEars : halfElfEars;
        CharacterAppearance appearance = appearanceSupplier.get();
        int earColor = DEFAULT_PREVIEW_SKIN_COLOR;
        if (appearance != null && appearance.getSkinTone() != null)
        {
            earColor = 0xFF000000 | appearance.getSkinTone().getRgb();
        }

        ears.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, earColor);
    }

    private void renderDwarfBuild(GuiGraphics graphics, PoseStack pose)
    {
        CharacterAppearance appearance = appearanceSupplier.get();
        int tunicColor = DWARF_TUNIC_COLOR;
        if (appearance != null && appearance.getShirtColor() != null)
        {
            tunicColor = 0xFF000000 | appearance.getShirtColor().getRgb();
        }

        ModelPart torso = genderSupplier.get() == PendingCharacter.Gender.FEMALE
                ? dwarfFemaleBuild
                : dwarfMaleBuild;

        torso.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tunicColor);
        dwarfArms.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tunicColor);
    }

    private void renderDwarfBeard(GuiGraphics graphics, PoseStack pose)
    {
        CharacterAppearance appearance = appearanceSupplier.get();
        int beardColor = DEFAULT_DWARF_BEARD_COLOR;
        if (appearance != null && appearance.getHairColor() != null)
        {
            beardColor = 0xFF000000 | appearance.getHairColor().getRgb();
        }

        int shadowColor = scaleRgb(beardColor, 0.58F);
        int highlightColor = scaleRgb(beardColor, 1.35F);

        dwarfBeardShadows.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, shadowColor);
        dwarfBeard.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, beardColor);
        dwarfBeardHighlights.render(pose, graphics.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, highlightColor);
    }

    private static int scaleRgb(int argb, float factor)
    {
        int r = Math.min(255, Math.round(((argb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((argb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((argb & 0xFF) * factor));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static ResourceLocation getWhiteTexture()
    {
        if (whiteTexture == null)
        {
            DynamicTexture texture = new DynamicTexture(1, 1, false);
            NativeImage pixels = texture.getPixels();
            if (pixels != null)
            {
                pixels.setPixelRGBA(0, 0, 0xFFFFFFFF);
                texture.upload();
            }
            whiteTexture = Minecraft.getInstance().getTextureManager().register("cobbledeep_preview_white", texture);
        }
        return whiteTexture;
    }

    private static ModelPart createDwarfMaleBuild()
    {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder torso = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.75F, 0.25F, -2.30F, 9.50F, 4.25F, 4.60F)
                .texOffs(0, 0).addBox(-4.45F, 4.50F, -2.20F, 8.90F, 4.00F, 4.40F)
                .texOffs(0, 0).addBox(-4.20F, 8.50F, -2.10F, 8.40F, 3.25F, 4.20F);
        mesh.getRoot().addOrReplaceChild("dwarf_male_torso", torso, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createDwarfFemaleBuild()
    {
        MeshDefinition mesh = new MeshDefinition();

        CubeListBuilder torso = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.50F, 0.35F, -2.25F, 9.00F, 4.00F, 4.50F)
                .texOffs(0, 0).addBox(-4.25F, 4.35F, -2.18F, 8.50F, 4.10F, 4.36F)
                .texOffs(0, 0).addBox(-4.15F, 8.45F, -2.12F, 8.30F, 3.25F, 4.24F);
        mesh.getRoot().addOrReplaceChild("dwarf_female_torso", torso, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createDwarfArms()
    {
        MeshDefinition mesh = new MeshDefinition();

        CubeListBuilder arms = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.10F, 1.10F, -2.15F, 2.45F, 5.35F, 4.30F)
                .texOffs(0, 0).addBox(4.65F, 1.10F, -2.15F, 2.45F, 5.35F, 4.30F);
        mesh.getRoot().addOrReplaceChild("dwarf_upper_arms", arms, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createDwarfBeard()
    {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder beard = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.35F, -2.55F, -4.65F, 6.70F, 1.20F, 0.85F)
                .texOffs(0, 0).addBox(-3.65F, -1.45F, -4.55F, 7.30F, 2.20F, 0.95F)
                .texOffs(0, 0).addBox(-3.20F, 0.60F, -3.90F, 6.40F, 2.35F, 1.25F)
                .texOffs(0, 0).addBox(-2.65F, 2.75F, -3.55F, 5.30F, 2.20F, 1.20F)
                .texOffs(0, 0).addBox(-1.80F, 4.75F, -3.25F, 3.60F, 1.45F, 1.05F);
        mesh.getRoot().addOrReplaceChild("dwarf_beard", beard, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createDwarfBeardHighlights()
    {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder highlights = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.45F, -2.45F, -4.78F, 2.90F, 0.55F, 0.24F)
                .texOffs(0, 0).addBox(-1.65F, -1.15F, -4.68F, 3.30F, 0.85F, 0.24F)
                .texOffs(0, 0).addBox(-2.25F, 0.90F, -4.02F, 1.25F, 3.40F, 0.26F)
                .texOffs(0, 0).addBox(1.00F, 0.90F, -4.02F, 1.25F, 3.40F, 0.26F)
                .texOffs(0, 0).addBox(-0.70F, 4.95F, -3.37F, 1.40F, 0.85F, 0.22F);
        mesh.getRoot().addOrReplaceChild("dwarf_beard_highlights", highlights, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createDwarfBeardShadows()
    {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder shadows = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.58F, -1.20F, -4.67F, 0.70F, 2.05F, 0.28F)
                .texOffs(0, 0).addBox(2.88F, -1.20F, -4.67F, 0.70F, 2.05F, 0.28F)
                .texOffs(0, 0).addBox(-2.95F, 2.35F, -3.70F, 0.75F, 2.35F, 0.30F)
                .texOffs(0, 0).addBox(2.20F, 2.35F, -3.70F, 0.75F, 2.35F, 0.30F)
                .texOffs(0, 0).addBox(-1.65F, 5.75F, -3.37F, 3.30F, 0.38F, 0.24F);
        mesh.getRoot().addOrReplaceChild("dwarf_beard_shadows", shadows, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createEars(boolean halfElf)
    {
        MeshDefinition mesh = new MeshDefinition();
        float upAngle = (float) Math.toRadians(halfElf ? HALF_ELF_EAR_UP_ANGLE : ELF_EAR_UP_ANGLE);
        float backAngle = (float) Math.toRadians(halfElf ? HALF_ELF_EAR_BACK_ANGLE : ELF_EAR_BACK_ANGLE);
        CubeListBuilder left = CubeListBuilder.create();
        CubeListBuilder right = CubeListBuilder.create();

        if (halfElf)
        {
            left.texOffs(0, 0).addBox(-1.20F, -0.75F, -0.55F, 1.20F, 1.50F, 1.10F)
                    .texOffs(0, 0).addBox(-2.10F, -0.50F, -0.40F, 0.90F, 1.00F, 0.80F)
                    .texOffs(0, 0).addBox(-2.55F, -0.25F, -0.25F, 0.45F, 0.50F, 0.50F);
            right.texOffs(0, 0).addBox(0.00F, -0.75F, -0.55F, 1.20F, 1.50F, 1.10F)
                    .texOffs(0, 0).addBox(1.20F, -0.50F, -0.40F, 0.90F, 1.00F, 0.80F)
                    .texOffs(0, 0).addBox(2.10F, -0.25F, -0.25F, 0.45F, 0.50F, 0.50F);
        }
        else
        {
            left.texOffs(0, 0).addBox(-1.45F, -0.90F, -0.65F, 1.45F, 1.80F, 1.30F)
                    .texOffs(0, 0).addBox(-2.65F, -0.65F, -0.50F, 1.20F, 1.30F, 1.00F)
                    .texOffs(0, 0).addBox(-3.45F, -0.40F, -0.35F, 0.80F, 0.80F, 0.70F)
                    .texOffs(0, 0).addBox(-3.85F, -0.20F, -0.20F, 0.40F, 0.40F, 0.40F);
            right.texOffs(0, 0).addBox(0.00F, -0.90F, -0.65F, 1.45F, 1.80F, 1.30F)
                    .texOffs(0, 0).addBox(1.45F, -0.65F, -0.50F, 1.20F, 1.30F, 1.00F)
                    .texOffs(0, 0).addBox(2.65F, -0.40F, -0.35F, 0.80F, 0.80F, 0.70F)
                    .texOffs(0, 0).addBox(3.45F, -0.20F, -0.20F, 0.40F, 0.40F, 0.40F);
        }

        mesh.getRoot().addOrReplaceChild("left_ear", left,
                PartPose.offsetAndRotation(-4.0F, -4.0F, 0.0F, 0.0F, backAngle, upAngle));
        mesh.getRoot().addOrReplaceChild("right_ear", right,
                PartPose.offsetAndRotation(4.0F, -4.0F, 0.0F, 0.0F, -backAngle, -upAngle));
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static RaceScale getRaceScale(CharacterRace race)
    {
        if (race == null) return RaceScale.HUMAN;
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
