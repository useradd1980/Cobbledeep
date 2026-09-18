package dev.cobbledeep.client.screen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

final class ScreenNavigationColumn
{
    static final int WIDTH = 82;
    static final int GAP = 4;
    private static final int HEIGHT = 18;
    private static final int SPACING = 20;

    enum Page
    {
        INVENTORY,
        RECORD
    }

    private ScreenNavigationColumn() {}

    static List<Button> create(int x, int y, Page currentPage,
            Runnable returnAction, Runnable inventoryAction, Runnable recordAction,
            Runnable saveAction)
    {
        List<Button> buttons = new ArrayList<>();
        add(buttons, x, y, "Return", null, returnAction, currentPage);
        add(buttons, x, y + SPACING, "Map", null, null, currentPage);
        add(buttons, x, y + SPACING * 2, "Journal", null, null, currentPage);
        add(buttons, x, y + SPACING * 3, "Inventory", Page.INVENTORY, inventoryAction, currentPage);
        add(buttons, x, y + SPACING * 4, "Record", Page.RECORD, recordAction, currentPage);
        add(buttons, x, y + SPACING * 5, "Mages Spells", null, null, currentPage);
        add(buttons, x, y + SPACING * 6, "Divine Spells", null, null, currentPage);
        add(buttons, x, y + SPACING * 7, "Save", null, saveAction, currentPage);
        add(buttons, x, y + SPACING * 8, "Sleep", null, null, currentPage);
        return buttons;
    }

    private static void add(List<Button> buttons, int x, int y, String label, Page page,
            Runnable action, Page currentPage)
    {
        boolean selected = page == currentPage;
        String displayedLabel = selected ? "> " + label + " <" : label;
        Button button = Button.builder(Component.literal(displayedLabel), ignored ->
        {
            if (action != null) action.run();
        }).bounds(x, y, WIDTH, HEIGHT).build();
        button.active = action != null && !selected;
        buttons.add(button);
    }
}
