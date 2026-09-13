package dev.cobbledeep.character;

public enum FightingStyle
{
    TWO_HANDED_WEAPON("Two-Handed Weapon Style"),
    SWORD_AND_SHIELD("Sword and Shield Style"),
    SINGLE_WEAPON("Single-Weapon Style"),
    TWO_WEAPON("Two-Weapon Style");

    private final String displayName;

    FightingStyle(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
