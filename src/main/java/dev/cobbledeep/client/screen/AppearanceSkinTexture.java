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

        paintBlotchyRect(image, 40, 20, 4, 4, shirt, 0.92F, 31);
        paintBlotchyRect(image, 44, 20, 4, 4, shirt, 1.00F, 37);
        paintBlotchyRect(image, 48, 20, 4, 4, shirt, 0.86F, 41);
        paintBlotchyRect(image, 52, 20, 4, 4, shirt, 0.82F, 43);
        paintBlotchyRect(image, 44, 16, 4, 4, shirt, 1.05F, 47);
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

        paintBlotchyRect(image, 32, 52, 4, 4, shirt, 0.92F, 53);
        paintBlotchyRect(image, 36, 52, 4, 4, shirt, 1.00F, 59);
        paintBlotchyRect(image, 40, 52, 4, 4, shirt, 0.86F, 61);
        paintBlotchyRect(image, 44, 52, 4, 4, shirt, 0.82F, 67);
        paintBlotchyRect(image, 36, 48, 4, 4, shirt, 1.05F, 71);
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
        int eyeWhite = 0xE8E8E8;
        paintPixel(image, 9, 11, eyeWhite);
        paintPixel(image, 10, 11, eyes);
        paintPixel(image, 13, 11, eyes);
        paintPixel(image, 14, 11, eyeWhite);

        paintPixel(image, 11, 13, shade(skin, 0.88F));
        paintPixel(image, 12, 13, shade(skin, 0.88F));
        paintPixel(image, 11, 14, shade(skin, 0.78F));
        paintPixel(image, 12, 14, shade(skin, 0.78F));

        if (hairStyle != CharacterAppearance.HairStyle.BALD)
        {
            int fringeHeight = hairStyle == CharacterAppearance.HairStyle.CROPPED ? 2 : 3;
            paintBlotchyRect(image, 8, 8, 8, fringeHeight, hair, 1.00F, 79);
            if (hairStyle != CharacterAppearance.HairStyle.CROPPED)
            {
                paintBlotchyRect(image, 8, 10, 1, 3, hair, 0.82F, 83);
                paintBlotchyRect(image, 15, 10, 1, 3, hair, 0.88F, 89);
            }
        }
    }

    private static void paintHair(NativeImage image, int hair, CharacterAppearance.HairStyle style)
    {
        if (style == CharacterAppearance.HairStyle.BALD) return;

        paintBlotchyRect(image, 8, 0, 8, 8, hair, 1.00F, 97);

        int sideDepth = style == CharacterAppearance.HairStyle.CROPPED ? 2
                : style == CharacterAppearance.HairStyle.SHORT ? 4 : 7;
        paintBlotchyRect(image, 0, 8, 8, sideDepth, hair, 0.86F, 101);
        paintBlotchyRect(image, 16, 8, 8, sideDepth, hair, 0.92F, 103);
        paintBlotchyRect(image, 24, 8, 8, Math.max(4, sideDepth), hair, 0.78F, 107);

        if (style == CharacterAppearance.HairStyle.SHOULDER_LENGTH
                || style == CharacterAppearance.HairStyle.LONG
                || style == CharacterAppearance.HairStyle.BRAIDED)
        {
            int length = style == CharacterAppearance.HairStyle.SHOULDER_LENGTH ? 4 : 7;

            // Texture the upper back from its very first row. A separate small
            // model-space bridge in RacePlayerSkinWidget closes the physical
            // head/body gap that the skin atlas alone cannot cover.
            paintBlotchyRect(image, 33, 20, 6, length, hair, 0.78F, 109);

            if (style == CharacterAppearance.HairStyle.BRAIDED)
            {
                paintBlotchyRect(image, 35, 20, 2, 10, hair, 0.72F, 113);
                for (int y = 21; y < 30; y += 2)
                {
                    paintPixel(image, 35, y, shade(hair, 0.92F));
                    paintPixel(image, 36, y + 1, shade(hair, 0.58F));
                }
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
        paintBlotchyRect(image, topX, topY, topW, topH, rgb, 1.05F, topX * 3 + topY);
        paintBlotchyRect(image, bottomX, bottomY, bottomW, bottomH, rgb, 0.78F, bottomX * 3 + bottomY);
        paintBlotchyRect(image, rightX, rightY, rightW, rightH, rgb, 0.94F, rightX * 3 + rightY);
        paintBlotchyRect(image, frontX, frontY, frontW, frontH, rgb, 1.00F, frontX * 3 + frontY);
        paintBlotchyRect(image, leftX, leftY, leftW, leftH, rgb, 0.88F, leftX * 3 + leftY);
        paintBlotchyRect(image, backX, backY, backW, backH, rgb, 0.84F, backX * 3 + backY);
    }

    private static void paintBlotchyRect(
            NativeImage image,
            int x,
            int y,
            int width,
            int height,
            int rgb,
            float baseFactor,
            int seed)
    {
        for (int py = y; py < y + height; py++)
        {
            for (int px = x; px < x + width; px++)
            {
                int hash = px * 73428767 ^ py * 912931 ^ seed * 19349663;
                hash ^= hash >>> 13;
                int bucket = Math.floorMod(hash, 11);

                float variation = switch (bucket)
                {
                    case 0 -> -0.13F;
                    case 1, 2 -> -0.07F;
                    case 8, 9 -> 0.06F;
                    case 10 -> 0.11F;
                    default -> 0.0F;
                };

                paintPixel(image, px, py, shade(rgb, baseFactor + variation));
            }
        }
    }

    private static void paintPixel(NativeImage image, int x, int y, int rgb)
    {
        image.setPixelRGBA(x, y, toAbgr(rgb));
    }

    private static int shade(int rgb, float factor)
    {
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 0xFF) * factor)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 0xFF) * factor)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 0xFF) * factor)));
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
