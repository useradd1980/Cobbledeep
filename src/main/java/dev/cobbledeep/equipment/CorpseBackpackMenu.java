package dev.cobbledeep.equipment;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Uses the vanilla three-row chest layout, but never shift-loots corpses into
 * Cobbledeep's quick weapon/item slots or its invisible vanilla storage slots.
 * Ordinary manual drag/drop is still available for equipping a chosen item.
 */
public final class CorpseBackpackMenu extends ChestMenu {
    private static final int CORPSE_SLOTS = 27;
    // Vanilla ChestMenu adds player indices 9..35 immediately after the corpse.
    // Thus the sixteen displayed backpack slots (indices 9..24) are 27..42.
    private static final int BACKPACK_MENU_START = CORPSE_SLOTS;
    private static final int BACKPACK_MENU_END = CORPSE_SLOTS
            + BackpackInventory.END_SLOT - BackpackInventory.FIRST_SLOT;

    public CorpseBackpackMenu(int containerId, Inventory inventory, Container corpseInventory) {
        super(MenuType.GENERIC_9x3, containerId, inventory, corpseInventory, 3);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int menuIndex) {
        if (menuIndex < 0 || menuIndex >= slots.size()) return ItemStack.EMPTY;
        if (menuIndex >= CORPSE_SLOTS) return super.quickMoveStack(player, menuIndex);

        Slot source = slots.get(menuIndex);
        if (!source.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = source.getItem();
        ItemStack original = sourceStack.copy();
        if (!moveItemStackTo(sourceStack, BACKPACK_MENU_START, BACKPACK_MENU_END, false))
            return ItemStack.EMPTY;

        if (sourceStack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        if (sourceStack.getCount() == original.getCount()) return ItemStack.EMPTY;
        source.onTake(player, sourceStack);
        return original;
    }
}
