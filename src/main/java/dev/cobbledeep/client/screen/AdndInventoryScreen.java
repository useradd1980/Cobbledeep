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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class AdndInventoryScreen extends AbstractContainerScreen<AdndInventoryMenu>
{
    private static final int PANEL = 0xF0101512;
    private static final int BORDER = 0xFF607766;
    private static final int HEADING = 0xFFD7C58A;
    private static final int LABEL = 0xFF9EAAA1;
    private static final int VALUE = 0xFFF0F2E8;
    private static final int INVENTORY_WIDTH = 392;
    private static final int COMBAT_PANEL_X = 404;

    private final Inventory playerInventory;

    public AdndInventoryScreen(AdndInventoryMenu menu, Inventory inventory, Component title)
    {
        super(menu, inventory, title);
        playerInventory = inventory;
        imageWidth = 600;
        imageHeight = 226;
        inventoryLabelY = 64;
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
        graphics.fill(leftPos + INVENTORY_WIDTH, topPos + 12,
                leftPos + INVENTORY_WIDTH + 1, topPos + imageHeight - 12, 0xFF455248);

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
        graphics.drawCenteredString(font, "COBBLEDEEP INVENTORY", INVENTORY_WIDTH / 2, 10, HEADING);
        graphics.drawString(font, "ACCESSORIES", 12, 57, HEADING);
        graphics.drawString(font, "EQUIPMENT", 148, 57, HEADING);
        graphics.drawString(font, "BACKPACK", 212, 57, HEADING);

        drawSlotLabel(graphics, "Amulet", 18, 76);
        drawSlotLabel(graphics, "Cloak", 94, 76);
        drawSlotLabel(graphics, "Ring L", 18, 112);
        drawSlotLabel(graphics, "Ring R", 94, 112);
        drawSlotLabel(graphics, "Gauntlets", 18, 148);
        drawSlotLabel(graphics, "Belt", 94, 148);

        drawSlotLabel(graphics, "Helmet", 160, 76);
        drawSlotLabel(graphics, "Armour", 160, 112);
        drawSlotLabel(graphics, "Boots", 160, 148);
        drawSlotLabel(graphics, "Off Hand", 160, 184);

        graphics.drawString(font, "Weapon sets 1–4  •  Ammo 5  •  Quick items 6–8", 212, 145, LABEL);
        renderCombatSummary(graphics);
    }

    private void renderCombatSummary(GuiGraphics graphics)
    {
        graphics.drawString(font, "LIVE COMBAT", COMBAT_PANEL_X, 57, HEADING);

        CharacterData data = playerInventory.player
                .getCapability(CharacterCapabilities.CHARACTER_DATA)
                .resolve().orElse(null);
        if (data == null || !data.isCharacterCreated())
        {
            graphics.drawString(font, "Character data unavailable", COMBAT_PANEL_X, 76, LABEL);
            return;
        }

        WeaponCombatProfile weapon = WeaponCombatStats.profile(playerInventory.player.getMainHandItem());
        int baseThac0 = CombatRules.thac0(data.getCharacterClass(), data.getLevel());
        int attackAdjustment = WeaponCombatStats.totalAttackAdjustment(data, weapon);

        int y = 76;
        drawCombatStat(graphics, "Armour Class", Integer.toString(DndCombatStats.armorClass(playerInventory.player)), y);
        drawCombatStat(graphics, "Base THAC0", Integer.toString(baseThac0), y += 15);
        drawCombatStat(graphics, "Effective THAC0", Integer.toString(baseThac0 - attackAdjustment), y += 15);
        drawCombatStat(graphics, "Weapon", weapon.displayName(), y += 15);
        drawCombatStat(graphics, "Proficiency", WeaponCombatStats.proficiencyText(data, weapon), y += 15);
        drawCombatStat(graphics, "Attack Adjustment", signed(attackAdjustment), y += 15);
        drawCombatStat(graphics, "Weapon Damage", WeaponCombatStats.damageText(data, weapon), y += 15);
    }

    private void drawCombatStat(GuiGraphics graphics, String label, String value, int y)
    {
        graphics.drawString(font, label, COMBAT_PANEL_X, y, LABEL);
        int valueX = imageWidth - 12 - font.width(value);
        graphics.drawString(font, value, Math.max(COMBAT_PANEL_X + 106, valueX), y, VALUE);
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
