package dev.cobbledeep.equipment;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Cobbledeep's visible backpack occupies player inventory indices 9 through 24.
 * Indices 0-8 and 25 are dedicated quick slots; 26-35 are not displayed in the
 * custom inventory and must never be used as overflow storage for new loot.
 */
public final class BackpackInventory {
    public static final int FIRST_SLOT = 9;
    public static final int END_SLOT = 25; // exclusive: sixteen visible backpack slots

    private BackpackInventory() { }

    /**
     * Moves as many items as possible into backpack slots alone. Mutates the
     * supplied stack to hold any remainder; never touches quick or hidden slots.
     * First fills compatible partial stacks, then uses empty backpack slots.
     */
    public static int insert(Inventory inventory, ItemStack remaining) {
        if (remaining.isEmpty()) return 0;
        int originalCount = remaining.getCount();

        for (int index = FIRST_SLOT; index < END_SLOT && !remaining.isEmpty(); index++) {
            ItemStack stored = inventory.getItem(index);
            if (stored.isEmpty() || !ItemStack.isSameItemSameComponents(stored, remaining)) continue;
            int capacity = Math.min(stored.getMaxStackSize(), inventory.getMaxStackSize())
                    - stored.getCount();
            if (capacity <= 0) continue;
            int moved = Math.min(capacity, remaining.getCount());
            stored.grow(moved);
            remaining.shrink(moved);
            inventory.setItem(index, stored);
        }

        for (int index = FIRST_SLOT; index < END_SLOT && !remaining.isEmpty(); index++) {
            if (!inventory.getItem(index).isEmpty()) continue;
            int moved = Math.min(remaining.getCount(),
                    Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()));
            if (moved <= 0) break;
            ItemStack stored = remaining.copy();
            stored.setCount(moved);
            inventory.setItem(index, stored);
            remaining.shrink(moved);
        }

        if (remaining.getCount() != originalCount) inventory.setChanged();
        return originalCount - remaining.getCount();
    }
}
