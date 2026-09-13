package dev.cobbledeep.character;

/**
 * Level-one divine spells available to starting Clerics and Druids.
 *
 * The broad mechanics follow classic AD&D-style computer RPG conventions,
 * while the descriptions are written specifically for Cobbledeep.
 */
public enum DivineSpell
{
    ARMOR_OF_FAITH(
            "Armor of Faith",
            "Abjuration",
            "Duration: 3 rounds + 1 round/level | Range: Self | Save: None | Effect: 5% physical damage resistance at level 1; improves with level",
            "Wraps the caster in divine protection, reducing incoming physical damage for a short time.",
            true,
            true),
    BLESS(
            "Bless",
            "Conjuration",
            "Duration: 6 rounds | Range: 90 ft | Area: 15-ft radius | Save: None | Effect: allies gain +1 to attack rolls and morale",
            "Calls down a brief blessing over nearby allies, improving their confidence and accuracy in battle.",
            true,
            true),
    CHARM_PERSON_OR_MAMMAL(
            "Charm Person or Mammal",
            "Enchantment",
            "Duration: 5 rounds | Range: 90 ft | Target: 1 humanoid or mammal | Save: Spell negates",
            "Attempts to bend the will of a humanoid or ordinary mammal, temporarily causing it to regard the caster as an ally.",
            false,
            true),
    COMMAND(
            "Command",
            "Enchantment",
            "Duration: 1 round | Range: 90 ft | Target: 1 creature | Save: Spell negates",
            "Issues a magically compelling one-word order. A failed save causes the target to collapse helplessly for a brief moment.",
            true,
            false),
    CURE_LIGHT_WOUNDS(
            "Cure Light Wounds",
            "Necromancy",
            "Duration: Instant | Range: Touch | Target: 1 creature | Healing: 1d8 hit points | Save: None",
            "Channels positive energy through the caster's touch to close minor wounds and restore lost vitality.",
            true,
            true),
    DOOM(
            "Doom",
            "Enchantment",
            "Duration: 1 turn | Range: 90 ft | Target: 1 creature | Save: None | Effect: -2 attack rolls and saving throws",
            "Places a divine curse on one enemy, making its attacks less accurate and its resistance to hostile effects weaker.",
            true,
            true),
    ENTANGLE(
            "Entangle",
            "Alteration",
            "Duration: 1 turn | Range: 90 ft | Area: 15-ft radius | Save: Spell each round | Effect: immobilizes on failed save",
            "Causes roots, vines, and nearby vegetation to seize creatures in an area, potentially holding them in place.",
            false,
            true),
    PROTECTION_FROM_EVIL(
            "Protection From Evil",
            "Abjuration",
            "Duration: 2 rounds/level | Range: Touch | Target: 1 creature | Save: None | Effect: +2 saves and improved defense against evil creatures",
            "Places a holy ward around one creature, making it harder for evil foes and hostile magic to overcome the protected target.",
            true,
            false),
    REMOVE_FEAR(
            "Remove Fear",
            "Abjuration",
            "Duration: 1 hour | Range: 30 ft | Area: nearby allies | Save: None | Effect: removes fear and grants strong resistance to fear",
            "Dispels supernatural fear from nearby allies and steadies their courage against further terror.",
            true,
            false),
    SANCTUARY(
            "Sanctuary",
            "Abjuration",
            "Duration: 2 rounds + 1 round/level | Range: Self | Save: None | Effect: enemies cannot directly target the caster until the ward is broken",
            "Surrounds the caster with a sacred veil. Hostile creatures have difficulty perceiving or directly attacking the caster while the sanctuary lasts.",
            true,
            false),
    SHILLELAGH(
            "Shillelagh",
            "Alteration",
            "Duration: 1 turn | Range: Self | Save: None | Effect: creates an enchanted club dealing 2d4 damage with a +1 attack bonus",
            "Imbues a wooden cudgel with natural power, turning it into a temporary magical weapon.",
            false,
            true);

    private final String displayName;
    private final String school;
    private final String statistics;
    private final String description;
    private final boolean clericSpell;
    private final boolean druidSpell;

    DivineSpell(
            String displayName,
            String school,
            String statistics,
            String description,
            boolean clericSpell,
            boolean druidSpell)
    {
        this.displayName = displayName;
        this.school = school;
        this.statistics = statistics;
        this.description = description;
        this.clericSpell = clericSpell;
        this.druidSpell = druidSpell;
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

    public boolean isAvailableTo(CharacterClass characterClass)
    {
        return switch (characterClass)
        {
            case CLERIC -> clericSpell;
            case DRUID -> druidSpell;
            default -> false;
        };
    }
}
