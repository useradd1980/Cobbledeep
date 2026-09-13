package dev.cobbledeep.character;

/**
 * Level-one arcane spells available to a starting Mage.
 *
 * The spell names and broad mechanics are inspired by classic tabletop and
 * computer RPG traditions. Descriptions are written specifically for Cobbledeep.
 */
public enum MageSpell
{
    ARMOR(
            "Armor",
            "Conjuration",
            "Surrounds the caster with invisible magical protection. It is intended as a long-lasting defensive spell for lightly armored mages."),
    BLINDNESS(
            "Blindness",
            "Illusion",
            "Attempts to rob one creature of its sight. A blinded enemy has great difficulty seeing and fighting effectively until the effect ends."),
    BURNING_HANDS(
            "Burning Hands",
            "Alteration",
            "Projects a short burst of flame from the caster's hands. It is most useful at close range and can scorch several creatures caught in front of the caster."),
    CHARM_PERSON(
            "Charm Person",
            "Enchantment",
            "Attempts to magically influence a humanoid creature, causing it to treat the caster as a friend for a limited time."),
    CHILL_TOUCH(
            "Chill Touch",
            "Necromancy",
            "Fills the caster's hand with numbing necromantic energy. A successful touch harms the target and may weaken its ability to fight back."),
    CHROMATIC_ORB(
            "Chromatic Orb",
            "Invocation",
            "Launches a brightly colored sphere of magical energy. Its damage and possible secondary effects become more potent as the caster grows in experience."),
    COLOR_SPRAY(
            "Color Spray",
            "Alteration",
            "Unleashes a fan of brilliant colors. Weaker creatures caught in the display may become stunned, blinded, or otherwise unable to act effectively."),
    FIND_FAMILIAR(
            "Find Familiar",
            "Conjuration",
            "Calls a small magical companion to bond with the caster. A familiar can provide companionship and useful abilities, but the bond also carries risks."),
    FRIENDS(
            "Friends",
            "Enchantment",
            "Temporarily enhances the caster's personal magnetism, making social interaction and persuasion easier while the spell remains active."),
    GREASE(
            "Grease",
            "Conjuration",
            "Covers an area with magically slick grease. Creatures moving through it may lose their footing, making the spell useful for controlling a battlefield."),
    IDENTIFY(
            "Identify",
            "Divination",
            "Reveals the magical properties of an otherwise unidentified item, allowing the caster to learn what the object does."),
    INFRAVISION(
            "Infravision",
            "Divination",
            "Temporarily grants enhanced vision in darkness, allowing the recipient to see more clearly where ordinary sight would struggle."),
    LARLOCHS_MINOR_DRAIN(
            "Larloch's Minor Drain",
            "Necromancy",
            "Steals a small amount of life force from a target and transfers it to the caster, harming the victim while temporarily strengthening the mage."),
    MAGIC_MISSILE(
            "Magic Missile",
            "Invocation",
            "Creates a bolt of magical force that flies directly toward its target. Additional missiles can be produced as the caster becomes more experienced."),
    PROTECTION_FROM_EVIL(
            "Protection From Evil",
            "Abjuration",
            "Places a protective ward around one creature, improving its defenses against hostile creatures of evil intent."),
    PROTECTION_FROM_PETRIFICATION(
            "Protection From Petrification",
            "Abjuration",
            "Wards one creature against magical effects that would turn flesh to stone."),
    REFLECTED_IMAGE(
            "Reflected Image",
            "Illusion",
            "Creates an illusory duplicate of the caster. An incoming attack may strike the false image instead of the real mage."),
    SHIELD(
            "Shield",
            "Invocation",
            "Creates an invisible barrier of force in front of the caster. It improves defense and is especially useful against missile attacks."),
    SHOCKING_GRASP(
            "Shocking Grasp",
            "Alteration",
            "Charges the caster's hand with electrical energy. The next successful touch can discharge the stored electricity into an enemy."),
    SLEEP(
            "Sleep",
            "Enchantment",
            "Sends weaker creatures in an area into magical sleep. Sleeping enemies are temporarily helpless until they awaken or are disturbed."),
    SPOOK(
            "Spook",
            "Illusion",
            "Creates a terrifying image visible only to the chosen target. If the illusion takes hold, the victim may panic and flee from danger." );

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
