package dev.cobbledeep.character;

/**
 * Level-one arcane spells available to a starting Mage.
 *
 * The spell names and mechanics follow the classic AD&D/Baldur's Gate-style
 * rules Cobbledeep is using. Descriptive wording is written for Cobbledeep.
 */
public enum MageSpell
{
    ARMOR(
            "Armor", "Conjuration",
            "Duration: 9 hours | Target: Caster | Effect: Sets base AC to 6",
            "Surrounds the caster with invisible magical protection. The protection lasts until its duration expires or it is dispelled."),
    BLINDNESS(
            "Blindness", "Illusion",
            "Duration: 2 hours | Range: 90 ft | Target: 1 creature | Save: Negates",
            "Attempts to rob one creature of its sight. A creature that fails its save is blinded, greatly reducing its effectiveness in combat."),
    BURNING_HANDS(
            "Burning Hands", "Alteration",
            "Duration: Instant | Range: 15 ft | Area: Cone | Damage: 1d3 fire per caster level (max 1d3+20)",
            "Projects a fan of flame from the caster's hands, burning creatures caught directly in front of the mage. Damage increases with caster level."),
    CHARM_PERSON(
            "Charm Person", "Enchantment",
            "Duration: 5 rounds | Range: 90 ft | Target: 1 humanoid | Save: Negates",
            "Attempts to magically influence a humanoid creature. On a failed save, the victim temporarily regards the caster as an ally."),
    CHILL_TOUCH(
            "Chill Touch", "Necromancy",
            "Duration: 1 round + 1 round/level | Range: Touch | Damage: 1d4 cold | Save: Special",
            "Fills the caster's hand with numbing necromantic energy. Successful melee touches inflict cold damage and may weaken the victim."),
    CHROMATIC_ORB(
            "Chromatic Orb", "Invocation",
            "Duration: Special | Range: 90 ft | Target: 1 creature | Damage: 1d4 at level 1 | Save: Special",
            "Launches a sphere of magical energy that strikes its target unerringly. At level 1 it deals 1d4 damage and can blind the victim for 1 round; its effects improve dramatically at higher caster levels."),
    COLOR_SPRAY(
            "Color Spray", "Alteration",
            "Duration: 5 rounds | Range: 45 ft | Area: Cone | Save: Negates",
            "Unleashes a fan of brilliant colors. Creatures caught in the cone that fail their save may fall unconscious for the spell's duration."),
    FIND_FAMILIAR(
            "Find Familiar", "Conjuration",
            "Duration: Permanent | Range: Caster | Target: Caster | Use: Normally once",
            "Calls a magical companion whose nature depends on the caster. The familiar strengthens its master while carried safely, but its death can seriously harm the mage."),
    FRIENDS(
            "Friends", "Enchantment",
            "Duration: 1d4 rounds + 1 round/level | Target: Caster | Effect: +6 Charisma",
            "Temporarily enhances the caster's personal magnetism, granting a substantial Charisma bonus for social interactions."),
    GREASE(
            "Grease", "Conjuration",
            "Duration: 3 rounds + 1 round/level | Range: 30 ft | Area: 15-ft radius | Save: Each round",
            "Covers the ground with magically slick grease. Creatures in the area must repeatedly save or fall, making it useful for controlling chokepoints and groups."),
    IDENTIFY(
            "Identify", "Divination",
            "Duration: Instant | Target: 1 item | Effect: Reveals magical properties",
            "Reveals the magical properties of an unidentified item, including the powers available to its wielder."),
    INFRAVISION(
            "Infravision", "Divination",
            "Duration: 2 hours | Range: 30 ft | Target: 1 creature",
            "Grants enhanced vision in darkness, allowing the recipient to perceive heat and see where ordinary vision would struggle."),
    LARLOCHS_MINOR_DRAIN(
            "Larloch's Minor Drain", "Necromancy",
            "Duration: 1 hour | Range: 30 ft | Target: 1 creature | Damage: 4 | Save: None",
            "Drains 4 hit points from the target and transfers that life force to the caster as temporary hit points for the duration."),
    MAGIC_MISSILE(
            "Magic Missile", "Invocation",
            "Duration: Instant | Range: 90 ft | Target: 1 creature | Damage: 1d4+1 per missile | Save: None",
            "Fires an unerring missile of magical force. The caster gains additional missiles with experience, eventually launching as many as five at once."),
    PROTECTION_FROM_EVIL(
            "Protection From Evil", "Abjuration",
            "Duration: 2 rounds/level | Range: Touch | Target: 1 creature | Effect: Defensive ward",
            "Places a ward around one creature. Evil attackers suffer penalties when attacking the protected target, improving the recipient's defenses."),
    PROTECTION_FROM_PETRIFICATION(
            "Protection From Petrification", "Abjuration",
            "Duration: 1 hour | Range: Touch | Target: 1 creature | Effect: Petrification immunity",
            "Wards one creature against magical attacks that would turn flesh to stone for the duration of the spell."),
    REFLECTED_IMAGE(
            "Reflected Image", "Illusion",
            "Duration: 3 rounds + 1 round/level | Target: Caster | Effect: Creates 1 duplicate",
            "Creates an illusory duplicate of the caster. When the mage is attacked, there is an equal chance that the attack strikes the image instead."),
    SHIELD(
            "Shield", "Invocation",
            "Duration: 1 hour | Target: Caster | Effect: AC 4 vs melee, AC 2 vs missiles",
            "Creates an invisible barrier of force around the caster. It provides strong protection from ordinary attacks and even greater protection from missile weapons."),
    SHOCKING_GRASP(
            "Shocking Grasp", "Alteration",
            "Duration: Until discharged | Range: Touch | Damage: 1d8 + 1/level electrical",
            "Charges the caster's hand with electrical energy. The charge remains until a successful melee touch discharges it into an enemy."),
    SLEEP(
            "Sleep", "Enchantment",
            "Duration: 5 rounds/level | Range: 90 ft | Area: 15-ft radius | Save: Negates",
            "Sends vulnerable creatures in an area into magical sleep. Creatures that fail their save become helpless until the spell ends or they are awakened."),
    SPOOK(
            "Spook", "Illusion",
            "Duration: 3 rounds | Range: 90 ft | Target: 1 creature | Save: Negates",
            "Creates a terrifying illusion visible only to one target. A victim that fails its save panics and flees; the saving throw becomes harder as the caster gains levels." );

    private final String displayName;
    private final String school;
    private final String statistics;
    private final String description;

    MageSpell(String displayName, String school, String statistics, String description)
    {
        this.displayName = displayName;
        this.school = school;
        this.statistics = statistics;
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

    public String getStatistics()
    {
        return statistics;
    }

    public String getDescription()
    {
        return description;
    }
}
