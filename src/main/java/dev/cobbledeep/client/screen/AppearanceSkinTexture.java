package dev.cobbledeep.client.screen;

import com.mojang.blaze3d.platform.NativeImage;

import dev.cobbledeep.character.CharacterAppearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Builds a simple 64x64 Minecraft skin from Cobbledeep's semantic appearance
 * choices. The texture is cached and only regenerated when one of the visible
 * appearance values changes.
 */
public final class AppearanceSkinTexture
{
    private static DynamicTexture texture;
    private static ResourceLocation textureLocation;
    private static String lastKey = "";

    private AppearanceSkinTexture() { }

    public static ResourceLocation get(CharacterAppearance appearance)
    {
        if (texture == null)
        {
            texture = new DynamicTexture(64, 64, false);
            textureLocation = Minecraft.getInstance().getTextureManager()
                    .register("cobbledeep_appearance_preview", texture);
        }

        String key = buildKey(appearance);
        if (!key.equals(lastKey))
        {
            rebuild(appearance);
            lastKey = key;
        }

        return textureLocation;
    }

    private static String buildKey(CharacterAppearance appearance)
    {
        if (appearance == null) return "default";
        return appearance.getSkinTone() + "|"
                + appearance.getHairStyle() + "|"
                + appearance.getHairColor() + "|"
                + appearance.getEyeColor() + "|"
                + appearance.getFacialHair() + "|"
                + appearance.getShirtColor() + "|"
                + appearance.getTrouserColor();
    }

    private static void rebuild(CharacterAppearance appearance)
    {
        NativeImage pixels = texture.getPixels();
        if (pixels == null || appearance == null) return;

        clear(pixels);

        int skin = appearance.getSkinTone().getRgb();
        int shirt = appearance.getShirtColor().getRgb();
        int trousers = appearance.getTrouserColor().getRgb();
        int hair = appearance.getHairColor().getRgb();
        int eyes = appearance.getEyeColor().getRgb();

        paintHead(pixels, skin);
        paintTorso(pixels, shirt);
        paintRightArm(pixels, skin, shirt);
        paintLeftArm(pixels, skin, shirt);
        paintRightLeg(pixels, trousers);
        paintLeftLeg(pixels, trousers);
        paintFace(pixels, skin, hair, eyes, appearance.getHairStyle());
        paintHair(pixels, hair, appearance.getHairStyle());

        texture.upload();
    }

    private static void clear(NativeImage image)
    {
        for (int y = 0; y < 64; y++)
        {
            for (int x = 0; x < 64; x++)
            {
                image.setPixelRGBA(x, y, 0x00000000);
            }
        }
    }

    private static void paintHead(NativeImage image, int rgb)
    {
        paintBoxFaces(image, rgb,
                8, 0, 8, 8,
                16, 0, 8, 8,
                0, 8, 8, 8,
                8, 8, 8, 8,
                16, 8, 8, 8,
                24, 8, 8, 8);
    }

    private static void paintTorso(NativeImage image, int rgb)
    {
        paintBoxFaces(image, rgb,
                20, 16, 8, 4,
                28, 16, 8, 4,
                16, 20, 4, 12,
                20, 20, 8, 12,
                28, 20, 4, 12,
                32, 20, 8, 12);
    }

    private static void paintRightArm(NativeImage image, int skin, int shirt)
    {
        paintBoxFaces(image, skin,
                44, 16, 4, 4,
                48, 16, 4, 4,
                40, 20, 4, 12,
                44, 20, 4, 12,
                48, 20, 4, 12,
                52, 20, 4, 12);
        paintRect(image, 40, 20, 4, 4, shade(shirt, 0.90F));
        paintRect(image, 44, 20, 4, 4, shirt);
        paintRect(image, 48, 20, 4, 4, shade(shirt, 0.82F));
        paintRect(image, 52, 20, 4, 4, shade(shirt, 0.76F));
        paintRect(image, 44, 16, 4, 4, shade(shirt, 1.08F));
    }

    private static void paintLeftArm(NativeImage image, int skin, int shirt)
    {
        paintBoxFaces(image, skin,
                36, 48, 4, 4,
                40, 48, 4, 4,
                32, 52, 4, 12,
                36, 52, 4, 12,
                40, 52, 4, 12,
                44, 52, 4, 12);
        paintRect(image, 32, 52, 4, 4, shade(shirt, 0.90F));
        paintRect(image, 36, 52, 4, 4, shirt);
        paintRect(image, 40, 52, 4, 4, shade(shirt, 0.82F));
        paintRect(image, 44, 52, 4, 4, shade(shirt, 0.76F));
        paintRect(image, 36, 48, 4, 4, shade(shirt, 1.08F));
    }

