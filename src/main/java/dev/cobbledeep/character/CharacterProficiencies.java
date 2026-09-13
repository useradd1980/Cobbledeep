package dev.cobbledeep.character;

import java.util.EnumMap;
import java.util.Map;

public class CharacterProficiencies
{
    private final Map<WeaponProficiency, Integer> ranks =
            new EnumMap<>(WeaponProficiency.class);

    private int availablePoints;
    private int maximumRank;
    private boolean initialized;

    public CharacterProficiencies()
    {
        reset();
    }

    public void initialize(CharacterClass characterClass)
    {
        reset();

        if (characterClass == null)
        {
            return;
        }

        availablePoints = switch (characterClass)
        {
            case FIGHTER -> 4;
            case RANGER, PALADIN, CLERIC, DRUID, THIEF, BARD -> 2;
            case MAGE -> 1;
        };

        maximumRank = characterClass == CharacterClass.FIGHTER ? 2 : 1;
        initialized = true;
    }

    public void reset()
    {
        ranks.clear();

        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            ranks.put(proficiency, 0);
        }

        availablePoints = 0;
        maximumRank = 0;
        initialized = false;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public int getAvailablePoints()
    {
        return availablePoints;
    }

    public int getRank(WeaponProficiency proficiency)
    {
        return ranks.getOrDefault(proficiency, 0);
    }

    public int getMaximumRank()
    {
        return maximumRank;
    }

    public boolean canUse(
            CharacterClass characterClass,
            WeaponProficiency proficiency)
    {
        if (characterClass == null || proficiency == null)
        {
            return false;
        }

        return switch (characterClass)
        {
            case FIGHTER -> true;

            case RANGER, PALADIN ->
                    proficiency != WeaponProficiency.MISSILE_WEAPONS;

            case CLERIC ->
                    proficiency == WeaponProficiency.BLUNT_WEAPONS
                    || proficiency == WeaponProficiency.SPIKED_WEAPONS;

            case DRUID ->
                    proficiency == WeaponProficiency.SMALL_SWORD
                    || proficiency == WeaponProficiency.SPEAR
                    || proficiency == WeaponProficiency.BLUNT_WEAPONS
                    || proficiency == WeaponProficiency.MISSILE_WEAPONS;

            case MAGE ->
                    proficiency == WeaponProficiency.SMALL_SWORD
                    || proficiency == WeaponProficiency.MISSILE_WEAPONS;

            case THIEF ->
                    proficiency == WeaponProficiency.SMALL_SWORD
                    || proficiency == WeaponProficiency.BOW
                    || proficiency == WeaponProficiency.BLUNT_WEAPONS
                    || proficiency == WeaponProficiency.MISSILE_WEAPONS;

            case BARD ->
                    proficiency == WeaponProficiency.LARGE_SWORD
                    || proficiency == WeaponProficiency.SMALL_SWORD
                    || proficiency == WeaponProficiency.BOW
                    || proficiency == WeaponProficiency.SPEAR
                    || proficiency == WeaponProficiency.AXE
                    || proficiency == WeaponProficiency.BLUNT_WEAPONS
                    || proficiency == WeaponProficiency.MISSILE_WEAPONS;
        };
    }

    public void increase(
            CharacterClass characterClass,
            WeaponProficiency proficiency)
    {
        if (availablePoints <= 0
                || !canUse(characterClass, proficiency))
        {
            return;
        }

        int currentRank = getRank(proficiency);

        if (currentRank >= maximumRank)
        {
            return;
        }

        ranks.put(proficiency, currentRank + 1);
        availablePoints--;
    }

    public void decrease(WeaponProficiency proficiency)
    {
        int currentRank = getRank(proficiency);

        if (currentRank <= 0)
        {
            return;
        }

        ranks.put(proficiency, currentRank - 1);
        availablePoints++;
    }
}
