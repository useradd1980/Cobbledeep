package dev.cobbledeep.character;

import java.util.concurrent.ThreadLocalRandom;

public class AbilityScores
{
    private int strength;
    private int dexterity;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;

    private int availablePoints;
    private int exceptionalStrength;

    private boolean rolled;

    // -------------------------------------------------
    // STORED ROLL
    // -------------------------------------------------

    private int storedStrength;
    private int storedDexterity;
    private int storedConstitution;
    private int storedIntelligence;
    private int storedWisdom;
    private int storedCharisma;

    private int storedAvailablePoints;
    private int storedExceptionalStrength;

    private boolean hasStoredRoll;

    // -------------------------------------------------
    // ROLLING
    // -------------------------------------------------

    public void roll(
            CharacterRace race,
            CharacterClass characterClass)
    {
        do
        {
            strength = rollQualifiedScore(
                    CharacterAbilityRules.getMinimumStrength(
                            race,
                            characterClass),
                    CharacterAbilityRules.getMaximumStrength(race)
            );

            dexterity = rollQualifiedScore(
                    CharacterAbilityRules.getMinimumDexterity(
                            race,
                            characterClass),
                    CharacterAbilityRules.getMaximumDexterity(race)
            );

            constitution = rollQualifiedScore(
                    CharacterAbilityRules.getMinimumConstitution(
                            race,
                            characterClass),
                    CharacterAbilityRules.getMaximumConstitution(race)
            );

            intelligence = rollQualifiedScore(
                    CharacterAbilityRules.getMinimumIntelligence(
                            race,
                            characterClass),
                    CharacterAbilityRules.getMaximumIntelligence(race)
            );

            wisdom = rollQualifiedScore(
                    CharacterAbilityRules.getMinimumWisdom(
                            race,
                            characterClass),
                    CharacterAbilityRules.getMaximumWisdom(race)
            );

            charisma = rollQualifiedScore(
                    CharacterAbilityRules.getMinimumCharisma(
                            race,
                            characterClass),
                    CharacterAbilityRules.getMaximumCharisma(race)
            );
        }
        while (getBaseTotal() < 75);

        availablePoints = 0;
        exceptionalStrength = 0;
        rolled = true;

        updateExceptionalStrength(
                race,
                characterClass
        );
    }

    private int rollQualifiedScore(
            int minimum,
            int maximum)
    {
        int result;

        do
        {
            result = roll3d6();
        }
        while (result < minimum
                || result > maximum);

        return result;
    }

    private int roll3d6()
    {
        ThreadLocalRandom random =
                ThreadLocalRandom.current();

        return random.nextInt(1, 7)
                + random.nextInt(1, 7)
                + random.nextInt(1, 7);
    }

    private int getBaseTotal()
    {
        return strength
                + dexterity
                + constitution
                + intelligence
                + wisdom
                + charisma;
    }

    // -------------------------------------------------
    // RESET
    // -------------------------------------------------

    public void reset()
    {
        strength = 0;
        dexterity = 0;
        constitution = 0;
        intelligence = 0;
        wisdom = 0;
        charisma = 0;

        availablePoints = 0;
        exceptionalStrength = 0;

        rolled = false;

        clearStoredRoll();
    }

    // -------------------------------------------------
    // STORE / RECALL
    // -------------------------------------------------

    public void storeCurrentRoll()
    {
        if (!rolled)
        {
            return;
        }

        storedStrength = strength;
        storedDexterity = dexterity;
        storedConstitution = constitution;
        storedIntelligence = intelligence;
        storedWisdom = wisdom;
        storedCharisma = charisma;

        storedAvailablePoints = availablePoints;
        storedExceptionalStrength = exceptionalStrength;

        hasStoredRoll = true;
    }

    public void recallStoredRoll()
    {
        if (!hasStoredRoll)
        {
            return;
        }

        strength = storedStrength;
        dexterity = storedDexterity;
        constitution = storedConstitution;
        intelligence = storedIntelligence;
        wisdom = storedWisdom;
        charisma = storedCharisma;

        availablePoints = storedAvailablePoints;
        exceptionalStrength = storedExceptionalStrength;

        rolled = true;
    }

    public boolean hasStoredRoll()
    {
        return hasStoredRoll;
    }

    public int getStoredTotal()
    {
        if (!hasStoredRoll)
        {
            return 0;
        }

        return storedStrength
                + storedDexterity
                + storedConstitution
                + storedIntelligence
                + storedWisdom
                + storedCharisma
                + storedAvailablePoints;
    }

    private void clearStoredRoll()
    {
        storedStrength = 0;
        storedDexterity = 0;
        storedConstitution = 0;
        storedIntelligence = 0;
        storedWisdom = 0;
        storedCharisma = 0;

        storedAvailablePoints = 0;
        storedExceptionalStrength = 0;

        hasStoredRoll = false;
    }

    // -------------------------------------------------
    // GENERAL GETTERS
    // -------------------------------------------------

    public boolean isRolled()
    {
        return rolled;
    }

    public int getStrength()
    {
        return strength;
    }

    public int getDexterity()
    {
        return dexterity;
    }

    public int getConstitution()
    {
        return constitution;
    }

    public int getIntelligence()
    {
        return intelligence;
    }

    public int getWisdom()
    {
        return wisdom;
    }

    public int getCharisma()
    {
        return charisma;
    }

    // -------------------------------------------------
    // FINAL SCORES
    // -------------------------------------------------

    public int getFinalStrength(
            CharacterRace race)
    {
        return strength
                + CharacterAbilityRules
                        .getStrengthModifier(race);
    }

    public int getFinalDexterity(
            CharacterRace race)
    {
        return dexterity
                + CharacterAbilityRules
                        .getDexterityModifier(race);
    }