    private static void paintRightLeg(NativeImage image, int rgb)
    {
        paintBoxFaces(image, rgb,
                4, 16, 4, 4,
                8, 16, 4, 4,
                0, 20, 4, 12,
                4, 20, 4, 12,
                8, 20, 4, 12,
                12, 20, 4, 12);
    }

    private static void paintLeftLeg(NativeImage image, int rgb)
    {
        paintBoxFaces(image, rgb,
                20, 48, 4, 4,
                24, 48, 4, 4,
                16, 52, 4, 12,
                20, 52, 4, 12,
                24, 52, 4, 12,
                28, 52, 4, 12);
    }

    private static void paintFace(
            NativeImage image,
            int skin,
            int hair,
            int eyes,
            CharacterAppearance.HairStyle hairStyle)
    {
        // Front of the base head is x=8..15, y=8..15.
        int eyeWhite = 0xE8E8E8;
        paintPixel(image, 9, 11, eyeWhite);
        paintPixel(image, 10, 11, eyes);
        paintPixel(image, 13, 11, eyes);
        paintPixel(image, 14, 11, eyeWhite);

        // A tiny amount of facial shading keeps the generated face from
        // looking like one perfectly flat block of colour.
        paintPixel(image, 11, 13, shade(skin, 0.88F));
        paintPixel(image, 12, 13, shade(skin, 0.88F));
        paintPixel(image, 11, 14, shade(skin, 0.78F));
        paintPixel(image, 12, 14, shade(skin, 0.78F));

        if (hairStyle != CharacterAppearance.HairStyle.BALD)
        {
            paintRect(image, 8, 8, 8, hairStyle == CharacterAppearance.HairStyle.CROPPED ? 2 : 3, hair);
            if (hairStyle != CharacterAppearance.HairStyle.CROPPED)
            {
                paintRect(image, 8, 10, 1, 3, shade(hair, 0.82F));
                paintRect(image, 15, 10, 1, 3, shade(hair, 0.82F));
            }
        }
    }

    private static void paintHair(NativeImage image, int hair, CharacterAppearance.HairStyle style)
    {
        if (style == CharacterAppearance.HairStyle.BALD) return;

        paintRect(image, 8, 0, 8, 8, shade(hair, 1.08F));
        int sideDepth = style == CharacterAppearance.HairStyle.CROPPED ? 2
                : style == CharacterAppearance.HairStyle.SHORT ? 4 : 7;
        paintRect(image, 0, 8, 8, sideDepth, shade(hair, 0.82F));
        paintRect(image, 16, 8, 8, sideDepth, shade(hair, 0.90F));
        paintRect(image, 24, 8, 8, Math.max(4, sideDepth), shade(hair, 0.76F));

        if (style == CharacterAppearance.HairStyle.SHOULDER_LENGTH
                || style == CharacterAppearance.HairStyle.LONG
                || style == CharacterAppearance.HairStyle.BRAIDED)
        {
            int length = style == CharacterAppearance.HairStyle.SHOULDER_LENGTH ? 3 : 6;
            paintRect(image, 34, 20, 4, length, shade(hair, 0.72F));
            if (style == CharacterAppearance.HairStyle.BRAIDED)
            {
                paintRect(image, 35, 20, 2, 10, shade(hair, 0.68F));
            }
        }
    }

    private static void paintBoxFaces(
            NativeImage image,
            int rgb,
            int topX, int topY, int topW, int topH,
            int bottomX, int bottomY, int bottomW, int bottomH,
            int rightX, int rightY, int rightW, int rightH,
            int frontX, int frontY, int frontW, int frontH,
            int leftX, int leftY, int leftW, int leftH,
            int backX, int backY, int backW, int backH)
    {
        paintRect(image, topX, topY, topW, topH, shade(rgb, 1.08F));
        paintRect(image, bottomX, bottomY, bottomW, bottomH, shade(rgb, 0.72F));
        paintRect(image, rightX, rightY, rightW, rightH, shade(rgb, 0.90F));
        paintRect(image, frontX, frontY, frontW, frontH, rgb);
        paintRect(image, leftX, leftY, leftW, leftH, shade(rgb, 0.82F));
        paintRect(image, backX, backY, backW, backH, shade(rgb, 0.76F));
    }

    private static void paintRect(NativeImage image, int x, int y, int width, int height, int rgb)
    {
        int abgr = toAbgr(rgb);
        for (int py = y; py < y + height; py++)
        {
            for (int px = x; px < x + width; px++)
            {
                image.setPixelRGBA(px, py, abgr);
            }
        }
    }

    private static void paintPixel(NativeImage image, int x, int y, int rgb)
    {
        image.setPixelRGBA(x, y, toAbgr(rgb));
    }

    private static int shade(int rgb, float factor)
    {
        int r = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    private static int toAbgr(int rgb)
    {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}
