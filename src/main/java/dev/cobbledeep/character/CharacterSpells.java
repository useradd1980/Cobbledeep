package dev.cobbledeep.character;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class CharacterSpells
{
    public static final int STARTING_MAGE_KNOWN_SPELLS = 2;
    public static final int STARTING_MAGE_MEMORIZED_SPELLS = 1;

    private final EnumSet<MageSpell> knownMageSpells = EnumSet.noneOf(MageSpell.class);
    private MageSpell memorizedMageSpell;

    public void reset()
    {
        knownMageSpells.clear();
        memorizedMageSpell = null;
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
}
