package dev.cobbledeep.equipment;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

@AutoRegisterCapability
public final class AccessoryEquipment extends ItemStackHandler
{
    public static final int AMULET = 0;
    public static final int CLOAK = 1;
    public static final int LEFT_RING = 2;
    public static final int RIGHT_RING = 3;
    public static final int GAUNTLETS = 4;
    public static final int BELT = 5;
    public static final int SLOT_COUNT = 6;

    public AccessoryEquipment()
    {
        super(SLOT_COUNT);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        return !stack.isEmpty();
    }

    public void copyFrom(AccessoryEquipment other)
    {
        for (int slot = 0; slot < SLOT_COUNT; slot++)
        {
            setStackInSlot(slot, other.getStackInSlot(slot).copy());
        }
    }
}
