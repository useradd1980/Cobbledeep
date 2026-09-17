package dev.cobbledeep.combat;

import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.character.WeaponProficiency;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;

public final class WeaponCombatStats
{
    private static final WeaponCombatProfile UNARMED =
            new WeaponCombatProfile("Unarmed", null, 1, 2, 0);

    private WeaponCombatStats() {}

    public static WeaponCombatProfile profile(ItemStack stack)
    {
        if (stack.isEmpty()) return UNARMED;
        String name = stack.getHoverName().getString();

        if (stack.getItem() instanceof SwordItem)
            return new WeaponCombatProfile(name, WeaponProficiency.LONG_SWORD, 1, 8, 0);
        if (stack.getItem() instanceof AxeItem)
            return new WeaponCombatProfile(name, WeaponProficiency.AXE, 1, 8, 0);
        if (stack.is(Items.TRIDENT))
            return new WeaponCombatProfile(name, WeaponProficiency.SPEAR, 1, 6, 0);
        if (stack.is(Items.MACE))
            return new WeaponCombatProfile(name, WeaponProficiency.MACE, 1, 6, 1);
        if (stack.getItem() instanceof CrossbowItem)
            return new WeaponCombatProfile(name, WeaponProficiency.CROSSBOW, 1, 8, 0);
        if (stack.getItem() instanceof BowItem)
            return new WeaponCombatProfile(name, WeaponProficiency.LONGBOW, 1, 8, 0);
        return new WeaponCombatProfile(name, null, 1, 4, 0);
    }

    public static int proficiencyRank(CharacterData data, WeaponCombatProfile profile)
    {
        return profile.proficiency() == null ? 1 : data.getWeaponRank(profile.proficiency());
    }

    public static int proficiencyAttackAdjustment(CharacterData data, WeaponCombatProfile profile)
    {
        if (profile.proficiency() == null) return 0;
        return CombatRules.weaponProficiencyAttackAdjustment(
                data.getCharacterClass(), proficiencyRank(data, profile));
    }

    public static int totalAttackAdjustment(CharacterData data, WeaponCombatProfile profile)
    {
        return CombatRules.strengthAttackAdjustment(data.getStrength(), data.getExceptionalStrength())
                + proficiencyAttackAdjustment(data, profile);
    }

    public static int totalDamageAdjustment(CharacterData data, WeaponCombatProfile profile)
    {
        int specialization = proficiencyRank(data, profile) >= 2 ? 2 : 0;
        return CombatRules.strengthDamageAdjustment(data.getStrength(), data.getExceptionalStrength())
                + specialization;
    }

    public static String proficiencyText(CharacterData data, WeaponCombatProfile profile)
    {
        if (profile.proficiency() == null) return "Unclassified";
        int rank = proficiencyRank(data, profile);
        if (rank <= 0) return "Not proficient";
        if (rank == 1) return "Proficient";
        return "Specialized";
    }

    public static String damageText(CharacterData data, WeaponCombatProfile profile)
    {
        int totalBonus = profile.damageBonus() + totalDamageAdjustment(data, profile);
        String bonus = totalBonus > 0 ? "+" + totalBonus
                : totalBonus < 0 ? Integer.toString(totalBonus) : "";
        return profile.diceCount() + "d" + profile.dieSize() + bonus;
    }

    public static int rollDamage(CharacterData data, WeaponCombatProfile profile)
    {
        return Math.max(1, profile.rollDamage() + totalDamageAdjustment(data, profile));
    }
}
