package dev.cobbledeep.character;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class CharacterSpells
{
    public static final int STARTING_MAGE_KNOWN_SPELLS = 2;
    public static final int STARTING_MAGE_MEMORIZED_SPELLS = 1;

    private final EnumSet<MageSpell> knownMageSpells = EnumSet.noneOf(MageSpell.class);
    private MageSpell memorizedMageSpell;

    private final EnumMap<DivineSpell, Integer> memorizedDivineSpells =
            new EnumMap<>(DivineSpell.class);

    public void reset()
    {
        knownMageSpells.clear();
        memorizedMageSpell = null;
        memorizedDivineSpells.clear();
    }

    public Set<MageSpell> getKnownMageSpells()
    {
        return Collections.unmodifiableSet(knownMageSpells);
    }

    public int getKnownMageSpellCount()
    {
        return knownMageSpells.size();
    }

    public boolean knowsMageSpell(MageSpell spell)
    {
        return spell != null && knownMageSpells.contains(spell);
    }

    public boolean canLearnAnotherMageSpell()
    {
        return knownMageSpells.size() < STARTING_MAGE_KNOWN_SPELLS;
    }

    public void toggleKnownMageSpell(MageSpell spell)
    {
        if (spell == null)
        {
            return;
        }

        if (knownMageSpells.contains(spell))
        {
            knownMageSpells.remove(spell);
            if (memorizedMageSpell == spell)
            {
                memorizedMageSpell = null;
            }
            return;
        }

        if (canLearnAnotherMageSpell())
        {
            knownMageSpells.add(spell);
        }
    }

    public MageSpell getMemorizedMageSpell()
    {
        return memorizedMageSpell;
    }

    public void setMemorizedMageSpell(MageSpell spell)
    {
        if (spell != null && knownMageSpells.contains(spell))
        {
            memorizedMageSpell = spell;
        }
    }

    public boolean isMageSelectionComplete()
    {
        return knownMageSpells.size() == STARTING_MAGE_KNOWN_SPELLS
                && memorizedMageSpell != null;
    }

    public Map<DivineSpell, Integer> getMemorizedDivineSpells()
    {
        return Collections.unmodifiableMap(memorizedDivineSpells);
    }

    public int getMemorizedDivineSpellCount(DivineSpell spell)
    {
        return memorizedDivineSpells.getOrDefault(spell, 0);
    }

    public int getTotalMemorizedDivineSpells()
    {
        int total = 0;
        for (int count : memorizedDivineSpells.values())
        {
            total += count;
        }
        return total;
    }

    public int getStartingDivineSpellSlots(CharacterClass characterClass, int finalWisdom)
    {
        if (characterClass != CharacterClass.CLERIC && characterClass != CharacterClass.DRUID)
        {
            return 0;
        }

        int bonusSlots = 0;
        if (finalWisdom >= 14)
        {
            bonusSlots = 2;
        }
        else if (finalWisdom >= 13)
        {
            bonusSlots = 1;
        }

        return 1 + bonusSlots;
    }

    public boolean canMemorizeAnotherDivineSpell(
            CharacterClass characterClass,
            int finalWisdom)
    {
        return getTotalMemorizedDivineSpells()
                < getStartingDivineSpellSlots(characterClass, finalWisdom);
    }

    public void increaseDivineSpell(
            CharacterClass characterClass,
            int finalWisdom,
            DivineSpell spell)
    {
        if (spell == null
                || !spell.isAvailableTo(characterClass)
                || !canMemorizeAnotherDivineSpell(characterClass, finalWisdom))
        {
            return;
        }

        memorizedDivineSpells.merge(spell, 1, Integer::sum);
    }

    public void decreaseDivineSpell(DivineSpell spell)
    {
        if (spell == null)
        {
            return;
        }

        int count = getMemorizedDivineSpellCount(spell);
        if (count <= 1)
        {
            memorizedDivineSpells.remove(spell);
        }
        else
        {
            memorizedDivineSpells.put(spell, count - 1);
        }
    }

    public boolean isDivineSelectionComplete(
            CharacterClass characterClass,
            int finalWisdom)
    {
        int slots = getStartingDivineSpellSlots(characterClass, finalWisdom);
        return slots > 0 && getTotalMemorizedDivineSpells() == slots;
    }
}
