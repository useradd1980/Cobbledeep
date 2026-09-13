package dev.cobbledeep.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class CharacterData
{
    private boolean characterCreated = false;

    public boolean isCharacterCreated()
    {
        return characterCreated;
    }

    public void setCharacterCreated(boolean characterCreated)
    {
        this.characterCreated = characterCreated;
    }

    public void saveNBTData(CompoundTag tag)
    {
        tag.putBoolean("characterCreated", characterCreated);
    }

    public void loadNBTData(CompoundTag tag)
    {
        characterCreated = tag.getBoolean("characterCreated");
    }
}