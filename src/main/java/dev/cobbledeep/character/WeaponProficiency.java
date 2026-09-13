package dev.cobbledeep.character;

/**
 * Broad weapon proficiency groups used by the original Baldur's Gate
 * proficiency system. Fighting styles are represented separately by
 * {@link FightingStyle}.
 */
public enum WeaponProficiency
{
    LARGE_SWORD("Large Sword"),
    SMALL_SWORD("Small Sword"),
    BOW("Bow"),
    SPEAR("Spear"),
    AXE("Axe"),
    BLUNT_WEAPONS("Blunt Weapons"),
    SPIKED_WEAPONS("Spiked Weapons"),
    MISSILE_WEAPONS("Missile Weapons");

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
