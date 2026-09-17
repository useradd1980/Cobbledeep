package dev.cobbledeep.network;

import dev.cobbledeep.equipment.AdndInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.event.network.CustomPayloadEvent;

public final class OpenAdndInventoryPacket
{
    public OpenAdndInventoryPacket() {}
    public OpenAdndInventoryPacket(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context)
    {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, owner) -> new AdndInventoryMenu(containerId, inventory),
                Component.literal("Cobbledeep Inventory")));
    }
}
