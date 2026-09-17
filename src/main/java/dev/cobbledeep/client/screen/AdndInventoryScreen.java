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

public final class AdndInventoryScreen extends AbstractContainerScreen<AdndInventoryMenu>
{
    private static final int PANEL = 0xF0101512;
    private static final int BORDER = 0xFF607766;
    private static final int HEADING = 0xFFD7C58A;
    private static final int LABEL = 0xFF9EAAA1;
    private static final int VALUE = 0xFFF0F2E8;
    private static final int COMBAT_PANEL_X = 232;

    private final Inventory playerInventory;

    public AdndInventoryScreen(AdndInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        playerInventory = inventory;
        imageWidth = 382;
        imageHeight = 250;
        inventoryLabelY = 156;
    }

    @Override
    protected void init()
    {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Record"), button ->
        {
            if (minecraft != null && minecraft.player != null)
            {
                minecraft.player.closeContainer();
                minecraft.setScreen(new CharacterSheetScreen());
            }
        }).bounds(leftPos + 12, topPos + 28, 72, 20).build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY)
    {
        graphics.fill(leftPos - 1, topPos - 1, leftPos + imageWidth + 1, topPos + imageHeight + 1, BORDER);
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, PANEL);
        graphics.fill(leftPos + 70, topPos + 50, leftPos + 216, topPos + 151, 0x80222923);
        graphics.fill(leftPos + 220, topPos + 50, leftPos + 221, topPos + 151, 0xFF455248);
        graphics.fill(leftPos + 8, topPos + 153, leftPos + imageWidth - 8, topPos + 154, 0xFF455248);

        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                leftPos + 108, topPos + 66, leftPos + 184, topPos + 146,
                30, 0.0625F, mouseX, mouseY, playerInventory.player);

        for (var slot : menu.slots)
        {
            graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1,
                    leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF667468);
            graphics.fill(leftPos + slot.x, topPos + slot.y,
                    leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF222923);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.drawCenteredString(font, "COBBLEDEEP INVENTORY", imageWidth / 2, 10, HEADING);
        graphics.drawString(font, "ACCESSORIES", 12, 43, HEADING);
        graphics.drawString(font, "EQUIPMENT", 88, 43, HEADING);

        drawSlotLabel(graphics, "Amul", 12, 62);
        drawSlotLabel(graphics, "Cloak", 40, 62);
        drawSlotLabel(graphics, "Ring L", 12, 92);
        drawSlotLabel(graphics, "Ring R", 40, 92);
        drawSlotLabel(graphics, "Glove", 12, 122);
        drawSlotLabel(graphics, "Belt", 40, 122);

        drawSlotLabel(graphics, "Helm", 116, 56);
        graphics.drawString(font, "Armour", 76, 76, LABEL);
        graphics.drawString(font, "Off hand", 178, 76, LABEL);
        graphics.drawCenteredString(font, "Boots", 125, 116, LABEL);

        graphics.drawString(font, "BACKPACK", 110, 156, HEADING);
        graphics.drawString(font, "Selected weapon  •  Ammunition  •  Quick items", 110, 216, LABEL);
        renderCombatSummary(graphics);
    }

    private void renderCombatSummary(GuiGraphics graphics)
    {
        graphics.drawString(font, "LIVE COMBAT", COMBAT_PANEL_X, 43, HEADING);

        CharacterData data = playerInventory.player
                .getCapability(CharacterCapabilities.CHARACTER_DATA)
                .resolve().orElse(null);
        if (data == null || !data.isCharacterCreated())
        {
            graphics.drawString(font, "Character data unavailable", COMBAT_PANEL_X, 60, LABEL);
            return;
        }

        WeaponCombatProfile weapon = WeaponCombatStats.profile(playerInventory.player.getMainHandItem());
        int baseThac0 = CombatRules.thac0(data.getCharacterClass(), data.getLevel());
        int attackAdjustment = WeaponCombatStats.totalAttackAdjustment(data, weapon);

        int y = 60;
        drawCombatStat(graphics, "Armour Class", Integer.toString(DndCombatStats.armorClass(playerInventory.player)), y);
        drawCombatStat(graphics, "Base THAC0", Integer.toString(baseThac0), y += 13);
        drawCombatStat(graphics, "Effective THAC0", Integer.toString(baseThac0 - attackAdjustment), y += 13);
        drawCombatStat(graphics, "Attack", signed(attackAdjustment), y += 13);
        drawCombatStat(graphics, "Damage", WeaponCombatStats.damageText(data, weapon), y += 13);
        drawCombatStat(graphics, "Weapon", weapon.displayName(), y += 13);
        drawCombatStat(graphics, "Proficiency", WeaponCombatStats.proficiencyText(data, weapon), y += 13);
    }

    private void drawCombatStat(GuiGraphics graphics, String label, String value, int y)
    {
        graphics.drawString(font, label, COMBAT_PANEL_X, y, LABEL);
        int maximumWidth = imageWidth - COMBAT_PANEL_X - 12;
        String visibleValue = font.plainSubstrByWidth(value, maximumWidth);
        int valueX = imageWidth - 12 - font.width(visibleValue);
        graphics.drawString(font, visibleValue, Math.max(COMBAT_PANEL_X + 68, valueX), y, VALUE);
    }

    private String signed(int value)
    {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private void drawSlotLabel(GuiGraphics graphics, String label, int x, int y)
    {
        graphics.drawString(font, label, x, y - 10, LABEL);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
