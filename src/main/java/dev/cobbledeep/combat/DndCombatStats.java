package dev.cobbledeep.combat;

import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public final class DndCombatStats
{
    private DndCombatStats() {}

    public static int maximumHitPoints(CharacterData data)
    {
        return CombatRules.levelOneHitPoints(data.getCharacterClass(), data.getConstitution());
    }

    public static int armorClass(LivingEntity target)
    {
        int armorClass = 10 - Math.round(target.getArmorValue() * 0.4F);
        if (target.getMainHandItem().is(Items.SHIELD) || target.getOffhandItem().is(Items.SHIELD))
        {
            armorClass--;
        }

        if (target instanceof Player player)
        {
            armorClass += player.getCapability(CharacterCapabilities.CHARACTER_DATA)
                    .filter(CharacterData::isCharacterCreated)
                    .map(data -> CombatRules.dexterityArmorClassAdjustment(data.getDexterity()))
                    .orElse(0);
        }
        return Math.max(-10, Math.min(10, armorClass));
    }
}
