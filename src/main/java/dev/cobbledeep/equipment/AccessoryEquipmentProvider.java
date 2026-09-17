package dev.cobbledeep.equipment;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AccessoryEquipmentProvider implements ICapabilityProvider, INBTSerializable<CompoundTag>
{
    private final AccessoryEquipment equipment = new AccessoryEquipment();
    private final LazyOptional<AccessoryEquipment> optional = LazyOptional.of(() -> equipment);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability,
            @Nullable Direction side)
    {
        return AccessoryEquipmentCapabilities.EQUIPMENT.orEmpty(capability, optional);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        return equipment.serializeNBT(provider);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag)
    {
        equipment.deserializeNBT(provider, tag);
    }

    public void invalidate()
    {
        optional.invalidate();
    }
}
