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

/** Right-hand party strip. The player is slot 1; reserved slots remain non-interactive until companions exist. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class PartyPortraitBar {
    private static final int WIDTH = 66;
    private static final int SLOT_HEIGHT = 60;
    private static final int SLOT_GAP = 4;
    private static final int PADDING = 5;
    private static final int TOP = 23;
    private static final int GOLD = 0xFFDECB88;

    private PartyPortraitBar() { }

    private static boolean visible(Minecraft mc) {
        return TacticalCameraController.isEnabled() && mc.player != null
                && mc.level != null && mc.screen == null && !mc.options.hideGui;
    }

    private static int left(Minecraft mc) {
        return mc.getWindow().getGuiScaledWidth() - WIDTH - 6;
    }

    private static int slots(Minecraft mc) {
        return Math.min(6, Math.max(1,
                (mc.getWindow().getGuiScaledHeight() - TOP - 7) / (SLOT_HEIGHT + SLOT_GAP)));
    }

    /** Called by the Forge GUI layer registered in CobbledeepClientRenderSetup. */
    public static void onHud(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc)) return;
        int x = left(mc);
        int count = slots(mc);
        int bottom = TOP + count * (SLOT_HEIGHT + SLOT_GAP);
        graphics.fill(x - 3, TOP - 19, x + WIDTH + 3, bottom, 0xB80C1515);
        graphics.drawCenteredString(mc.font, "PARTY", x + WIDTH / 2, TOP - 14, GOLD);

        for (int i = 0; i < count; i++) {
            int y = TOP + i * (SLOT_HEIGHT + SLOT_GAP);
            graphics.fill(x, y, x + WIDTH, y + SLOT_HEIGHT,
                    i == 0 ? GOLD : 0xFF46544F);
            graphics.fill(x + 2, y + 2, x + WIDTH - 2, y + SLOT_HEIGHT - 2,
                    i == 0 ? 0xFF253B32 : 0xFF172020);
            if (i == 0) drawPlayer(mc, graphics, x, y);
            else graphics.drawCenteredString(mc.font, "—", x + WIDTH / 2,
                    y + SLOT_HEIGHT / 2 - 4, 0xFF50625A);
        }
    }

    private static void drawPlayer(Minecraft mc, GuiGraphics graphics, int x, int y) {
        graphics.fill(x + PADDING - 1, y + 4, x + WIDTH - PADDING + 1, y + 49, 0xFF0B1719);
        PartyPortraits.draw(mc, graphics, PartyPortraits.selected(mc),
                x + PADDING, y + 5, WIDTH - PADDING * 2, 42);

        float max = Math.max(1.0F, mc.player.getMaxHealth());
        float fraction = Math.max(0.0F, Math.min(1.0F, mc.player.getHealth() / max));
        graphics.fill(x + PADDING, y + 47, x + WIDTH - PADDING, y + 51, 0xFF501F23);
        graphics.fill(x + PADDING, y + 47,
                x + PADDING + Math.round((WIDTH - PADDING * 2) * fraction), y + 51,
                fraction < 0.30F ? 0xFFFF5757 : 0xFF60DD77);
        graphics.drawCenteredString(mc.font, "EDIT", x + WIDTH / 2, y + 52, GOLD);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != GLFW.GLFW_PRESS
                || (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT)) return;
        Minecraft mc = Minecraft.getInstance();
        if (!visible(mc) || !mc.isWindowActive()) return;
        double screenWidth = mc.getWindow().getScreenWidth();
        double screenHeight = mc.getWindow().getScreenHeight();
        if (screenWidth <= 0 || screenHeight <= 0) return;
        double mouseX = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / screenWidth;
        double mouseY = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / screenHeight;
        int x = left(mc);
        int y = TOP;
        if (mouseX < x || mouseX >= x + WIDTH || mouseY < y || mouseY >= y + SLOT_HEIGHT) {
            return; // Empty party slots do not intercept world interaction.
        }
        event.setCanceled(true);
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT || mouseY >= y + 51) {
            mc.setScreen(new PortraitPickerScreen());
            return;
        }
        // The player is the only controllable party member so far. Keeping
        // selection separate from the portrait picker allows more party members later.
        String name = mc.player.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .map(data -> data.isCharacterCreated() ? data.getName() : mc.player.getName().getString())
                .orElse(mc.player.getName().getString());
        mc.player.displayClientMessage(Component.literal("Selected: " + name), true);
    }
}
