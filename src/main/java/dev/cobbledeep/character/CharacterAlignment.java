package dev.cobbledeep.character;

public enum CharacterAlignment
{
    LAWFUL_GOOD(
            "Lawful Good",
            "Lawful Good characters value order, duty, and compassion. "
            + "They try to do what is right while respecting laws, traditions, "
            + "and legitimate authority."
    ),

    NEUTRAL_GOOD(
            "Neutral Good",
            "Neutral Good characters are guided primarily by compassion and the "
            + "desire to help others, without a strong commitment to either law or chaos."
    ),

    CHAOTIC_GOOD(
            "Chaotic Good",
            "Chaotic Good characters value freedom and individual conscience. "
            + "They oppose oppression and are willing to ignore unjust laws."
    ),

    LAWFUL_NEUTRAL(
            "Lawful Neutral",
            "Lawful Neutral characters place great importance on order, rules, "
            + "tradition, or a personal code, regardless of whether the outcome is good or evil."
    ),

    TRUE_NEUTRAL(
            "True Neutral",
            "True Neutral characters avoid strong commitments to law, chaos, good, "
            + "or evil, often preferring balance, practicality, or personal interests."
    ),

    CHAOTIC_NEUTRAL(
            "Chaotic Neutral",
            "Chaotic Neutral characters value personal freedom above structure and "
            + "tradition. They tend to follow their own impulses and resist restrictions."
    ),

    LAWFUL_EVIL(
            "Lawful Evil",
            "Lawful Evil characters use systems, hierarchies, laws, and codes to "
            + "advance selfish or cruel goals."
    ),

    NEUTRAL_EVIL(
            "Neutral Evil",
            "Neutral Evil characters are primarily concerned with their own interests "
            + "and will use whatever methods are most advantageous."
    ),

    CHAOTIC_EVIL(
            "Chaotic Evil",
            "Chaotic Evil characters reject restraint and pursue their goals through "
            + "violence, domination, or destruction when it suits them."
    );

    private final String displayName;
    private final String description;

    CharacterAlignment(
            String displayName,
            String description)
    {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDescription()
    {
        return description;
    }
}