package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.PendingCharacter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Extra Cobbledeep player geometry rendered on top of the vanilla-compatible
 * player model. Parts are attached to the animated vanilla head/body transforms,
 * so hair, ears and beards follow head movement while the Dwarf torso follows
 * normal body animation.
 */
final class CobbledeepPlayerGeometry
{
    private static final int DEFAULT_SKIN_COLOR = 0xFFB47A60;
    private static final int DEFAULT_HAIR_COLOR = 0xFF5B3825;
    private static final int DEFAULT_TUNIC_COLOR = 0xFF3E6F6A;

    private static final ModelPart ELF_EARS = createEars(false);
    private static final ModelPart HALF_ELF_EARS = createEars(true);
    private static final ModelPart DWARF_MALE_TORSO = createDwarfMaleBuild();
    private static final ModelPart DWARF_FEMALE_TORSO = createDwarfFemaleBuild();
    private static final ModelPart DWARF_BEARD = createDwarfBeard();
    private static final ModelPart DWARF_BEARD_HIGHLIGHTS = createDwarfBeardHighlights();
    private static final ModelPart DWARF_BEARD_SHADOWS = createDwarfBeardShadows();

    private static final ModelPart CROPPED_HAIR = createHairGeometry(CharacterAppearance.HairStyle.CROPPED);
    private static final ModelPart SHORT_HAIR = createHairGeometry(CharacterAppearance.HairStyle.SHORT);
    private static final ModelPart SHOULDER_HAIR = createHairGeometry(CharacterAppearance.HairStyle.SHOULDER_LENGTH);
    private static final ModelPart LONG_HAIR = createHairGeometry(CharacterAppearance.HairStyle.LONG);
    private static final ModelPart BRAIDED_HAIR = createHairGeometry(CharacterAppearance.HairStyle.BRAIDED);
    private static final ModelPart HAIR_HIGHLIGHTS = createHairHighlights();
    private static final ModelPart HAIR_SHADOWS = createHairShadows();

    private static final ModelPart STUBBLE = createStubbleGeometry();
    private static final ModelPart MOUSTACHE = createMoustacheGeometry();
    private static final ModelPart GOATEE = createGoateeGeometry();
    private static final ModelPart SHORT_BEARD = createShortBeardGeometry();
    private static final ModelPart FULL_BEARD = createFullBeardGeometry();

    private static ResourceLocation whiteTexture;

    private CobbledeepPlayerGeometry() { }

    static void render(
            CharacterData data,
            PlayerModel<AbstractClientPlayer> model,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight)
    {
        if (data == null || !data.isCharacterCreated()) return;
        CharacterAppearance appearance = data.getAppearance();
        CharacterRace race = data.getRace();
        if (appearance == null || race == null) return;

        if (race == CharacterRace.DWARF)
        {
            renderDwarfTorso(data, model, pose, buffers, packedLight);
        }

        pose.pushPose();
        model.head.translateAndRotate(pose);

        if (race == CharacterRace.ELF || race == CharacterRace.HALF_ELF)
        {
            renderEars(race, appearance, pose, buffers, packedLight);
        }

        renderHair(appearance, pose, buffers, packedLight);

        if (data.getGender() != PendingCharacter.Gender.FEMALE)
        {
            renderFacialHair(race, appearance, pose, buffers, packedLight);
        }

        pose.popPose();
    }

    private static void renderDwarfTorso(
            CharacterData data,
            PlayerModel<AbstractClientPlayer> model,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight)
    {
        CharacterAppearance appearance = data.getAppearance();
        int color = appearance.getShirtColor() == null
                ? DEFAULT_TUNIC_COLOR
                : 0xFF000000 | appearance.getShirtColor().getRgb();
        ModelPart torso = data.getGender() == PendingCharacter.Gender.FEMALE
                ? DWARF_FEMALE_TORSO
                : DWARF_MALE_TORSO;

        pose.pushPose();
        model.body.translateAndRotate(pose);
        renderPart(torso, pose, buffers, packedLight, color);
        pose.popPose();
    }

    private static void renderEars(
            CharacterRace race,
            CharacterAppearance appearance,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight)
    {
        int color = appearance.getSkinTone() == null
                ? DEFAULT_SKIN_COLOR
                : 0xFF000000 | appearance.getSkinTone().getRgb();
        renderPart(race == CharacterRace.ELF ? ELF_EARS : HALF_ELF_EARS,
                pose, buffers, packedLight, color);
    }

