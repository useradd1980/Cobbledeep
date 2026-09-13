package dev.cobbledeep.character;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CharacterDataProvider
        implements ICapabilityProvider, INBTSerializable<CompoundTag>
{
    private final CharacterData data = new CharacterData();

    private final LazyOptional<CharacterData> optional =
            LazyOptional.of(() -> data);

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(
            @NotNull Capability<T> cap,
            @Nullable Direction side)
    {
        return CharacterCapabilities.CHARACTER_DATA.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider)
    {
        CompoundTag tag = new CompoundTag();
        data.saveNBTData(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(
            HolderLookup.Provider provider,
            CompoundTag tag)
    {
        data.loadNBTData(tag);
    }

    public void invalidate()
    {
        optional.invalidate();
    }
}