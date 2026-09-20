package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * The Cobbledeep HUD has its own font: Minecraft's chat, menus and tooltips keep
 * their normal font. Measurements must use the same font/scale as actual drawing.
 */
public final class FantasyUiFont {
    public static final float SCALE = 0.55F;
    private static final ResourceLocation FONT =
            ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "fantasy");

    private FantasyUiFont() { }

    /** Applies the fantasy font as the inherited font without erasing combat colours. */
    public static Component style(Component text) {
        return Component.empty().withStyle(s -> s.withFont(FONT)).append(text.copy());
    }

    public static int width(Minecraft mc, String text) {
        return (int) Math.ceil(mc.font.width(style(Component.literal(text))) * SCALE);
    }

    public static void draw(GuiGraphics graphics, Minecraft mc, Component text,
                            float x, float y, int colour) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.drawString(mc.font, style(text), 0, 0, colour, false);
        graphics.pose().popPose();
    }

    public static void draw(GuiGraphics graphics, Minecraft mc, String text,
                            float x, float y, int colour) {
        draw(graphics, mc, Component.literal(text), x, y, colour);
    }

    public static void drawCentered(GuiGraphics graphics, Minecraft mc, String text,
                                    float centerX, float y, int colour) {
        Component styled = style(Component.literal(text));
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0.0F);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.drawCenteredString(mc.font, styled, 0, 0, colour);
        graphics.pose().popPose();
    }

    /** A wrapped line already carries the custom font from font-aware split(). */
    public static void drawLine(GuiGraphics graphics, Minecraft mc, FormattedCharSequence line,
                                float x, float y) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.drawString(mc.font, line, 0, 0, 0xFFFFFFFF, false);
        graphics.pose().popPose();
    }
}
