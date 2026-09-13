package dev.cobbledeep.character;

import java.util.Set;

public enum CharacterRace
{
    HUMAN(
            "Human",
            "Humans are versatile and adaptable. They may choose any class "
            + "and receive no racial ability score adjustments.",
            Set.of(
                    CharacterClass.FIGHTER,
                    CharacterClass.RANGER,
                    CharacterClass.PALADIN,
                    CharacterClass.CLERIC,
                    CharacterClass.DRUID,
                    CharacterClass.MAGE,
                    CharacterClass.THIEF,
                    CharacterClass.BARD
            )
    ),

    ELF(
            "Elf",
            "Elves are graceful, perceptive, and long-lived. They receive +1 Dexterity "
            + "and -1 Constitution, possess infravision, and have restricted class choices.",
            Set.of(
                    CharacterClass.FIGHTER,
                    CharacterClass.RANGER,
                    CharacterClass.MAGE,
                    CharacterClass.THIEF
            )
    ),

    HALF_ELF(
            "Half-Elf",
            "Half-Elves combine human versatility with elven heritage. They possess "
            + "infravision and have access to a wide variety of classes.",
            Set.of(
                    CharacterClass.FIGHTER,
                    CharacterClass.RANGER,
                    CharacterClass.CLERIC,
                    CharacterClass.DRUID,
                    CharacterClass.MAGE,
                    CharacterClass.THIEF,
                    CharacterClass.BARD
            )
    ),

    DWARF(
            "Dwarf",
            "Dwarves are hardy and resilient. They receive +1 Constitution and "
            + "-1 Charisma, possess infravision, and gain strong resistance to "
            + "poison and magic.",
            Set.of(
                    CharacterClass.FIGHTER,
                    CharacterClass.CLERIC,
                    CharacterClass.THIEF
            )
    ),

    HALFLING(
            "Halfling",
            "Halflings are small and nimble. They receive +1 Dexterity and -1 Strength "
            + "and gain strong saving throw bonuses, but have limited class choices.",
            Set.of(
                    CharacterClass.FIGHTER,
                    CharacterClass.CLERIC,
                    CharacterClass.THIEF
            )
    ),

    GNOME(
            "Gnome",
            "Gnomes are clever and magically inclined. They receive +1 Intelligence "
            + "and -1 Wisdom, possess infravision, and have an affinity for illusion magic.",
            Set.of(
                    CharacterClass.FIGHTER,
                    CharacterClass.CLERIC,
                    CharacterClass.MAGE,
                    CharacterClass.THIEF
            )
    );

    private final String displayName;
    private final String description;
    private final Set<CharacterClass> allowedClasses;

    CharacterRace(
            String displayName,
            String description,
            Set<CharacterClass> allowedClasses)
    {
        this.displayName = displayName;
        this.description = description;
        this.allowedClasses = allowedClasses;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean canChooseClass(
            CharacterClass characterClass)
    {
        return allowedClasses.contains(characterClass);
    }
}