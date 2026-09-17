package dev.cobbledeep.combat;

import dev.cobbledeep.character.CharacterClass;

/** Pure AD&D-style calculations, deliberately independent of Minecraft state. */
public final class CombatRules
{
    private CombatRules() {}

    public static boolean isWarrior(CharacterClass characterClass)
    {
        return characterClass == CharacterClass.FIGHTER
                || characterClass == CharacterClass.RANGER
                || characterClass == CharacterClass.PALADIN;
    }

    public static int hitDie(CharacterClass characterClass)
    {
        return switch (characterClass)
        {
            case FIGHTER, RANGER, PALADIN -> 10;
            case CLERIC, DRUID -> 8;
            case THIEF, BARD -> 6;
            case MAGE -> 4;
        };
    }

    public static int constitutionHitPointAdjustment(int constitution, boolean warrior)
    {
        if (constitution <= 3) return -2;
        if (constitution <= 6) return -1;
        if (constitution <= 14) return 0;
        if (constitution == 15) return 1;
        if (constitution == 16) return 2;
        if (constitution == 17) return warrior ? 3 : 2;
        return warrior ? 4 : 2;
    }

    public static int levelOneHitPoints(CharacterClass characterClass, int constitution)
    {
        return Math.max(1, hitDie(characterClass)
                + constitutionHitPointAdjustment(constitution, isWarrior(characterClass)));
    }

    public static int thac0(CharacterClass characterClass, int level)
    {
        int safeLevel = Math.max(1, level);
        if (isWarrior(characterClass)) return Math.max(0, 21 - safeLevel);
        return switch (characterClass)
        {
            case CLERIC, DRUID -> Math.max(0, 20 - 2 * ((safeLevel - 1) / 3));
            case THIEF, BARD -> Math.max(0, 20 - ((safeLevel - 1) / 2));
            case MAGE -> Math.max(0, 20 - ((safeLevel - 1) / 3));
            default -> throw new IllegalStateException("Unhandled character class " + characterClass);
        };
    }

    public static int strengthAttackAdjustment(int strength, int exceptionalStrength)
    {
        if (strength <= 3) return -3;
        if (strength <= 5) return -2;
        if (strength <= 7) return -1;
        if (strength <= 16) return 0;
        if (strength == 17) return 1;
        if (exceptionalStrength >= 100) return 3;
        return exceptionalStrength >= 51 ? 2 : 1;
    }

    public static int dexterityArmorClassAdjustment(int dexterity)
    {
        if (dexterity <= 3) return 4;
        if (dexterity == 4) return 3;
        if (dexterity == 5) return 2;
        if (dexterity == 6) return 1;
        if (dexterity <= 14) return 0;
        if (dexterity == 15) return -1;
        if (dexterity == 16) return -2;
        if (dexterity == 17) return -3;
        return -4;
    }

    public static int requiredRoll(int thac0, int targetArmorClass, int attackAdjustment)
    {
        return thac0 - targetArmorClass - attackAdjustment;
    }

    public static boolean hits(int naturalRoll, int thac0, int targetArmorClass, int attackAdjustment)
    {
        if (naturalRoll <= 1) return false;
        if (naturalRoll >= 20) return true;
        return naturalRoll >= requiredRoll(thac0, targetArmorClass, attackAdjustment);
    }
}
