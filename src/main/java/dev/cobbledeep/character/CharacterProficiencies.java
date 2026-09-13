package dev.cobbledeep.character;

import java.util.EnumMap;
import java.util.Map;

public class CharacterProficiencies
{
    private final Map<WeaponProficiency, Integer> weaponRanks =
            new EnumMap<>(WeaponProficiency.class);

    private final Map<FightingStyle, Integer> styleRanks =
            new EnumMap<>(FightingStyle.class);

    private int availablePoints;
    private boolean initialized;

    public CharacterProficiencies()
    {
        reset();
    }

    public void initialize(CharacterClass characterClass)
    {
        reset();

        if (characterClass == null)
        {
            return;
        }

        // Level-one BG-style starting proficiency-slot budgets.
        availablePoints = switch (characterClass)
        {
            case FIGHTER, RANGER, PALADIN -> 4;
            case CLERIC, DRUID, THIEF, BARD -> 2;
            case MAGE -> 1;
        };

        // Rangers receive two innate ranks in Two-Weapon Style.
        if (characterClass == CharacterClass.RANGER)
        {
            styleRanks.put(FightingStyle.TWO_WEAPON, 2);
        }

        initialized = true;
    }

    public void reset()
    {
        weaponRanks.clear();
        styleRanks.clear();

        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            weaponRanks.put(proficiency, 0);
        }

        for (FightingStyle style : FightingStyle.values())
        {
            styleRanks.put(style, 0);
        }

        availablePoints = 0;
        initialized = false;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public int getAvailablePoints()
    {
        return availablePoints;
    }

    public int getWeaponRank(WeaponProficiency proficiency)
    {
        return weaponRanks.getOrDefault(proficiency, 0);
    }

    public int getStyleRank(FightingStyle style)
    {
        return styleRanks.getOrDefault(style, 0);
    }

    public int getMaximumWeaponRank(CharacterClass characterClass)
    {
        if (characterClass == null)
        {
            return 0;
        }

        // At level one, warriors may specialize; other classes are limited to proficiency.
        return switch (characterClass)
        {
            case FIGHTER, RANGER, PALADIN -> 2;
            case CLERIC, DRUID, MAGE, THIEF, BARD -> 1;
        };
    }

    public int getMaximumStyleRank(CharacterClass characterClass, FightingStyle style)
    {
        if (characterClass == null || style == null || !canUseStyle(characterClass, style))
        {
            return 0;
        }

        if (style == FightingStyle.TWO_WEAPON
                && (characterClass == CharacterClass.FIGHTER
                    || characterClass == CharacterClass.RANGER
                    || characterClass == CharacterClass.PALADIN))
        {
            return 3;
        }

        return switch (characterClass)
        {
            case FIGHTER, RANGER, PALADIN -> 2;
            case CLERIC, DRUID, THIEF, BARD -> 1;
            case MAGE -> 0;
        };
    }

    public boolean canUseWeapon(CharacterClass characterClass, WeaponProficiency proficiency)
    {
        if (characterClass == null || proficiency == null)
        {
            return false;
        }

        return switch (characterClass)
        {
            case FIGHTER, RANGER, PALADIN -> true;

            case BARD -> true;

            case CLERIC -> switch (proficiency)
            {
                case CLUB, MACE, SLING, FLAIL_MORNING_STAR,
                     QUARTERSTAFF, WAR_HAMMER -> true;
                default -> false;
            };

            case DRUID -> switch (proficiency)
            {
                case CLUB, DAGGER, DART, SCIMITAR_WAKIZASHI_NINJATO,
                     SPEAR, QUARTERSTAFF, SLING -> true;
                default -> false;
            };

            case MAGE -> switch (proficiency)
            {
                case DAGGER, DART, SLING, QUARTERSTAFF -> true;
                default -> false;
            };

            case THIEF -> switch (proficiency)
            {
                case CLUB, DAGGER, DART, LONG_SWORD, SHORTBOW,
                     SHORT_SWORD, QUARTERSTAFF, SLING,
                     KATANA, SCIMITAR_WAKIZASHI_NINJATO,
                     CROSSBOW -> true;
                default -> false;
            };
        };
    }

    public boolean canUseStyle(CharacterClass characterClass, FightingStyle style)
    {
        if (characterClass == null || style == null)
        {
            return false;
        }

        return characterClass != CharacterClass.MAGE;
    }

    public void increaseWeapon(CharacterClass characterClass, WeaponProficiency proficiency)
    {
        if (availablePoints <= 0 || !canUseWeapon(characterClass, proficiency))
        {
            return;
        }

        int currentRank = getWeaponRank(proficiency);
        int maximumRank = getMaximumWeaponRank(characterClass);

        if (currentRank >= maximumRank)
        {
            return;
        }

        weaponRanks.put(proficiency, currentRank + 1);
        availablePoints--;
    }

    public void decreaseWeapon(WeaponProficiency proficiency)
    {
        int currentRank = getWeaponRank(proficiency);

        if (currentRank <= 0)
        {
            return;
        }

        weaponRanks.put(proficiency, currentRank - 1);
        availablePoints++;
    }

    public void increaseStyle(CharacterClass characterClass, FightingStyle style)
    {
        if (availablePoints <= 0 || !canUseStyle(characterClass, style))
        {
            return;
        }

        int currentRank = getStyleRank(style);
        int maximumRank = getMaximumStyleRank(characterClass, style);

        if (currentRank >= maximumRank)
        {
            return;
        }

        styleRanks.put(style, currentRank + 1);
        availablePoints--;
    }

    public void decreaseStyle(CharacterClass characterClass, FightingStyle style)
    {
        int currentRank = getStyleRank(style);
        int minimumRank = characterClass == CharacterClass.RANGER
                && style == FightingStyle.TWO_WEAPON ? 2 : 0;

        if (currentRank <= minimumRank)
        {
            return;
        }

        styleRanks.put(style, currentRank - 1);
        availablePoints++;
    }
}
