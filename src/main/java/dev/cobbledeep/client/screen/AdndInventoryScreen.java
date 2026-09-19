package dev.cobbledeep.client.screen;

import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.combat.CombatRules;
import dev.cobbledeep.combat.DndCombatStats;
import dev.cobbledeep.combat.WeaponCombatProfile;
import dev.cobbledeep.combat.WeaponCombatStats;
import dev.cobbledeep.equipment.AdndInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

public final class AdndInventoryScreen extends AbstractContainerScreen<AdndInventoryMenu>
{
    private static final float UI_SCALE = 0.8F;
    private static final int PANEL = 0xF0101512;
    private static final int BORDER = 0xFF607766;
    private static final int HEADING = 0xFFD7C58A;
    private static final int LABEL = 0xFF9EAAA1;
    private static final int VALUE = 0xFFF0F2E8;

    private final Inventory playerInventory;

    public AdndInventoryScreen(AdndInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        playerInventory = inventory;
        imageWidth = 326;
        imageHeight = 238;
        inventoryLabelY = 188;
    }

    @Override
    protected void init()
    {
        super.init();
        for (Button button : ScreenNavigationColumn.create(
                leftPos - ScreenNavigationColumn.WIDTH - ScreenNavigationColumn.GAP, topPos + 20,
                ScreenNavigationColumn.Page.INVENTORY,
                this::onClose,
                null,
                this::openRecord,
                this::openSaveManager))
            addRenderableWidget(button);
    }

    private void openRecord()
    {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.player.closeContainer();
        minecraft.setScreen(new CharacterSheetScreen());
    }

    private void openSaveManager()
    {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.player.closeContainer();
        minecraft.setScreen(new SnapshotLoadScreen(null, true));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, BORDER);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 86, topPos + 50, leftPos + 248, topPos + 183, 0x80222923);
        graphics.fill(leftPos + 8, topPos + 185, leftPos + imageWidth - 8, topPos + 186, 0xFF455248);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                leftPos + 112, topPos + 86, leftPos + 200, topPos + 154,
                24, 0.0625F, mouseX, mouseY, playerInventory.player);

        for (var slot : menu.slots)
        {
            graphics.fill(leftPos + slot.x, topPos + slot.y,
                    leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF667468);
            graphics.fill(leftPos + slot.x + 1, topPos + slot.y + 1,
                    leftPos + slot.x + 15, topPos + slot.y + 15, 0xFF222923);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawCenteredString(font, "COBBLEDEEP INVENTORY", imageWidth / 2, 10, HEADING);
        graphics.drawString(font, "ARM", 108, 56, LABEL);
        graphics.drawString(font, "GLV", 136, 56, LABEL);
        graphics.drawString(font, "HELM", 161, 56, LABEL);
        graphics.drawString(font, "NECK", 194, 56, LABEL);

        graphics.drawString(font, "QUIVER", 30, 72, HEADING);
        graphics.drawString(font, "WEAPONS", 12, 102, HEADING);
        graphics.drawString(font, "QUICK ITEMS", 12, 132, HEADING);
        graphics.drawString(font, "L RING", 92, 120, LABEL);

        graphics.drawString(font, "OFF HAND", 194, 90, LABEL);
        graphics.drawString(font, "R RING", 195, 120, LABEL);
        graphics.drawString(font, "CLK", 122, 154, LABEL);
        graphics.drawString(font, "BOOT", 147, 154, LABEL);
        graphics.drawString(font, "BELT", 176, 154, LABEL);

        graphics.drawCenteredString(font, "BACKPACK", imageWidth / 2, 188, HEADING);
        graphics.drawString(font, "GROUND", 254, 188, HEADING);
        renderCombatSummary(graphics);
    }

    private void renderCombatSummary(GuiGraphics graphics)
    {
        CharacterData data = playerInventory.player
                .getCapability(CharacterCapabilities.CHARACTER_DATA)
                .resolve().orElse(null);
        if (data == null || !data.isCharacterCreated())
        {
            graphics.drawString(font, "GOLD " + carriedGold(), 254, 53, HEADING);
            graphics.drawString(font, "AC --", 254, 65, LABEL);
            graphics.drawString(font, "THAC0 --", 254, 77, LABEL);
            return;
        }

        WeaponCombatProfile weapon = WeaponCombatStats.profile(playerInventory.player.getMainHandItem());
        int baseThac0 = CombatRules.thac0(data.getCharacterClass(), data.getLevel());
        int attackAdjustment = WeaponCombatStats.totalAttackAdjustment(data, weapon);

        graphics.drawString(font, "GOLD " + carriedGold(), 254, 53, HEADING);
        graphics.drawString(font,
                "AC " + DndCombatStats.armorClass(playerInventory.player), 254, 65, VALUE);
        graphics.drawString(font,
                "THAC0 " + (baseThac0 - attackAdjustment), 254, 77, VALUE);
    }

    private int carriedGold()
    {
        int ingots = playerInventory.countItem(Items.GOLD_INGOT);
        int blocks = playerInventory.countItem(Items.GOLD_BLOCK);
        int nuggets = playerInventory.countItem(Items.GOLD_NUGGET);
        return ingots + blocks * 9 + nuggets / 9;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        // The vanilla blur samples the previous framebuffer. Once this screen
        // is scaled that creates a visible, unscaled ghost of the panel and
        // paper doll behind the current frame. Dim the live world directly.
        graphics.fill(0, 0, width, height, 0xA0000000);
        double scaledMouseX = unscaledX(mouseX);
        double scaledMouseY = unscaledY(mouseY);
        graphics.pose().pushPose();
        graphics.pose().translate(width / 2.0F, height / 2.0F, 0.0F);
        graphics.pose().scale(UI_SCALE, UI_SCALE, 1.0F);
        graphics.pose().translate(-width / 2.0F, -height / 2.0F, 0.0F);
        super.render(graphics, (int) scaledMouseX, (int) scaledMouseY, partialTick);
        renderTooltip(graphics, (int) scaledMouseX, (int) scaledMouseY);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        return super.mouseClicked(unscaledX(mouseX), unscaledY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        return super.mouseReleased(unscaledX(mouseX), unscaledY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button,
            double dragX, double dragY)
    {
        return super.mouseDragged(unscaledX(mouseX), unscaledY(mouseY), button,
                dragX / UI_SCALE, dragY / UI_SCALE);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        return super.mouseScrolled(unscaledX(mouseX), unscaledY(mouseY), scrollX, scrollY);
    }

    private double unscaledX(double mouseX)
    {
        return width / 2.0 + (mouseX - width / 2.0) / UI_SCALE;
    }

    private double unscaledY(double mouseY)
    {
        return height / 2.0 + (mouseY - height / 2.0) / UI_SCALE;
    }
}
