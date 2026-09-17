package dev.cobbledeep.combat;

import java.util.concurrent.ThreadLocalRandom;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class DndCombatEvents
{
    private DndCombatEvents() {}

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingAttack(LivingAttackEvent event)
    {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;

        attacker.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .filter(CharacterData::isCharacterCreated)
                .ifPresent(data -> resolveAttack(event, attacker, data));
    }

    private static void resolveAttack(LivingAttackEvent event, ServerPlayer attacker, CharacterData data)
    {
        int roll = ThreadLocalRandom.current().nextInt(1, 21);
        int thac0 = CombatRules.thac0(data.getCharacterClass(), data.getLevel());
        int armorClass = DndCombatStats.armorClass(event.getEntity());
        WeaponCombatProfile weapon = WeaponCombatStats.profile(attacker.getMainHandItem());
        int attackAdjustment = WeaponCombatStats.totalAttackAdjustment(data, weapon);
        int target = thac0 - armorClass;
        int total = roll + attackAdjustment;
        boolean hit = CombatRules.hits(roll, thac0, armorClass, attackAdjustment);

        String adjustment = attackAdjustment >= 0 ? "+" + attackAdjustment : Integer.toString(attackAdjustment);
        Component message = Component.literal(
                "Attack: d20 " + roll + " " + adjustment + " = " + total + " vs " + target
                        + "  (" + weapon.displayName() + ", THAC0 " + thac0
                        + ", AC " + armorClass + ")  "
                        + (hit ? "HIT" : "MISS"))
                .withStyle(hit ? ChatFormatting.GREEN : ChatFormatting.RED);
        attacker.displayClientMessage(message, true);

        if (!hit) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLivingHurt(LivingHurtEvent event)
    {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (event.getSource().getDirectEntity() != attacker) return;

        attacker.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .filter(CharacterData::isCharacterCreated)
                .ifPresent(data ->
                {
                    WeaponCombatProfile weapon = WeaponCombatStats.profile(attacker.getMainHandItem());
                    event.setAmount(WeaponCombatStats.rollDamage(data, weapon));
                });
    }
}
