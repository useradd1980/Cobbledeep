package dev.cobbledeep.character;

import java.util.Set;

public enum CharacterClass
{
    FIGHTER(
            "Fighter",
            "Fighters are masters of weapons and armor. They have the best combat "
            + "progression, receive more weapon proficiency choices, and can develop "
            + "exceptional Strength when their Strength score reaches 18.",
            Set.of(
                    CharacterAlignment.LAWFUL_GOOD,
                    CharacterAlignment.NEUTRAL_GOOD,
                    CharacterAlignment.CHAOTIC_GOOD,
                    CharacterAlignment.LAWFUL_NEUTRAL,
                    CharacterAlignment.TRUE_NEUTRAL,
                    CharacterAlignment.CHAOTIC_NEUTRAL,
                    CharacterAlignment.LAWFUL_EVIL,
                    CharacterAlignment.NEUTRAL_EVIL,
                    CharacterAlignment.CHAOTIC_EVIL
            )
    ),

    RANGER(
            "Ranger",
            "Rangers are skilled warriors and wilderness experts. They combine strong "
            + "combat ability with tracking, survival skills, and limited divine magic "
            + "at higher levels.",
            Set.of(
                    CharacterAlignment.LAWFUL_GOOD,
                    CharacterAlignment.NEUTRAL_GOOD,
                    CharacterAlignment.CHAOTIC_GOOD
            )
    ),

    PALADIN(
            "Paladin",
            "Paladins are holy warriors bound by strict ideals. They combine martial "
            + "ability with divine powers, strong saving throws, and special abilities "
            + "against evil creatures.",
            Set.of(
                    CharacterAlignment.LAWFUL_GOOD
            )
    ),

    CLERIC(
            "Cleric",
            "Clerics are divine spellcasters and capable frontline fighters. They use "
            + "divine magic for healing, protection, and combat support.",
            Set.of(
                    CharacterAlignment.LAWFUL_GOOD,
                    CharacterAlignment.NEUTRAL_GOOD,
                    CharacterAlignment.CHAOTIC_GOOD,
                    CharacterAlignment.LAWFUL_NEUTRAL,
                    CharacterAlignment.TRUE_NEUTRAL,
                    CharacterAlignment.CHAOTIC_NEUTRAL,
                    CharacterAlignment.LAWFUL_EVIL,
                    CharacterAlignment.NEUTRAL_EVIL,
                    CharacterAlignment.CHAOTIC_EVIL
            )
    ),

    DRUID(
            "Druid",
            "Druids draw divine power from nature. They use nature-oriented magic and "
            + "eventually gain abilities related to animals and shapeshifting.",
            Set.of(
                    CharacterAlignment.TRUE_NEUTRAL
            )
    ),

    MAGE(
            "Mage",
            "Mages are powerful arcane spellcasters. They have limited weapon and armor "
            + "options, but gain access to a wide range of arcane spells.",
            Set.of(
                    CharacterAlignment.LAWFUL_GOOD,
                    CharacterAlignment.NEUTRAL_GOOD,
                    CharacterAlignment.CHAOTIC_GOOD,
                    CharacterAlignment.LAWFUL_NEUTRAL,
                    CharacterAlignment.TRUE_NEUTRAL,
                    CharacterAlignment.CHAOTIC_NEUTRAL,
                    CharacterAlignment.LAWFUL_EVIL,
                    CharacterAlignment.NEUTRAL_EVIL,
                    CharacterAlignment.CHAOTIC_EVIL
            )
    ),

    THIEF(
            "Thief",
            "Thieves specialize in stealth, traps, locks, and opportunistic combat. "
            + "They receive thief skill points that can be allocated during character creation.",
            Set.of(
                    CharacterAlignment.NEUTRAL_GOOD,
                    CharacterAlignment.CHAOTIC_GOOD,
                    CharacterAlignment.LAWFUL_NEUTRAL,
                    CharacterAlignment.TRUE_NEUTRAL,
                    CharacterAlignment.CHAOTIC_NEUTRAL,
                    CharacterAlignment.LAWFUL_EVIL,
                    CharacterAlignment.NEUTRAL_EVIL,
                    CharacterAlignment.CHAOTIC_EVIL
            )
    ),

    BARD(
            "Bard",
            "Bards combine combat, thief-like abilities, lore, and arcane magic. "
            + "They are versatile but less specialized than dedicated fighters, thieves, or mages.",
            Set.of(
                    CharacterAlignment.NEUTRAL_GOOD,
                    CharacterAlignment.LAWFUL_NEUTRAL,
                    CharacterAlignment.TRUE_NEUTRAL,
                    CharacterAlignment.CHAOTIC_NEUTRAL,
                    CharacterAlignment.NEUTRAL_EVIL
            )
    );

    private final String displayName;
    private final String description;
    private final Set<CharacterAlignment> allowedAlignments;

    CharacterClass(
            String displayName,
            String description,
            Set<CharacterAlignment> allowedAlignments)
    {
        this.displayName = displayName;
        this.description = description;
        this.allowedAlignments = allowedAlignments;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean canChooseAlignment(
            CharacterAlignment alignment)
    {
        return allowedAlignments.contains(alignment);
    }
}