    private static void renderHair(
            CharacterAppearance appearance,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight)
    {
        CharacterAppearance.HairStyle style = appearance.getHairStyle();
        if (style == null || style == CharacterAppearance.HairStyle.BALD) return;

        ModelPart hair = switch (style)
        {
            case CROPPED -> CROPPED_HAIR;
            case SHORT -> SHORT_HAIR;
            case SHOULDER_LENGTH -> SHOULDER_HAIR;
            case LONG -> LONG_HAIR;
            case BRAIDED -> BRAIDED_HAIR;
            default -> null;
        };
        if (hair == null) return;

        int color = appearance.getHairColor() == null
                ? DEFAULT_HAIR_COLOR
                : 0xFF000000 | appearance.getHairColor().getRgb();
        renderPart(HAIR_SHADOWS, pose, buffers, packedLight, scaleRgb(color, 0.72F));
        renderPart(hair, pose, buffers, packedLight, color);
        renderPart(HAIR_HIGHLIGHTS, pose, buffers, packedLight, scaleRgb(color, 1.18F));
    }

    private static void renderFacialHair(
            CharacterRace race,
            CharacterAppearance appearance,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight)
    {
        CharacterAppearance.FacialHair style = appearance.getFacialHair();
        if (style == null || style == CharacterAppearance.FacialHair.NONE) return;

        int color = appearance.getHairColor() == null
                ? DEFAULT_HAIR_COLOR
                : 0xFF000000 | appearance.getHairColor().getRgb();

        if (race == CharacterRace.DWARF && style == CharacterAppearance.FacialHair.FULL_BEARD)
        {
            renderPart(DWARF_BEARD_SHADOWS, pose, buffers, packedLight, scaleRgb(color, 0.58F));
            renderPart(DWARF_BEARD, pose, buffers, packedLight, color);
            renderPart(DWARF_BEARD_HIGHLIGHTS, pose, buffers, packedLight, scaleRgb(color, 1.35F));
            return;
        }

        ModelPart part = switch (style)
        {
            case STUBBLE -> STUBBLE;
            case MOUSTACHE -> MOUSTACHE;
            case GOATEE -> GOATEE;
            case SHORT_BEARD -> SHORT_BEARD;
            case FULL_BEARD -> FULL_BEARD;
            default -> null;
        };
        if (part != null) renderPart(part, pose, buffers, packedLight, color);
    }

