package dev.cobbledeep.equipment;

import dev.cobbledeep.registry.ModMenus;
import net.minecraft.world.SimpleContainer;
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
    private static final int BACKPACK_END = 26;
    private static final int UTILITY_START = 26;
    private static final int UTILITY_END = 36;
    private static final int GROUND_END = 42;
    private final Player owner;
    private final SimpleContainer groundInventory = new SimpleContainer(6);

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
        addGroundSlots();
    }

    private void addAccessorySlots(ItemStackHandler accessories)
    {
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.AMULET, 196, 58));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.CLOAK, 122, 164));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.LEFT_RING, 112, 130));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.RIGHT_RING, 200, 130));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.GAUNTLETS, 136, 58));
        addSlot(new AccessorySlot(accessories, AccessoryEquipment.BELT, 178, 164));
    }

    private void addEquipmentSlots(Inventory inventory)
    {
        addSlot(new EquipmentItemSlot(inventory, 39, 164, 58, EquipmentSlot.HEAD, owner));
        addSlot(new EquipmentItemSlot(inventory, 38, 108, 58, EquipmentSlot.CHEST, owner));
        addSlot(new EquipmentItemSlot(inventory, 36, 150, 164, EquipmentSlot.FEET, owner));
        addSlot(new Slot(inventory, 40, 200, 100)
        {
            @Override public int getMaxStackSize() { return 1; }
        });
    }

    private void addBackpackSlots(Inventory inventory)
    {
        for (int row = 0; row < 2; row++)
        {
            for (int column = 0; column < 8; column++)
            {
                addSlot(new Slot(inventory, 9 + row * 8 + column,
                        91 + column * 18, 198 + row * 18));
            }
        }
    }

    private void addHotbarSlots(Inventory inventory)
    {
        for (int column = 0; column < 4; column++)
            addSlot(new Slot(inventory, column, 26 + column * 18, 112));
        for (int column = 0; column < 3; column++)
            addSlot(new Slot(inventory, 4 + column, 44 + column * 18, 82));
        for (int column = 0; column < 3; column++)
        {
            int inventoryIndex = column < 2 ? 7 + column : 25;
            addSlot(new Slot(inventory, inventoryIndex, 26 + column * 18, 142));
        }
    }

    private void addGroundSlots()
    {
        for (int row = 0; row < 2; row++)
            for (int column = 0; column < 3; column++)
                addSlot(new Slot(groundInventory, row * 3 + column,
                        254 + column * 18, 198 + row * 18));
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
            if (!moveItemStackTo(sourceStack, BACKPACK_START, UTILITY_END, true))
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
                if (!moveItemStackTo(sourceStack, UTILITY_START, UTILITY_END, false))
                    return ItemStack.EMPTY;
            }
            else if (index < UTILITY_END)
            {
                if (!moveItemStackTo(sourceStack, BACKPACK_START, BACKPACK_END, false))
                    return ItemStack.EMPTY;
            }
            else if (index < GROUND_END
                    && !moveItemStackTo(sourceStack, BACKPACK_START, BACKPACK_END, false))
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

    @Override
    public void removed(Player player)
    {
        super.removed(player);
        if (player.level().isClientSide) return;

        for (int slot = 0; slot < groundInventory.getContainerSize(); slot++)
        {
            ItemStack stack = groundInventory.removeItemNoUpdate(slot);
            if (!stack.isEmpty()) player.drop(stack, false);
        }
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
