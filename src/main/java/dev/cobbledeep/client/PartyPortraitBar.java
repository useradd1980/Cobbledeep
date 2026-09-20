package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.client.screen.PortraitPickerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Compact, right-edge party strip. Only actual party members occupy slots. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class PartyPortraitBar {
    private static final int WIDTH = 50;
    private static final int SLOT_HEIGHT = 60;
    private static final int PADDING = 3;
    private static final int TOP = 20;
    private static final int HEADER_TOP = TOP - 17;
    private static final int GOLD = 0xFFDECB88;

    private PartyPortraitBar() { }

    private static boolean visible(Minecraft mc) {
        return TacticalCameraController.isEnabled() && mc.player != null
                && mc.level != null && mc.screen == null && !mc.options.hideGui;
    }

    private static int left(Minecraft mc) {
        // The panel touches the right edge, with no horizontal screen margin.
        return mc.getWindow().getGuiScaledWidth() - WIDTH;
    }

    /** All input handlers use the exact same bounds as the visible panel. */
    static boolean isOverBar(Minecraft mc) {
        if (!visible(mc)) return false;
        double screenWidth = mc.getWindow().getScreenWidth();
        double screenHeight = mc.getWindow().getScreenHeight();
        if (screenWidth <= 0.0 || screenHeight <= 0.0) return false;
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / screenWidth;
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / screenHeight;
        return mouseX >= left(mc) && mouseX < mc.getWindow().getGuiScaledWidth()
                && mouseY >= HEADER_TOP && mouseY < TOP + SLOT_HEIGHT;
    }

    /** Called by the Forge GUI layer registered in CobbledeepClientRenderSetup. */
    public static void onHud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        int x = left(mc);
        int right = mc.getWindow().getGuiScaledWidth();
        // Draw only the existing player portrait. Companions will add slots
        // when they actually join the party, rather than showing placeholders.
        graphics.fill(x, HEADER_TOP, right, TOP + SLOT_HEIGHT, 0xB80C1515);
        graphics.drawCenteredString(mc.font, "PARTY", x + WIDTH / 2, TOP - 13, GOLD);
        graphics.fill(x, TOP, right, TOP + SLOT_HEIGHT, GOLD);
        graphics.fill(x + 1, TOP + 2, right - 1, TOP + SLOT_HEIGHT - 2, 0xFF253B32);
        drawPlayer(mc, graphics, x, TOP);
    }

    private static void drawPlayer(Minecraft mc, GuiGraphics graphics, int x, int y) {
        int portraitWidth = WIDTH - PADDING * 2;
        graphics.fill(x + PADDING - 1, y + 4, x + WIDTH - PADDING + 1, y + 47, 0xFF0B1719);
        PartyPortraits.draw(mc, graphics, PartyPortraits.selected(mc),
                x + PADDING, y + 5, portraitWidth, 41);

        float max = Math.max(1.0F, mc.player.getMaxHealth());
        float fraction = Math.max(0.0F, Math.min(1.0F, mc.player.getHealth() / max));
        graphics.fill(x + PADDING, y + 47, x + WIDTH - PADDING, y + 51, 0xFF501F23);
        graphics.fill(x + PADDING, y + 47,
                x + PADDING + Math.round(portraitWidth * fraction), y + 51,
                fraction < 0.30F ? 0xFFFF5757 : 0xFF60DD77);
        graphics.drawCenteredString(mc.font, "EDIT", x + WIDTH / 2, y + 52, GOLD);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS
                || (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isWindowActive() || !isOverBar(mc)) return;

        // Consume clicks on the entire visible bar, including the title/frame.
        // They must not become movement, loot, combat, or camera-drag orders.
        event.setCanceled(true);
        double screenHeight = mc.getWindow().getScreenHeight();
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / screenHeight;
        if (mouseY < TOP) return; // The header isn't an action button.
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT || mouseY >= TOP + 51) {
            mc.setScreen(new PortraitPickerScreen());
            return;
        }

        // For now the player is the only party member, and remains selected.
        String name = mc.player.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .map(data -> data.isCharacterCreated() ? data.getName() : mc.player.getName().getString())
                .orElse(mc.player.getName().getString());
        mc.player.displayClientMessage(Component.literal("Selected: " + name), true);
    }
}
