package dev.cobbledeep.equipment;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class AccessoryEquipmentEvents
{
    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "accessory_equipment");

    private AccessoryEquipmentEvents() {}

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            AccessoryEquipmentProvider provider = new AccessoryEquipmentProvider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(AccessoryEquipmentCapabilities.EQUIPMENT).ifPresent(oldEquipment ->
                event.getEntity().getCapability(AccessoryEquipmentCapabilities.EQUIPMENT).ifPresent(
                        newEquipment -> newEquipment.copyFrom(oldEquipment)));
        event.getOriginal().invalidateCaps();
    }
}
