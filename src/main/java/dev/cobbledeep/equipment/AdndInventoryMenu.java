package dev.cobbledeep.equipment;

import dev.cobbledeep.registry.ModMenus;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class AdndInventoryMenu extends AbstractContainerMenu
{
    private static final int ACCESSORY_END = 6;
    private static final int EQUIPMENT_END = 10;
    private static final int BACKPACK_START = 10;
    private static final int BACKPACK_END = 37;
    private static final int HOTBAR_START = 37;
    private static final int HOTBAR_END = 46;
    private final Player owner;

    public AdndInventoryMenu(int containerId, Inventory playerInventory)
    {
        super(ModMenus.ADND_INVENTORY.get(), containerId);
        owner = playerInventory.player;

        AccessoryEquipment accessories = playerInventory.player
                .getCapability(AccessoryEquipmentCapabilities.EQUIPMENT)
                .resolve().orElse(null);
        ItemStackHandler accessoryHandler = accessories == null
                ? new ItemStackHandler(AccessoryEquipment.SLOT_COUNT) : accessories;

        addAccessorySlots(accessoryHandler);
        addEquipmentSlots(playerInventory);
        addBackpackSlots(playerInventory);
        addHotbarSlots(playerInventory);
    }

    private void addAccessorySlots(ItemStackHandler accessories)
    {
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.AMULET, 196, 58));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.CLOAK, 122, 134));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.LEFT_RING, 59, 134));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.RIGHT_RING, 244, 134));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.GAUNTLETS, 136, 58));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.BELT, 178, 134));
    }

    private void addEquipmentSlots(Inventory inventory)
    {
        addSlot(new EquipmentItemSlot(inventory, 39, 164, 58, EquipmentSlot.HEAD, owner));
        addSlot(new EquipmentItemSlot(inventory, 38, 108, 58, EquipmentSlot.CHEST, owner));
        addSlot(new EquipmentItemSlot(inventory, 36, 150, 134, EquipmentSlot.FEET, owner));
        addSlot(new Slot(inventory, 40, 244, 92)
        {
            @Override public int getMaxStackSize() { return 1; }
        });
    }

    private void addBackpackSlots(Inventory inventory)
    {
        for (int row = 0; row < 3; row++)
        {
            for (int column = 0; column < 9; column++)
            {
                addSlot(new Slot(inventory, 9 + row * 9 + column,
                        82 + column * 18, 170 + row * 18));
            }
        }
    }

    private void addHotbarSlots(Inventory inventory)
    {
        for (int column = 0; column < 9; column++)
        {
            if (column < 4)
            {
                addSlot(new Slot(inventory, column, 34 + column * 18, 112));
            }
            else if (column < 7)
            {
                addSlot(new Slot(inventory, column, 43 + (column - 4) * 18, 86));
            }
            else
            {
                addSlot(new Slot(inventory, column, 145 + (column - 7) * 18, 228));
            }
        }
    }

    @Override
    public boolean stillValid(Player player)
    {
        return player.isAlive();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        Slot source = slots.get(index);
        if (!source.hasItem()) return ItemStack.EMPTY;

        ItemStack sourceStack = source.getItem();
        ItemStack original = sourceStack.copy();

        if (index < EQUIPMENT_END)
        {
            if (!moveItemStackTo(sourceStack, BACKPACK_START, HOTBAR_END, true))
                return ItemStack.EMPTY;
        }
        else
        {
            int equipmentIndex = equipmentMenuIndex(sourceStack);
            if (equipmentIndex >= ACCESSORY_END && !slots.get(equipmentIndex).hasItem())
            {
                if (!moveItemStackTo(sourceStack, equipmentIndex, equipmentIndex + 1, false))
                    return ItemStack.EMPTY;
            }
            else if (index < BACKPACK_END)
            {
                if (!moveItemStackTo(sourceStack, HOTBAR_START, HOTBAR_END, false))
                    return ItemStack.EMPTY;
            }
            else if (!moveItemStackTo(sourceStack, BACKPACK_START, BACKPACK_END, false))
            {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) source.set(ItemStack.EMPTY);
        else source.setChanged();
        if (sourceStack.getCount() == original.getCount()) return ItemStack.EMPTY;
        source.onTake(player, sourceStack);
        return original;
    }

    private int equipmentMenuIndex(ItemStack stack)
    {
        return switch (owner.getEquipmentSlotForItem(stack))
        {
            case HEAD -> 6;
            case CHEST -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    private static final class AccessorySlot extends SlotItemHandler
    {
        private AccessorySlot(ItemStackHandler handler, int index, int x, int y)
        {
            super(handler, index, x, y);
        }

        @Override public int getMaxStackSize() { return 1; }
    }

    private static final class EquipmentItemSlot extends Slot
    {
        private final EquipmentSlot equipmentSlot;
        private final Player owner;

        private EquipmentItemSlot(Inventory inventory, int index, int x, int y,
                EquipmentSlot equipmentSlot, Player owner)
        {
            super(inventory, index, x, y);
            this.equipmentSlot = equipmentSlot;
            this.owner = owner;
        }

        @Override public boolean mayPlace(ItemStack stack)
        {
            return owner.getEquipmentSlotForItem(stack) == equipmentSlot;
        }

        @Override public int getMaxStackSize() { return 1; }
    }
}