    public int getFinalConstitution(
            CharacterRace race)
    {
        return constitution
                + CharacterAbilityRules
                        .getConstitutionModifier(race);
    }

    public int getFinalIntelligence(
            CharacterRace race)
    {
        return intelligence
                + CharacterAbilityRules
                        .getIntelligenceModifier(race);
    }

    public int getFinalWisdom(
            CharacterRace race)
    {
        return wisdom
                + CharacterAbilityRules
                        .getWisdomModifier(race);
    }

    public int getFinalCharisma(
            CharacterRace race)
    {
        return charisma
                + CharacterAbilityRules
                        .getCharismaModifier(race);
    }

    // -------------------------------------------------
    // POINT POOL
    // -------------------------------------------------

    public int getAvailablePoints()
    {
        return availablePoints;
    }

    public int getTotal()
    {
        return strength
                + dexterity
                + constitution
                + intelligence
                + wisdom
                + charisma
                + availablePoints;
    }

    // -------------------------------------------------
    // STRENGTH
    // -------------------------------------------------

    public void decreaseStrength(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int minimum =
                CharacterAbilityRules
                        .getMinimumStrength(
                                race,
                                characterClass);

        if (strength > minimum)
        {
            strength--;
            availablePoints++;

            updateExceptionalStrength(
                    race,
                    characterClass
            );
        }
    }

    public void increaseStrength(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int maximum =
                CharacterAbilityRules
                        .getMaximumStrength(race);

        if (availablePoints > 0
                && strength < maximum)
        {
            strength++;
            availablePoints--;

            updateExceptionalStrength(
                    race,
                    characterClass
            );
        }
    }

    // -------------------------------------------------
    // DEXTERITY
    // -------------------------------------------------

    public void decreaseDexterity(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int minimum =
                CharacterAbilityRules
                        .getMinimumDexterity(
                                race,
                                characterClass);

        if (dexterity > minimum)
        {
            dexterity--;
            availablePoints++;
        }
    }

    public void increaseDexterity(
            CharacterRace race)
    {
        int maximum =
                CharacterAbilityRules
                        .getMaximumDexterity(race);

        if (availablePoints > 0
                && dexterity < maximum)
        {
            dexterity++;
            availablePoints--;
        }
    }

    // -------------------------------------------------
    // CONSTITUTION
    // -------------------------------------------------

    public void decreaseConstitution(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int minimum =
                CharacterAbilityRules
                        .getMinimumConstitution(
                                race,
                                characterClass);

        if (constitution > minimum)
        {
            constitution--;
            availablePoints++;
        }
    }

    public void increaseConstitution(
            CharacterRace race)
    {
        int maximum =
                CharacterAbilityRules
                        .getMaximumConstitution(race);

        if (availablePoints > 0
                && constitution < maximum)
        {
            constitution++;
            availablePoints--;
        }
    }

    // -------------------------------------------------
    // INTELLIGENCE
    // -------------------------------------------------

    public void decreaseIntelligence(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int minimum =
                CharacterAbilityRules
                        .getMinimumIntelligence(
                                race,
                                characterClass);

        if (intelligence > minimum)
        {
            intelligence--;
            availablePoints++;
        }
    }

    public void increaseIntelligence(
            CharacterRace race)
    {
        int maximum =
                CharacterAbilityRules
                        .getMaximumIntelligence(race);

        if (availablePoints > 0
                && intelligence < maximum)
        {
            intelligence++;
            availablePoints--;
        }
    }

    // -------------------------------------------------
    // WISDOM
    // -------------------------------------------------

    public void decreaseWisdom(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int minimum =
                CharacterAbilityRules
                        .getMinimumWisdom(
                                race,
                                characterClass);

        if (wisdom > minimum)
        {
            wisdom--;
            availablePoints++;
        }
    }

    public void increaseWisdom(
            CharacterRace race)
    {
        int maximum =
                CharacterAbilityRules
                        .getMaximumWisdom(race);

        if (availablePoints > 0
                && wisdom < maximum)
        {
            wisdom++;
            availablePoints--;
        }
    }

    // -------------------------------------------------
    // CHARISMA
    // -------------------------------------------------

    public void decreaseCharisma(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int minimum =
                CharacterAbilityRules
                        .getMinimumCharisma(
                                race,
                                characterClass);

        if (charisma > minimum)
        {
            charisma--;
            availablePoints++;
        }
    }

    public void increaseCharisma(
            CharacterRace race)
    {
        int maximum =
                CharacterAbilityRules
                        .getMaximumCharisma(race);

        if (availablePoints > 0
                && charisma < maximum)
        {
            charisma++;
            availablePoints--;
        }
    }

    // -------------------------------------------------
    // EXCEPTIONAL STRENGTH
    // -------------------------------------------------

    public int getExceptionalStrength()
    {
        return exceptionalStrength;
    }

    public void setExceptionalStrength(
            int exceptionalStrength)
    {
        this.exceptionalStrength =
                Math.max(
                        0,
                        Math.min(
                                exceptionalStrength,
                                100
                        )
                );
    }

    private void updateExceptionalStrength(
            CharacterRace race,
            CharacterClass characterClass)
    {
        int finalStrength =
                getFinalStrength(race);

        boolean eligible =
                CharacterAbilityRules
                        .canHaveExceptionalStrength(
                                race,
                                characterClass,
                                finalStrength
                        );

        if (!eligible)
        {
            exceptionalStrength = 0;
            return;
        }

        if (exceptionalStrength == 0)
        {
            exceptionalStrength =
                    ThreadLocalRandom
                            .current()
                            .nextInt(1, 101);
        }
    }
}