package dev.cobbledeep.client.screen;

import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.combat.CombatRules;
import dev.cobbledeep.combat.DndCombatStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class CharacterSheetScreen extends Screen
{
    private static final int PANEL = 0xF0101512;
    private static final int BORDER = 0xFF607766;
    private static final int HEADING = 0xFFD7C58A;
    private static final int LABEL = 0xFF9EAAA1;
    private static final int VALUE = 0xFFF0F2E8;
    private static final int SLOT = 0xFF222923;
    private static final int SLOT_BORDER = 0xFF667468;

    private boolean inventoryPage;

    public CharacterSheetScreen()
    {
        this(false);
    }

    public CharacterSheetScreen(boolean inventoryPage)
    {
        super(Component.literal("Character Record"));
        this.inventoryPage = inventoryPage;
    }

    @Override
    protected void init()
    {
        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int bottom = panelBottom();
        int buttonWidth = Math.min(130, (panelWidth - 36) / 2);

        int tabWidth = 76;
        Button recordTab = Button.builder(Component.literal("Record"), button -> switchPage(false))
                .bounds(left + 12, panelTop() + 34, tabWidth, 20).build();
        recordTab.active = inventoryPage;
        addRenderableWidget(recordTab);
        Button inventoryTab = Button.builder(Component.literal("Inventory"), button -> switchPage(true))
                .bounds(left + 92, panelTop() + 34, tabWidth, 20).build();
        inventoryTab.active = !inventoryPage;
        addRenderableWidget(inventoryTab);

        Button levelUp = Button.builder(Component.literal("Level Up — Unavailable"), button -> {})
                .bounds(left + 12, bottom - 30, buttonWidth, 20)
                .build();
        levelUp.active = false;
        levelUp.visible = !inventoryPage;
        addRenderableWidget(levelUp);

        addRenderableWidget(Button.builder(Component.literal("Close"), button -> onClose())
                .bounds(left + panelWidth - buttonWidth - 12, bottom - 30, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
        {
            graphics.drawCenteredString(font, "No character is currently loaded.", width / 2,
                    panelTop() + 70, 0xFFFF7777);
            return;
        }

        CharacterData data = minecraft.player.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .resolve().orElse(null);
        if (data != null)
        {
            if (inventoryPage) renderInventory(graphics, data, mouseX, mouseY);
            else renderCharacter(graphics, data);
        }
        else
        {
            graphics.drawCenteredString(font, "Character data is unavailable.", width / 2,
                    panelTop() + 70, 0xFFFF7777);
        }
    }

    private void drawPanel(GuiGraphics graphics)
    {
        int left = panelLeft();
        int top = panelTop();
        int right = left + panelWidth();
        int bottom = panelBottom();
        graphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.drawCenteredString(font, "CHARACTER RECORD", width / 2, top + 12, HEADING);
        graphics.fill(left + 12, top + 28, right - 12, top + 29, 0xFF455248);
    }

    private void renderCharacter(GuiGraphics graphics, CharacterData data)
    {
        int left = panelLeft() + 16;
        int rightColumn = width / 2 + 12;
        int top = panelTop() + 62;

        if (!data.isCharacterCreated())
        {
            graphics.drawCenteredString(font, "No Cobbledeep character has been created.", width / 2,
                    top + 30, 0xFFFF7777);
            return;
        }

        String identity = data.getName() + "  •  Level " + data.getLevel() + " "
                + data.getCharacterClass().getDisplayName();
        graphics.drawCenteredString(font, identity, width / 2, top, VALUE);
        graphics.drawCenteredString(font,
                data.getRace().getDisplayName() + "  •  " + data.getAlignment().getDisplayName(),
                width / 2, top + 14, LABEL);

        int sectionY = top + 37;
        drawSection(graphics, "ABILITY SCORES", left, sectionY);
        int rowY = sectionY + 17;
        drawStat(graphics, "Strength", formatStrength(data), left, rowY);
        drawStat(graphics, "Dexterity", Integer.toString(data.getDexterity()), left, rowY + 15);
        drawStat(graphics, "Constitution", Integer.toString(data.getConstitution()), left, rowY + 30);
        drawStat(graphics, "Intelligence", Integer.toString(data.getIntelligence()), left, rowY + 45);
        drawStat(graphics, "Wisdom", Integer.toString(data.getWisdom()), left, rowY + 60);
        drawStat(graphics, "Charisma", Integer.toString(data.getCharisma()), left, rowY + 75);

        drawSection(graphics, "COMBAT", rightColumn, sectionY);
        int armorClass = DndCombatStats.armorClass(Minecraft.getInstance().player);
        int thac0 = CombatRules.thac0(data.getCharacterClass(), data.getLevel());
        int attack = CombatRules.strengthAttackAdjustment(data.getStrength(), data.getExceptionalStrength());
        int maximumHitPoints = DndCombatStats.maximumHitPoints(data);
        float health = Minecraft.getInstance().player.getHealth();

        drawStat(graphics, "Hit Points", formatHealth(health) + " / " + maximumHitPoints, rightColumn, rowY);
        drawStat(graphics, "Hit Die", "d" + CombatRules.hitDie(data.getCharacterClass()), rightColumn, rowY + 15);
        drawStat(graphics, "THAC0", Integer.toString(thac0), rightColumn, rowY + 30);
        drawStat(graphics, "Armour Class", Integer.toString(armorClass), rightColumn, rowY + 45);
        drawStat(graphics, "Melee Attack", signed(attack), rightColumn, rowY + 60);
        drawStat(graphics, "Experience", "Not yet tracked", rightColumn, rowY + 75);

        int noteY = Math.min(panelBottom() - 48, rowY + 105);
        graphics.drawCenteredString(font,
                "Level advancement will become available when experience rules are added.",
                width / 2, noteY, LABEL);
    }

    private void renderInventory(GuiGraphics graphics, CharacterData data, int mouseX, int mouseY)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (!data.isCharacterCreated() || minecraft.player == null) return;

        int left = panelLeft() + 16;
        int top = panelTop() + 62;
        graphics.drawString(font, "EQUIPMENT", left, top, HEADING);
        graphics.drawString(font, "BACKPACK", left + 220, top, HEADING);

        int equipmentY = top + 18;
        drawEquipmentSlot(graphics, "Helmet", minecraft.player.getItemBySlot(EquipmentSlot.HEAD), left, equipmentY, mouseX, mouseY);
        drawEquipmentSlot(graphics, "Armour", minecraft.player.getItemBySlot(EquipmentSlot.CHEST), left, equipmentY + 29, mouseX, mouseY);
        drawEquipmentSlot(graphics, "Boots", minecraft.player.getItemBySlot(EquipmentSlot.FEET), left, equipmentY + 58, mouseX, mouseY);

        drawEquipmentSlot(graphics, "Main Hand", minecraft.player.getMainHandItem(), left + 104, equipmentY, mouseX, mouseY);
        drawEquipmentSlot(graphics, "Off Hand", minecraft.player.getOffhandItem(), left + 104, equipmentY + 29, mouseX, mouseY);
        drawEquipmentSlot(graphics, "Ammunition", minecraft.player.getInventory().getItem(4), left + 104, equipmentY + 58, mouseX, mouseY);

        int accessoryY = equipmentY + 94;
        drawEmptySlot(graphics, "Amulet", left, accessoryY);
        drawEmptySlot(graphics, "Cloak", left + 104, accessoryY);
        drawEmptySlot(graphics, "Ring L", left, accessoryY + 29);
        drawEmptySlot(graphics, "Ring R", left + 104, accessoryY + 29);
        drawEmptySlot(graphics, "Gauntlets", left, accessoryY + 58);
        drawEmptySlot(graphics, "Belt", left + 104, accessoryY + 58);

        int backpackX = left + 220;
        int backpackY = equipmentY;
        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                int inventoryIndex = 9 + row * 9 + column;
                drawItemSlot(graphics, minecraft.player.getInventory().getItem(inventoryIndex),
                        backpackX + column * 19, backpackY + row * 19, mouseX, mouseY);
            }
        }

        graphics.drawString(font, "WEAPON SETS", backpackX, backpackY + 68, HEADING);
        for (int i = 0; i < 4; i++)
        {
            drawItemSlot(graphics, minecraft.player.getInventory().getItem(i),
                    backpackX + i * 38, backpackY + 83, mouseX, mouseY);
            graphics.drawCenteredString(font, Integer.toString(i + 1), backpackX + i * 38 + 9,
                    backpackY + 103, LABEL);
        }

        graphics.drawString(font, "QUICK ITEMS", backpackX, backpackY + 121, HEADING);
        for (int i = 0; i < 3; i++)
        {
            drawItemSlot(graphics, minecraft.player.getInventory().getItem(5 + i),
                    backpackX + i * 38, backpackY + 136, mouseX, mouseY);
        }

        graphics.drawString(font, "One armour suit replaces Minecraft leggings.", backpackX,
                backpackY + 166, LABEL);
        graphics.drawString(font, "Accessory storage and bonuses are the next inventory step.", backpackX,
                backpackY + 179, LABEL);
    }

    private void drawEquipmentSlot(GuiGraphics graphics, String label, ItemStack stack,
            int x, int y, int mouseX, int mouseY)
    {
        drawItemSlot(graphics, stack, x, y, mouseX, mouseY);
        graphics.drawString(font, label, x + 23, y + 5, LABEL);
    }

    private void drawEmptySlot(GuiGraphics graphics, String label, int x, int y)
    {
        drawSlotFrame(graphics, x, y);
        graphics.drawString(font, label, x + 23, y + 5, LABEL);
    }

    private void drawItemSlot(GuiGraphics graphics, ItemStack stack, int x, int y, int mouseX, int mouseY)
    {
        drawSlotFrame(graphics, x, y);
        if (!stack.isEmpty())
        {
            graphics.renderItem(stack, x + 2, y + 2);
            graphics.renderItemDecorations(font, stack, x + 2, y + 2);
            if (mouseX >= x && mouseX < x + 20 && mouseY >= y && mouseY < y + 20)
                graphics.renderTooltip(font, stack, mouseX, mouseY);
        }
    }

    private void drawSlotFrame(GuiGraphics graphics, int x, int y)
    {
        graphics.fill(x, y, x + 20, y + 20, SLOT_BORDER);
        graphics.fill(x + 1, y + 1, x + 19, y + 19, SLOT);
    }

    private void switchPage(boolean showInventory)
    {
        if (inventoryPage == showInventory) return;
        inventoryPage = showInventory;
        clearWidgets();
        init();
    }

    private void drawSection(GuiGraphics graphics, String title, int x, int y)
    {
        graphics.drawString(font, title, x, y, HEADING);
    }

    private void drawStat(GuiGraphics graphics, String label, String value, int x, int y)
    {
        graphics.drawString(font, label, x, y, LABEL);
        int valueX = x + Math.max(88, panelWidth() / 4 - 16);
        graphics.drawString(font, value, valueX, y, VALUE);
    }

    private String formatStrength(CharacterData data)
    {
        if (data.getStrength() != 18 || data.getExceptionalStrength() <= 0)
            return Integer.toString(data.getStrength());
        return "18/" + (data.getExceptionalStrength() >= 100
                ? "00" : String.format("%02d", data.getExceptionalStrength()));
    }

    private String formatHealth(float health)
    {
        return health == Math.floor(health) ? Integer.toString((int) health) : String.format("%.1f", health);
    }

    private String signed(int value)
    {
        return value > 0 ? "+" + value : Integer.toString(value);
    }

    private int panelWidth() { return Math.min(620, Math.max(500, width - 32)); }
    private int panelHeight() { return Math.min(330, Math.max(285, height - 32)); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return (height - panelHeight()) / 2; }
    private int panelBottom() { return panelTop() + panelHeight(); }

    @Override
    public boolean isPauseScreen()
    {
        return true;
    }
}
