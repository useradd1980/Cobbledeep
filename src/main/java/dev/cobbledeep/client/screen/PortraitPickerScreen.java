package dev.cobbledeep.client.screen;

import dev.cobbledeep.client.PartyPortraits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Choose a portrait for the currently controlled character, without altering their world model. */
public final class PortraitPickerScreen extends Screen {
    private static final int PER_PAGE = 12;
    private final int requestedPage;
    private List<String> portraits = List.of();
    private int page;

    public PortraitPickerScreen() { this(0); }

    private PortraitPickerScreen(int requestedPage) {
        super(Component.literal("Character Portrait"));
        this.requestedPage = requestedPage;
    }

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        PartyPortraits.refresh(mc);
        portraits = PartyPortraits.available(mc);
        page = Math.min(requestedPage, Math.max(0, (portraits.size() - 1) / PER_PAGE));

        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int top = panelTop();
        int buttonWidth = (panelWidth - 36) / 3;
        int startX = left + 12;
        int buttonTop = top + 99;

        addRenderableWidget(Button.builder(Component.literal("Use player skin"), button -> {
                    PartyPortraits.choose(mc, PartyPortraits.DEFAULT);
                    onClose();
                }).bounds(left + panelWidth - 137, top + 45, 124, 20).build());

        for (int i = page * PER_PAGE; i < Math.min(portraits.size(), (page + 1) * PER_PAGE); i++) {
            final String portrait = portraits.get(i);
            int relative = i - page * PER_PAGE;
            int x = startX + (relative % 3) * (buttonWidth + 6);
            int y = buttonTop + (relative / 3) * 25;
            String label = portrait.length() > 15 ? portrait.substring(0, 12) + "..." : portrait;
            addRenderableWidget(Button.builder(Component.literal(label), button -> {
                        PartyPortraits.choose(mc, portrait);
                        onClose();
                    }).bounds(x, y, buttonWidth, 20).build());
        }

        addRenderableWidget(Button.builder(Component.literal("<"), button ->
                        mc.setScreen(new PortraitPickerScreen(page - 1)))
                .bounds(left + 12, top + 202, 28, 20).build()).active = page > 0;
        addRenderableWidget(Button.builder(Component.literal(">"), button ->
                        mc.setScreen(new PortraitPickerScreen(page + 1)))
                .bounds(left + panelWidth - 40, top + 202, 28, 20).build())
                .active = (page + 1) * PER_PAGE < portraits.size();
        addRenderableWidget(Button.builder(Component.literal("Refresh PNGs"), button ->
                        mc.setScreen(new PortraitPickerScreen(page)))
                .bounds(left + 12, top + 229, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(left + panelWidth - 93, top + 229, 81, 20).build());
    }

    /** Screen.render() calls renderBackground() again through super.render().
     * Keeping that method empty avoids blurring the panel and labels already
     * drawn by our render() method, while preserving vanilla widget rendering.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This screen draws its own opaque backdrop in render().
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF101716);
        int left = (width - panelWidth()) / 2;
        int top = panelTop();
        graphics.fill(left - 2, top - 2, left + panelWidth() + 2, top + 259, 0xFF66755D);
        graphics.fill(left, top, left + panelWidth(), top + 257, 0xFF141918);
        graphics.drawCenteredString(font, "CHARACTER PORTRAIT", width / 2, top + 10, 0xFFE7D6A4);
        graphics.fill(left + 12, top + 30, left + panelWidth() - 12, top + 31, 0xFF536454);

        Minecraft mc = Minecraft.getInstance();
        graphics.fill(left + 12, top + 38, left + 66, top + 94, 0xFFB9A16F);
        graphics.fill(left + 14, top + 40, left + 64, top + 92, 0xFF18221E);
        PartyPortraits.draw(mc, graphics, PartyPortraits.selected(mc), left + 16, top + 42, 46, 48);
        graphics.drawString(font, "Current: " + abbreviated(PartyPortraits.selected(mc), 22),
                left + 73, top + 73, 0xFFE9E9DA, false);
        graphics.drawString(font, "Add PNG files to: cobbledeep/portraits", left + 12,
                top + 183, 0xFFCCD5C4, false);
        graphics.drawString(font, "Square images work best (max 2 MB, 1024 px)", left + 12,
                top + 193, 0xFF9DA99B, false);
        graphics.drawCenteredString(font, "Page " + (page + 1) + " / "
                + Math.max(1, (portraits.size() + PER_PAGE - 1) / PER_PAGE),
                width / 2, top + 207, 0xFFDDD7BE);
        if (portraits.isEmpty()) graphics.drawCenteredString(font,
                "No custom PNGs found. Use Refresh after adding one.",
                width / 2, top + 131, 0xFFE1C68A);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String abbreviated(String value, int limit) {
        return value.length() <= limit ? value : value.substring(0, limit - 3) + "...";
    }

    private int panelWidth() { return Math.min(368, width - 24); }
    private int panelTop() { return Math.max(8, (height - 259) / 2); }

    @Override
    public void onClose() { Minecraft.getInstance().setScreen(null); }

    @Override
    public boolean isPauseScreen() { return false; }
}
