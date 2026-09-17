package dev.cobbledeep.equipment;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class AccessoryEquipmentCapabilities
{
    public static final Capability<AccessoryEquipment> EQUIPMENT =
            CapabilityManager.get(new CapabilityToken<>() {});

    private AccessoryEquipmentCapabilities() {}
}
