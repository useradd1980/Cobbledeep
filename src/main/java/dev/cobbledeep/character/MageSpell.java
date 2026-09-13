package dev.cobbledeep.character;

/**
 * Level-one arcane spells available to a starting Mage.
 *
 * The initial list is based on classic computer-RPG wizard spellbooks,
 * but Cobbledeep owns the implementation and spell behavior.
 */
public enum MageSpell
{
    ARMOR("Armor", "Conjuration", "Creates a protective magical armor around the caster."),
    BLINDNESS("Blindness", "Illusion", "Attempts to blind a creature, reducing its combat effectiveness."),
    BURNING_HANDS("Burning Hands", "Alteration", "Projects a short cone of flame from the caster's hands."),
    CHARM_PERSON("Charm Person", "Enchantment", "Temporarily causes a humanoid target to regard the caster as an ally."),
    CHILL_TOUCH("Chill Touch", "Necromancy", "Empowers a touch attack with necromantic cold."),
    CHROMATIC_ORB("Chromatic Orb", "Invocation", "Launches an orb whose damage and secondary effects improve with caster level."),
    COLOR_SPRAY("Color Spray", "Alteration", "Unleashes a fan of dazzling colors that can disable weaker creatures."),
    FIND_FAMILIAR("Find Familiar", "Conjuration", "Summons a small magical companion bound to the caster."),
    FRIENDS("Friends", "Enchantment", "Temporarily improves the caster's presence and social influence."),
    GREASE("Grease", "Conjuration", "Coats an area in slippery grease, making movement difficult."),
    IDENTIFY("Identify", "Divination", "Reveals the properties of an unidentified magical item."),
    INFRAVISION("Infravision", "Divination", "Grants enhanced vision in darkness for a limited time."),
    LARLOCHS_MINOR_DRAIN("Larloch's Minor Drain", "Necromancy", "Drains a small amount of life from a target and transfers it to the caster."),
    MAGIC_MISSILE("Magic Missile", "Invocation", "Fires an unerring bolt of magical force at a target."),
    PROTECTION_FROM_EVIL("Protection From Evil", "Abjuration", "Grants defensive protection against hostile evil creatures."),
    PROTECTION_FROM_PETRIFICATION("Protection From Petrification", "Abjuration", "Protects a creature from petrifying attacks."),
    REFLECTED_IMAGE("Reflected Image", "Illusion", "Creates a duplicate image that can absorb an incoming attack."),
    SHIELD("Shield", "Invocation", "Creates a magical shield that improves defense, especially against missiles."),
    SHOCKING_GRASP("Shocking Grasp", "Alteration", "Charges the caster's hand with electricity for a touch attack."),
    SLEEP("Sleep", "Enchantment", "Puts weaker creatures in an area into magical sleep."),
    SPOOK("Spook", "Illusion", "Conjures a terrifying illusion that may cause a target to flee." );

    private final String displayName;
    private final String school;
    private final String description;

    MageSpell(String displayName, String school, String description)
    {
        this.displayName = displayName;
        this.school = school;
        this.description = description;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getSchool()
    {
        return school;
    }

    public String getDescription()
    {
        return description;
    }
}
