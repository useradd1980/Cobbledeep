package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.screen.CharacterSheetScreen;
import dev.cobbledeep.client.screen.SnapshotLoadScreen;
import dev.cobbledeep.network.OpenAdndInventoryPacket;
import dev.cobbledeep.network.RPGNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** In-world counterpart of the navigation buttons in Cobbledeep's inventory. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class PartyActionBar {
    // 80% of the original 90-pixel sidebar, flush against the left edge.
    private static final int WIDTH = 72;
    private static final int BUTTON_X = 3;
    private static final int BUTTON_WIDTH = 66;
    private static final int BUTTON_TOP = 22;
    private static final int BUTTON_HEIGHT = 18;
    private static final int BUTTON_SPACING = 20;
    private static final int GOLD = 0xFFDECB88;
    private static final String[] ACTIONS = {
            "Return", "Map", "Journal", "Inventory", "Record",
            "Mages Spells", "Divine Spells", "Save", "Sleep"
    };

    private PartyActionBar() { }

    private static boolean visible(Minecraft mc) {
        return TacticalCameraController.isEnabled() && mc.player != null
                && mc.level != null && mc.screen == null && !mc.options.hideGui;
    }

    /** This is a UI hit region only; edge panning is handled independently. */
    static boolean isOverBar(Minecraft mc) {
        if (!visible(mc)) return false;
        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        if (windowWidth <= 0.0 || windowHeight <= 0.0) return false;
        double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / windowWidth;
        double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / windowHeight;
        return x >= 0.0 && x < WIDTH && y >= 0.0
                && y < mc.getWindow().getGuiScaledHeight();
    }

    /** Called by the existing tactical HUD GUI layer alongside the portrait bar. */
    public static void onHud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;

        int height = mc.getWindow().getGuiScaledHeight();
        graphics.fill(0, 0, WIDTH, height, 0xE0101718);
        graphics.fill(WIDTH - 1, 0, WIDTH, height, 0xFF718171);
        graphics.drawCenteredString(mc.font, "MENU", WIDTH / 2, 6, GOLD);

        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        double mouseX = windowWidth > 0.0
                ? mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / windowWidth : -1.0;
        double mouseY = windowHeight > 0.0
                ? mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / windowHeight : -1.0;

        for (int i = 0; i < ACTIONS.length; i++) {
            int y = BUTTON_TOP + i * BUTTON_SPACING;
            if (y + BUTTON_HEIGHT > height) break;
            boolean active = i == 3 || i == 4 || i == 7;
            boolean hovered = active && mouseX >= BUTTON_X && mouseX < BUTTON_X + BUTTON_WIDTH
                    && mouseY >= y && mouseY < y + BUTTON_HEIGHT;
            graphics.fill(BUTTON_X, y, BUTTON_X + BUTTON_WIDTH, y + BUTTON_HEIGHT,
                    active ? (hovered ? GOLD : 0xFF738475) : 0xFF38413D);
            graphics.fill(BUTTON_X + 1, y + 1,
                    BUTTON_X + BUTTON_WIDTH - 1, y + BUTTON_HEIGHT - 1,
                    active ? (hovered ? 0xFF3A5846 : 0xFF26382E) : 0xFF19221F);
            // Preserve the full menu labels, scaling only text that would
            // otherwise overflow the narrower button (e.g. Divine Spells).
            int textWidth = mc.font.width(ACTIONS[i]);
            float textScale = textWidth > BUTTON_WIDTH - 4
                    ? (BUTTON_WIDTH - 4.0F) / textWidth : 1.0F;
            graphics.pose().pushPose();
            graphics.pose().translate(WIDTH / 2.0F, y + 5.0F, 0.0F);
            graphics.pose().scale(textScale, textScale, 1.0F);
            graphics.drawCenteredString(mc.font, ACTIONS[i], 0, 0,
                    active ? (hovered ? 0xFFFFFFFF : 0xFFE3E8DB) : 0xFF7A8880);
            graphics.pose().popPose();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive() || !isOverBar(mc)) return;

        // A release over the sidebar still reaches the camera controller so a
        // right-drag begun in the world can be reset. Never issue world orders.
        if (event.getAction() != GLFW.GLFW_PRESS) return;
        event.setCanceled(true);
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        double x = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / windowWidth;
        double y = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / windowHeight;
        if (x < BUTTON_X || x >= BUTTON_X + BUTTON_WIDTH || y < BUTTON_TOP) return;
        int index = (int) ((y - BUTTON_TOP) / BUTTON_SPACING);
        if (index < 0 || index >= ACTIONS.length
                || y >= BUTTON_TOP + index * BUTTON_SPACING + BUTTON_HEIGHT) return;

        switch (index) {
            case 3 -> RPGNetwork.CHANNEL.send(
                    new OpenAdndInventoryPacket(), PacketDistributor.SERVER.noArg());
            case 4 -> mc.setScreen(new CharacterSheetScreen());
            case 7 -> mc.setScreen(new SnapshotLoadScreen(null, true));
            default -> { /* Unimplemented pages are displayed but not clickable. */ }
        }
    }
}
