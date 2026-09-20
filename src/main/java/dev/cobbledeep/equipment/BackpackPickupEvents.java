package dev.cobbledeep.equipment;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Replaces vanilla's hotbar-first ground pickup with server-side backpack-only insertion. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class BackpackPickupEvents {
    private static final Map<UUID, Long> LAST_FULL_MESSAGE = new HashMap<>();
    private static final long FULL_MESSAGE_INTERVAL = 100L;

    private BackpackPickupEvents() { }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemEntity groundItem = event.getItem();
        ItemStack original = groundItem.getItem().copy();
        if (original.isEmpty()) return;

        // Forge fires this event before vanilla checks whether a dropped item
        // has been reserved for a specific player. Preserve that protection.
        CompoundTag itemData = groundItem.saveWithoutId(new CompoundTag());
        if (itemData.hasUUID("Owner") && !player.getUUID().equals(itemData.getUUID("Owner"))) {
            event.setCanceled(true);
            return;
        }

        // Do not allow the default Inventory.add call: it fills the hotbar and
        // hidden inventory slots even if Cobbledeep's sixteen backpack slots
        // are full. The event is server-side and we handle pickup explicitly.
        event.setCanceled(true);
        ItemStack remainder = original.copy();
        int pickedUp = BackpackInventory.insert(player.getInventory(), remainder);
        if (pickedUp == 0) {
            showFullMessage(player);
            return; // Leave the entire item stack on the ground.
        }

        ItemStack pickedStack = original.copy();
        pickedStack.setCount(pickedUp);
        ForgeEventFactory.firePlayerItemPickupEvent(player, groundItem, pickedStack);
        player.take(groundItem, pickedUp);
        player.awardStat(Stats.ITEM_PICKED_UP.get(original.getItem()), pickedUp);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                0.9F + player.getRandom().nextFloat() * 0.2F);

        if (remainder.isEmpty()) groundItem.discard();
        else groundItem.setItem(remainder); // Only the portion that did not fit remains.
    }

    private static void showFullMessage(ServerPlayer player) {
        long now = player.level().getGameTime();
        long last = LAST_FULL_MESSAGE.getOrDefault(player.getUUID(), Long.MIN_VALUE);
        if (last != Long.MIN_VALUE && now >= last && now - last < FULL_MESSAGE_INTERVAL) return;
        LAST_FULL_MESSAGE.put(player.getUUID(), now);
        player.displayClientMessage(Component.literal("Backpack is full")
                .withStyle(ChatFormatting.RED), true);
    }
}
