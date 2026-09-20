package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * Cobbledeep's native-size book font for small HUD text and decorative font
 * for headings. Both are rasterised at the size in their font JSON; avoid
 * scaling glyphs down with the GUI pose, which breaks fine strokes.
 */
public final class FantasyUiFont {
    private static final ResourceLocation BODY_FONT =
            ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "fantasy");
    private static final ResourceLocation HEADING_FONT =
            ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "fantasy_heading");

    private FantasyUiFont() { }

    /** Retain colours on child components (notably coloured combat rolls). */
    public static Component style(Component text) {
        return Component.empty().withStyle(s -> s.withFont(BODY_FONT)).append(text.copy());
    }

    private static Component heading(String text) {
        return Component.empty().withStyle(s -> s.withFont(HEADING_FONT))
                .append(Component.literal(text));
    }

    /** Width and wrapping are measured in the same unscaled pixels as drawing. */
    public static int width(Minecraft mc, String text) {
        return mc.font.width(style(Component.literal(text)));
    }

    public static void draw(GuiGraphics graphics, Minecraft mc, Component text,
                            float x, float y, int colour) {
        graphics.drawString(mc.font, style(text), Math.round(x), Math.round(y), colour, false);
    }

    public static void draw(GuiGraphics graphics, Minecraft mc, String text,
                            float x, float y, int colour) {
        draw(graphics, mc, Component.literal(text), x, y, colour);
    }

    public static void drawCentered(GuiGraphics graphics, Minecraft mc, String text,
                                    float centerX, float y, int colour) {
        graphics.drawCenteredString(mc.font, style(Component.literal(text)),
                Math.round(centerX), Math.round(y), colour);
    }

    public static void drawHeading(GuiGraphics graphics, Minecraft mc, String text,
                                   float centerX, float y, int colour) {
        graphics.drawCenteredString(mc.font, heading(text),
                Math.round(centerX), Math.round(y), colour);
    }

    /** Split lines already retain the coloured, custom-font text styles. */
    public static void drawLine(GuiGraphics graphics, Minecraft mc, FormattedCharSequence line,
                                float x, float y) {
        graphics.drawString(mc.font, line, Math.round(x), Math.round(y), 0xFFFFFFFF, false);
    }
}
