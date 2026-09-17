package dev.cobbledeep.combat;

import java.util.concurrent.ThreadLocalRandom;

import dev.cobbledeep.character.WeaponProficiency;

public record WeaponCombatProfile(
        String displayName,
        WeaponProficiency proficiency,
        int diceCount,
        int dieSize,
        int damageBonus)
{
    public String damageText()
    {
        String bonus = damageBonus > 0 ? "+" + damageBonus
                : damageBonus < 0 ? Integer.toString(damageBonus) : "";
        return diceCount + "d" + dieSize + bonus;
    }

    public int rollDamage()
    {
        int damage = damageBonus;
        for (int die = 0; die < diceCount; die++)
            damage += ThreadLocalRandom.current().nextInt(1, dieSize + 1);
        return Math.max(1, damage);
    }
}
