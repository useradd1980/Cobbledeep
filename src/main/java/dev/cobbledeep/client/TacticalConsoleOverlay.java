package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** In-world log for combat, dialogue and system events; no vanilla HUD dependency. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalConsoleOverlay {
    public enum Category { COMBAT, DIALOGUE, SYSTEM }

    private record Entry(Category category, Component message, String time) { }
    private static final int HEIGHT = 110;
    private static final int PADDING = 5;
    private static final int HEADER_HEIGHT = 16;
    private static final int TRACK_WIDTH = 4;
    private static final int MAX_ENTRIES = 400;
    private static final int EDGE_COLOR = 0xFF718171;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<Entry> entries = new ArrayList<>();
    private static Category filter;
    private static boolean showTimes;
    /** Number of wrapped text lines above the newest line (zero means follow new messages). */
    private static int scrollFromBottom;
    private static Object currentWorld;

    private TacticalConsoleOverlay() { }

    private static boolean visible(Minecraft mc) {
        return TacticalCameraController.isEnabled() && mc.player != null && mc.level != null
                && mc.screen == null && !mc.options.hideGui;
    }

    private static int left() { return PartyActionBar.width(); }
    private static int right(Minecraft mc) { return mc.getWindow().getGuiScaledWidth() - PartyPortraitBar.width(); }
    private static int top(Minecraft mc) { return Math.max(0, mc.getWindow().getGuiScaledHeight() - HEIGHT); }
    private static int bottom(Minecraft mc) { return mc.getWindow().getGuiScaledHeight(); }

    private static double mouseX(Minecraft mc) {
        return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                / (double) mc.getWindow().getScreenWidth();
    }
    private static double mouseY(Minecraft mc) {
        return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                / (double) mc.getWindow().getScreenHeight();
    }

    static boolean isOverConsole(Minecraft mc) {
        if (!visible(mc) || mc.getWindow().getScreenWidth() <= 0
                || mc.getWindow().getScreenHeight() <= 0) return false;
        return mouseX(mc) >= left() && mouseX(mc) < right(mc)
                && mouseY(mc) >= top(mc) && mouseY(mc) < bottom(mc);
    }

    /** Also available to future NPC scripts and quest messages. */
    public static void addMessage(Category category, Component message) {
        if (category == null || message == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != currentWorld) {
            entries.clear();
            scrollFromBottom = 0;
            currentWorld = mc.level;
        }
        // Preserve the place the reader was looking at as new lines arrive.
        if (scrollFromBottom > 0) scrollFromBottom++;
        entries.add(new Entry(category, message.copy(), LocalTime.now().format(CLOCK)));
        if (entries.size() > MAX_ENTRIES) entries.remove(0);
    }

    public static void addMessage(Category category, String text, int rgb) {
        addMessage(category, Component.literal(text).withStyle(style -> style.withColor(rgb)));
    }

    private static List<FormattedCharSequence> lines(Minecraft mc, int maxWidth) {
        List<FormattedCharSequence> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (filter != null && filter != entry.category()) continue;
            Component formatted = showTimes
                    ? Component.literal("[" + entry.time() + "] ")
                        .withStyle(net.minecraft.ChatFormatting.GRAY).append(entry.message().copy())
                    : entry.message();
            result.addAll(mc.font.split(formatted, Math.max(20, maxWidth)));
        }
        return result;
    }

    private static int visibleLines(Minecraft mc) {
        return Math.max(1, (bottom(mc) - top(mc) - HEADER_HEIGHT - PADDING * 2)
                / (mc.font.lineHeight + 2));
    }

    private static int maxScroll(Minecraft mc) {
        return Math.max(0, lines(mc, Math.max(20, right(mc) - left() - 3 * PADDING - TRACK_WIDTH)).size()
                - visibleLines(mc));
    }

    /** Added to the existing tactical HUD layer, between the two sidebars. */
    public static void onHud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        if (mc.level != currentWorld) {
            entries.clear();
            scrollFromBottom = 0;
            currentWorld = mc.level;
        }
        int x0 = left(), x1 = right(mc), y0 = top(mc), y1 = bottom(mc);
        if (x1 - x0 < 70 || y1 - y0 < 35) return;
        graphics.fill(x0, y0, x1, y1, 0xF9000000);
        // Draw only horizontal console borders: the sidebar borders remain the sole vertical dividers.
        graphics.fill(x0, y0, x1, y0 + 1, EDGE_COLOR);
        graphics.fill(x0, y1 - 1, x1, y1, EDGE_COLOR);
        graphics.fill(x0, y0 + HEADER_HEIGHT, x1, y0 + HEADER_HEIGHT + 1, 0xFF3C5144);

        int tabX = x0 + PADDING;
        tabX = drawTab(graphics, mc, "ALL", filter == null, tabX, y0 + 3, 0);
        for (Category category : Category.values())
            tabX = drawTab(graphics, mc, category.name(), filter == category, tabX, y0 + 3, 0);
        String timeLabel = showTimes ? "TIME ON" : "TIME OFF";
        int timeWidth = mc.font.width(timeLabel) + 7;
        if (tabX + timeWidth + PADDING < x1)
            drawTab(graphics, mc, timeLabel, showTimes, x1 - PADDING - timeWidth, y0 + 3, timeWidth);

        int contentWidth = x1 - x0 - 3 * PADDING - TRACK_WIDTH;
        List<FormattedCharSequence> wrapped = lines(mc, contentWidth);
        int count = visibleLines(mc);
        int max = Math.max(0, wrapped.size() - count);
        scrollFromBottom = Math.min(scrollFromBottom, max);
        int start = Math.max(0, wrapped.size() - count - scrollFromBottom);
        int end = Math.min(wrapped.size(), start + count);
        int y = y0 + HEADER_HEIGHT + PADDING;
        for (int i = start; i < end; i++) {
            graphics.drawString(mc.font, wrapped.get(i), x0 + PADDING, y, 0xFFFFFFFF, false);
            y += mc.font.lineHeight + 2;
        }

        if (wrapped.size() > count) {
            int trackX = x1 - PADDING - TRACK_WIDTH;
            int trackY = y0 + HEADER_HEIGHT + PADDING;
            int trackHeight = y1 - PADDING - trackY;
            graphics.fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + trackHeight, 0xFF202B26);
            int thumbHeight = Math.max(8, trackHeight * count / wrapped.size());
            int thumbY = trackY + (trackHeight - thumbHeight) * (max - scrollFromBottom) / max;
            graphics.fill(trackX, thumbY, trackX + TRACK_WIDTH, thumbY + thumbHeight, EDGE_COLOR);
        }
    }

    private static int drawTab(GuiGraphics g, Minecraft mc, String name, boolean selected,
                               int x, int y, int prescribedWidth) {
        int width = prescribedWidth > 0 ? prescribedWidth : mc.font.width(name) + 7;
        g.fill(x, y, x + width, y + 11, selected ? 0xFF465D4E : 0xFF161F19);
        g.drawString(mc.font, name, x + 3, y + 2, selected ? 0xFFFFFFFF : 0xFFB6C3B8, false);
        return x + width + 3;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!isOverConsole(mc)) return;
        int max = maxScroll(mc);
        if (event.getDeltaY() > 0) scrollFromBottom = Math.min(max, scrollFromBottom + 3);
        if (event.getDeltaY() < 0) scrollFromBottom = Math.max(0, scrollFromBottom - 3);
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !isOverConsole(Minecraft.getInstance())) return;
        event.setCanceled(true);
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;
        Minecraft mc = Minecraft.getInstance();
        int x0 = left(), x1 = right(mc), y0 = top(mc);
        double x = mouseX(mc), y = mouseY(mc);
        if (y >= y0 + 3 && y < y0 + 14) {
            int tx = x0 + PADDING;
            int allWidth = mc.font.width("ALL") + 7;
            if (x >= tx && x < tx + allWidth) { filter = null; scrollFromBottom = 0; return; }
            tx += allWidth + 3;
            for (Category category : Category.values()) {
                int width = mc.font.width(category.name()) + 7;
                if (x >= tx && x < tx + width) { filter = category; scrollFromBottom = 0; return; }
                tx += width + 3;
            }
            int timeWidth = mc.font.width(showTimes ? "TIME ON" : "TIME OFF") + 7;
            if (tx + timeWidth + PADDING < x1 && x >= x1 - PADDING - timeWidth && x < x1 - PADDING) {
                showTimes = !showTimes;
                scrollFromBottom = 0;
            }
            return;
        }
        // Clicking the track moves to the corresponding place in the log.
        int trackX = x1 - PADDING - TRACK_WIDTH;
        int trackY = y0 + HEADER_HEIGHT + PADDING;
        int trackHeight = bottom(mc) - PADDING - trackY;
        if (x >= trackX && x < trackX + TRACK_WIDTH && y >= trackY && y < trackY + trackHeight) {
            int max = maxScroll(mc);
            scrollFromBottom = max - (int) (max * (y - trackY) / Math.max(1, trackHeight));
            scrollFromBottom = Math.max(0, Math.min(max, scrollFromBottom));
        }
    }
}
