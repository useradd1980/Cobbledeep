package dev.cobbledeep.character;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class CharacterCapabilities
{
    public static final Capability<CharacterData> CHARACTER_DATA =
            CapabilityManager.get(new CapabilityToken<>() {});
}