package dev.cobbledeep.network;

import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Requests a corpse inventory by entity id; the server validates state and reach. */
public final class OpenCorpseLootPacket {
    private final int entityId;

    public OpenCorpseLootPacket(int entityId) {
        this.entityId = entityId;
    }

    public OpenCorpseLootPacket(FriendlyByteBuf buffer) {
        this(buffer.readVarInt());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !player.isAlive()) return;
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof GiantRatEntity rat) {
            rat.openCorpseLoot(player);
        }
    }
}