    private static void renderPart(
            ModelPart part,
            PoseStack pose,
            MultiBufferSource buffers,
            int packedLight,
            int color)
    {
        part.render(
                pose,
                buffers.getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                color);
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
            whiteTexture = Minecraft.getInstance().getTextureManager()
                    .register("cobbledeep_ingame_white", texture);
        }
        return whiteTexture;
    }

    private static ModelPart createHairGeometry(CharacterAppearance.HairStyle style)
    {
        MeshDefinition mesh = new MeshDefinition();
        CubeListBuilder hair = CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.35F, -8.95F, -3.35F, 6.70F, 0.45F, 6.70F)
                .texOffs(0, 0).addBox(-3.85F, -8.55F, -3.85F, 7.70F, 0.55F, 7.70F)
                .texOffs(0, 0).addBox(-4.20F, -8.05F, -4.15F, 8.40F, 0.70F, 8.30F)
                .texOffs(0, 0).addBox(-3.80F, -7.55F, -4.32F, 7.60F, 1.30F, 0.55F);

        float sideLength = switch (style)
        {
            case CROPPED -> 2.25F;
            case SHORT -> 4.10F;
            default -> 6.60F;
        };

        hair.texOffs(0, 0).addBox(-4.35F, -7.55F, -3.80F, 0.55F, sideLength, 7.60F)
                .texOffs(0, 0).addBox(3.80F, -7.55F, -3.80F, 0.55F, sideLength, 7.60F)
                .texOffs(0, 0).addBox(-3.95F, -7.70F, 3.78F, 7.90F, Math.min(sideLength + 0.7F, 7.5F), 0.62F);

        if (style == CharacterAppearance.HairStyle.SHOULDER_LENGTH)
        {
            hair.texOffs(0, 0).addBox(-3.80F, -1.45F, 3.60F, 7.60F, 3.10F, 0.82F);
        }
        else if (style == CharacterAppearance.HairStyle.LONG)
        {
            hair.texOffs(0, 0).addBox(-3.85F, -1.45F, 3.58F, 7.70F, 6.20F, 0.86F)
                    .texOffs(0, 0).addBox(-3.25F, 4.55F, 3.54F, 6.50F, 2.10F, 0.82F);
        }
        else if (style == CharacterAppearance.HairStyle.BRAIDED)
        {
            hair.texOffs(0, 0).addBox(-2.20F, -1.35F, 3.58F, 4.40F, 2.15F, 0.84F)
                    .texOffs(0, 0).addBox(-1.20F, 0.65F, 3.48F, 2.40F, 7.10F, 1.02F)
                    .texOffs(0, 0).addBox(-0.82F, 7.50F, 3.42F, 1.64F, 1.55F, 0.92F);
        }

        mesh.getRoot().addOrReplaceChild("hair_" + style.name().toLowerCase(), hair, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }

    private static ModelPart createHairHighlights()
    {
        return createSimplePart("hair_highlights", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.35F, -9.02F, -2.65F, 2.30F, 0.16F, 4.20F)
                .texOffs(0, 0).addBox(-4.43F, -6.85F, -2.65F, 0.16F, 2.70F, 3.20F)
                .texOffs(0, 0).addBox(-2.30F, -6.80F, 4.42F, 2.00F, 3.25F, 0.16F));
    }

    private static ModelPart createHairShadows()
    {
        return createSimplePart("hair_shadows", CubeListBuilder.create()
                .texOffs(0, 0).addBox(2.05F, -8.62F, -2.20F, 1.35F, 0.16F, 4.90F)
                .texOffs(0, 0).addBox(4.27F, -6.45F, -1.60F, 0.16F, 3.20F, 4.50F)
                .texOffs(0, 0).addBox(1.05F, -5.85F, 4.40F, 2.20F, 3.60F, 0.16F));
    }

    private static ModelPart createStubbleGeometry()
    {
        return createSimplePart("stubble", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.10F, -1.45F, -4.16F, 1.85F, 1.30F, 0.16F)
                .texOffs(0, 0).addBox(1.25F, -1.45F, -4.16F, 1.85F, 1.30F, 0.16F)
                .texOffs(0, 0).addBox(-2.00F, -0.35F, -4.14F, 4.00F, 0.35F, 0.14F));
    }

    private static ModelPart createMoustacheGeometry()
    {
        return createSimplePart("moustache", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.00F, -2.30F, -4.38F, 1.85F, 0.70F, 0.42F)
                .texOffs(0, 0).addBox(1.15F, -2.30F, -4.38F, 1.85F, 0.70F, 0.42F));
    }

    private static ModelPart createGoateeGeometry()
    {
        return createSimplePart("goatee", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.00F, -2.30F, -4.38F, 1.85F, 0.70F, 0.42F)
                .texOffs(0, 0).addBox(1.15F, -2.30F, -4.38F, 1.85F, 0.70F, 0.42F)
                .texOffs(0, 0).addBox(-1.35F, -0.60F, -4.32F, 2.70F, 1.15F, 0.40F));
    }

    private static ModelPart createShortBeardGeometry()
    {
        return createSimplePart("short_beard", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.45F, -2.05F, -4.38F, 2.10F, 2.05F, 0.44F)
                .texOffs(0, 0).addBox(1.35F, -2.05F, -4.38F, 2.10F, 2.05F, 0.44F)
                .texOffs(0, 0).addBox(-2.35F, -0.35F, -4.12F, 4.70F, 1.10F, 0.52F));
    }

    private static ModelPart createFullBeardGeometry()
    {
        return createSimplePart("full_beard", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.70F, -2.20F, -4.48F, 2.35F, 2.55F, 0.54F)
                .texOffs(0, 0).addBox(1.35F, -2.20F, -4.48F, 2.35F, 2.55F, 0.54F)
                .texOffs(0, 0).addBox(-3.05F, -0.25F, -4.12F, 6.10F, 2.00F, 0.70F)
                .texOffs(0, 0).addBox(-2.10F, 1.55F, -3.82F, 4.20F, 1.15F, 0.72F));
    }

    private static ModelPart createDwarfMaleBuild()
    {
        return createSimplePart("dwarf_male_torso", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.75F, 0.25F, -2.30F, 9.50F, 4.25F, 4.60F)
                .texOffs(0, 0).addBox(-4.45F, 4.50F, -2.20F, 8.90F, 4.00F, 4.40F)
                .texOffs(0, 0).addBox(-4.20F, 8.50F, -2.10F, 8.40F, 3.25F, 4.20F));
    }

    private static ModelPart createDwarfFemaleBuild()
    {
        return createSimplePart("dwarf_female_torso", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.50F, 0.35F, -2.25F, 9.00F, 4.00F, 4.50F)
                .texOffs(0, 0).addBox(-4.25F, 4.35F, -2.18F, 8.50F, 4.10F, 4.36F)
                .texOffs(0, 0).addBox(-4.15F, 8.45F, -2.12F, 8.30F, 3.25F, 4.24F));
    }

    private static ModelPart createDwarfBeard()
    {
        return createSimplePart("dwarf_beard", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.35F, -2.55F, -4.65F, 2.40F, 1.20F, 0.85F)
                .texOffs(0, 0).addBox(0.95F, -2.55F, -4.65F, 2.40F, 1.20F, 0.85F)
                .texOffs(0, 0).addBox(-3.65F, -1.45F, -4.55F, 2.70F, 2.20F, 0.95F)
                .texOffs(0, 0).addBox(0.95F, -1.45F, -4.55F, 2.70F, 2.20F, 0.95F)
                .texOffs(0, 0).addBox(-3.20F, 0.60F, -3.90F, 6.40F, 2.35F, 1.25F)
                .texOffs(0, 0).addBox(-2.65F, 2.75F, -3.55F, 5.30F, 2.20F, 1.20F)
                .texOffs(0, 0).addBox(-1.80F, 4.75F, -3.25F, 3.60F, 1.45F, 1.05F));
    }

    private static ModelPart createDwarfBeardHighlights()
    {
        return createSimplePart("dwarf_beard_highlights", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-2.90F, -2.45F, -4.78F, 1.10F, 0.55F, 0.24F)
                .texOffs(0, 0).addBox(1.80F, -2.45F, -4.78F, 1.10F, 0.55F, 0.24F)
                .texOffs(0, 0).addBox(-2.25F, 0.90F, -4.02F, 1.25F, 3.40F, 0.26F)
                .texOffs(0, 0).addBox(1.00F, 0.90F, -4.02F, 1.25F, 3.40F, 0.26F)
                .texOffs(0, 0).addBox(-0.70F, 4.95F, -3.37F, 1.40F, 0.85F, 0.22F));
    }

    private static ModelPart createDwarfBeardShadows()
    {
        return createSimplePart("dwarf_beard_shadows", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.58F, -1.20F, -4.67F, 0.70F, 2.05F, 0.28F)
                .texOffs(0, 0).addBox(2.88F, -1.20F, -4.67F, 0.70F, 2.05F, 0.28F)
                .texOffs(0, 0).addBox(-2.95F, 2.35F, -3.70F, 0.75F, 2.35F, 0.30F)
                .texOffs(0, 0).addBox(2.20F, 2.35F, -3.70F, 0.75F, 2.35F, 0.30F)
                .texOffs(0, 0).addBox(-1.65F, 5.75F, -3.37F, 3.30F, 0.38F, 0.24F));
    }

    private static ModelPart createEars(boolean halfElf)
    {
        MeshDefinition mesh = new MeshDefinition();
        float upAngle = (float) Math.toRadians(halfElf ? 22.0F : 32.0F);
        float backAngle = (float) Math.toRadians(halfElf ? 20.0F : 30.0F);
        CubeListBuilder left = CubeListBuilder.create();
        CubeListBuilder right = CubeListBuilder.create();

        if (halfElf)
        {
            left.texOffs(0, 0).addBox(-1.20F, -0.75F, -0.55F, 1.20F, 1.50F, 1.10F)
                    .texOffs(0, 0).addBox(-2.10F, -0.50F, -0.40F, 0.90F, 1.00F, 0.80F)
                    .texOffs(0, 0).addBox(-2.55F, -0.25F, -0.25F, 0.45F, 0.50F, 0.50F);
            right.texOffs(0, 0).addBox(0.0F, -0.75F, -0.55F, 1.20F, 1.50F, 1.10F)
                    .texOffs(0, 0).addBox(1.20F, -0.50F, -0.40F, 0.90F, 1.00F, 0.80F)
                    .texOffs(0, 0).addBox(2.10F, -0.25F, -0.25F, 0.45F, 0.50F, 0.50F);
        }
        else
        {
            left.texOffs(0, 0).addBox(-1.45F, -0.90F, -0.65F, 1.45F, 1.80F, 1.30F)
                    .texOffs(0, 0).addBox(-2.65F, -0.65F, -0.50F, 1.20F, 1.30F, 1.00F)
                    .texOffs(0, 0).addBox(-3.45F, -0.40F, -0.35F, 0.80F, 0.80F, 0.70F)
                    .texOffs(0, 0).addBox(-3.85F, -0.20F, -0.20F, 0.40F, 0.40F, 0.40F);
            right.texOffs(0, 0).addBox(0.0F, -0.90F, -0.65F, 1.45F, 1.80F, 1.30F)
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

    private static ModelPart createSimplePart(String name, CubeListBuilder cubes)
    {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild(name, cubes, PartPose.ZERO);
        return LayerDefinition.create(mesh, 16, 16).bakeRoot();
    }
}
