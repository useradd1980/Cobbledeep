package dev.cobbledeep.combat;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class DndPlayerMechanics
{
    private DndPlayerMechanics() {}

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event)
    {
        if (!(event.player instanceof ServerPlayer player)) return;

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);
        player.getFoodData().setExhaustion(0.0F);
        removeLeggings(player);

        if (player.tickCount % 40 == 0 && player.getServer() != null)
        {
            player.level().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION)
                    .set(false, player.getServer());
        }

        player.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .filter(CharacterData::isCharacterCreated)
                .ifPresent(data -> applyCharacter(player, data, false));
    }

    public static void applyCharacter(ServerPlayer player, CharacterData data, boolean healToFull)
    {
        if (!data.isCharacterCreated() || data.getCharacterClass() == null) return;

        int maximumHitPoints = DndCombatStats.maximumHitPoints(data);
        AttributeInstance maximumHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maximumHealth == null) return;

        if (Math.abs(maximumHealth.getBaseValue() - maximumHitPoints) > 0.001D)
        {
            maximumHealth.setBaseValue(maximumHitPoints);
        }
        if (healToFull)
        {
            player.setHealth(maximumHitPoints);
        }
        else if (player.getHealth() > maximumHitPoints)
        {
            player.setHealth(maximumHitPoints);
        }
    }

    private static void removeLeggings(ServerPlayer player)
    {
        ItemStack leggings = player.getItemBySlot(EquipmentSlot.LEGS);
        if (leggings.isEmpty()) return;

        ItemStack returned = leggings.copy();
        player.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        if (!player.getInventory().add(returned))
        {
            player.drop(returned, false);
        }
    }
}
