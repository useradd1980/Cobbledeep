package dev.cobbledeep.character;

public final class CharacterAbilityRules
{
    public static final int GLOBAL_MINIMUM = 6;
    public static final int DEFAULT_MAXIMUM = 18;

    private CharacterAbilityRules()
    {
        // Utility class
    }

    // -------------------------------------------------
    // MINIMUMS
    // -------------------------------------------------

    public static int getMinimumStrength(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int racialMinimum = switch (race)
        {
            case DWARF -> 8;
            case HALFLING -> 7;
            default -> GLOBAL_MINIMUM;
        };

        int classMinimum = switch (characterClass)
        {
            case FIGHTER -> 9;
            case RANGER -> 13;
            case PALADIN -> 12;
            default -> GLOBAL_MINIMUM;
        };

        return Math.max(
                GLOBAL_MINIMUM,
                Math.max(racialMinimum, classMinimum)
        );
    }

    public static int getMinimumDexterity(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int racialMinimum = switch (race)
        {
            case ELF -> 6;
            case HALF_ELF -> 6;
            case HALFLING -> 7;
            default -> GLOBAL_MINIMUM;
        };

        int classMinimum = switch (characterClass)
        {
            case RANGER -> 13;
            case THIEF -> 9;
            case BARD -> 12;
            default -> GLOBAL_MINIMUM;
        };

        return Math.max(
                GLOBAL_MINIMUM,
                Math.max(racialMinimum, classMinimum)
        );
    }

    public static int getMinimumConstitution(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int racialMinimum = switch (race)
        {
            case DWARF -> 11;
            case ELF -> 7;
            case HALFLING -> 10;
            case GNOME -> 8;
            default -> GLOBAL_MINIMUM;
        };

        int classMinimum = switch (characterClass)
        {
            case RANGER -> 14;
            case PALADIN -> 9;
            default -> GLOBAL_MINIMUM;
        };

        return Math.max(
                GLOBAL_MINIMUM,
                Math.max(racialMinimum, classMinimum)
        );
    }

    public static int getMinimumIntelligence(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int racialMinimum = switch (race)
        {
            case ELF -> 8;
            default -> GLOBAL_MINIMUM;
        };

        int classMinimum = switch (characterClass)
        {
            case MAGE -> 9;
            case BARD -> 13;
            default -> GLOBAL_MINIMUM;
        };

        return Math.max(
                GLOBAL_MINIMUM,
                Math.max(racialMinimum, classMinimum)
        );
    }

    public static int getMinimumWisdom(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int racialMinimum = GLOBAL_MINIMUM;

        int classMinimum = switch (characterClass)
        {
            case RANGER -> 14;
            case PALADIN -> 13;
            case CLERIC -> 9;
            case DRUID -> 12;
            default -> GLOBAL_MINIMUM;
        };

        return Math.max(
                GLOBAL_MINIMUM,
                Math.max(racialMinimum, classMinimum)
        );
    }

    public static int getMinimumCharisma(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int racialMinimum = switch (race)
        {
            case ELF -> 8;
            default -> GLOBAL_MINIMUM;
        };

        int classMinimum = switch (characterClass)
        {
            case PALADIN -> 17;
            case DRUID -> 15;
            case BARD -> 15;
            default -> GLOBAL_MINIMUM;
        };

        return Math.max(
                GLOBAL_MINIMUM,
                Math.max(racialMinimum, classMinimum)
        );
    }

    // -------------------------------------------------
    // BASE SCORE MAXIMUMS
    // -------------------------------------------------

    public static int getMaximumStrength(
            CharacterRace race)
    {
        return DEFAULT_MAXIMUM;
    }

    public static int getMaximumDexterity(
            CharacterRace race)
    {
        return switch (race)
        {
            case DWARF -> 17;
            default -> DEFAULT_MAXIMUM;
        };
    }

    public static int getMaximumConstitution(
            CharacterRace race)
    {
        return DEFAULT_MAXIMUM;
    }

    public static int getMaximumIntelligence(
            CharacterRace race)
    {
        return DEFAULT_MAXIMUM;
    }

    public static int getMaximumWisdom(
            CharacterRace race)
    {
        return DEFAULT_MAXIMUM;
    }

    public static int getMaximumCharisma(
            CharacterRace race)
    {
        return DEFAULT_MAXIMUM;
    }

    // -------------------------------------------------
    // RACIAL MODIFIERS
    // -------------------------------------------------

    public static int getStrengthModifier(
            CharacterRace race)
    {
        return switch (race)
        {
            case HALFLING -> -1;
            default -> 0;
        };
    }

    public static int getDexterityModifier(
            CharacterRace race)
    {
        return switch (race)
        {
            case ELF,
                 HALFLING -> 1;

            default -> 0;
        };
    }

    public static int getConstitutionModifier(
            CharacterRace race)
    {
        return switch (race)
        {
            case DWARF -> 1;
            case ELF -> -1;
            default -> 0;
        };
    }

    public static int getIntelligenceModifier(
            CharacterRace race)
    {
        return switch (race)
        {
            case GNOME -> 1;
            default -> 0;
        };
    }

    public static int getWisdomModifier(
            CharacterRace race)
    {
        return switch (race)
        {
            case GNOME -> -1;
            default -> 0;
        };
    }

    public static int getCharismaModifier(
            CharacterRace race)
    {
        return switch (race)
        {
            case DWARF -> -1;
            default -> 0;
        };
    }

    // -------------------------------------------------
    // EXCEPTIONAL STRENGTH
    // -------------------------------------------------

    public static boolean canHaveExceptionalStrength(
            CharacterRace race,
            CharacterClass characterClass,
            int finalStrength)
    {
        if (finalStrength != 18)
        {
            return false;
        }

        if (race == CharacterRace.HALFLING)
        {
            return false;
        }

        return switch (characterClass)
        {
            case FIGHTER,
                 RANGER,
                 PALADIN -> true;

            default -> false;
        };
    }
}