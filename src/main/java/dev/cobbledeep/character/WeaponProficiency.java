package dev.cobbledeep.character;

/**
 * Weapon proficiency categories based on Baldur's Gate II.
 * Fighting styles are represented separately by {@link FightingStyle}.
 */
public enum WeaponProficiency
{
    BASTARD_SWORD("Bastard Sword"),
    LONG_SWORD("Long Sword"),
    SHORT_SWORD("Short Sword"),
    AXE("Axe"),
    TWO_HANDED_SWORD("Two-Handed Sword"),
    KATANA("Katana"),
    SCIMITAR_WAKIZASHI_NINJATO("Scimitar / Wakizashi / Ninjato"),
    DAGGER("Dagger"),
    WAR_HAMMER("War Hammer"),
    SPEAR("Spear"),
    HALBERD("Halberd"),
    FLAIL_MORNING_STAR("Flail / Morning Star"),
    MACE("Mace"),
    QUARTERSTAFF("Quarterstaff"),
    CROSSBOW("Crossbow"),
    LONGBOW("Longbow"),
    SHORTBOW("Shortbow"),
    DART("Dart"),
    SLING("Sling"),
    CLUB("Club");

    private final String displayName;

    WeaponProficiency(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }
}
