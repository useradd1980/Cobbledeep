package dev.cobbledeep.character;

public class CharacterSkills
{
    private static final int SKILL_INCREMENT = 5;

    private int openLocks;
    private int findTraps;
    private int pickPockets;
    private int moveSilently;
    private int hideInShadows;
    private int detectIllusion;
    private int setTraps;

    /*
     * These remember the minimum starting values after
     * race + Dexterity have been applied.
     *
     * The player can never reclaim these points.
     */
    private int baseOpenLocks;
    private int baseFindTraps;
    private int basePickPockets;
    private int baseMoveSilently;
    private int baseHideInShadows;
    private int baseDetectIllusion;
    private int baseSetTraps;

    private int availablePoints;

    private boolean initialized;

    public void initialize(
            CharacterRace race,
            CharacterClass characterClass,
            AbilityScores abilityScores)
    {
        reset();

        if (characterClass != CharacterClass.THIEF)
        {
            return;
        }

        int dexterity =
                abilityScores.getFinalDexterity(race);

        /*
         * Baldur's Gate racial starting values.
         */
        switch (race)
        {
            case HUMAN:
                baseOpenLocks = 10;
                baseFindTraps = 5;
                basePickPockets = 15;
                baseMoveSilently = 10;
                baseHideInShadows = 5;
                baseDetectIllusion = 0;
                baseSetTraps = 0;
                break;

            case DWARF:
                baseOpenLocks = 20;
                baseFindTraps = 20;
                basePickPockets = 15;
                baseMoveSilently = 10;
                baseHideInShadows = 5;
                baseDetectIllusion = 5;
                baseSetTraps = 10;
                break;

            case ELF:
                baseOpenLocks = 5;
                baseFindTraps = 5;
                basePickPockets = 20;
                baseMoveSilently = 15;
                baseHideInShadows = 15;
                baseDetectIllusion = 0;
                baseSetTraps = 0;
                break;

            case GNOME:
                baseOpenLocks = 15;
                baseFindTraps = 15;
                basePickPockets = 15;
                baseMoveSilently = 15;
                baseHideInShadows = 10;
                baseDetectIllusion = 10;
                baseSetTraps = 5;
                break;

            case HALF_ELF:
                baseOpenLocks = 10;
                baseFindTraps = 5;
                basePickPockets = 25;
                baseMoveSilently = 10;
                baseHideInShadows = 10;
                baseDetectIllusion = 0;
                baseSetTraps = 0;
                break;

            case HALFLING:
                baseOpenLocks = 15;
                baseFindTraps = 10;
                basePickPockets = 20;
                baseMoveSilently = 20;
                baseHideInShadows = 20;
                baseDetectIllusion = 0;
                baseSetTraps = 0;
                break;
        }

        /*
         * Add Dexterity modifiers.
         */
        baseOpenLocks +=
                getDexterityOpenLocksModifier(
                        dexterity);

        baseFindTraps +=
                getDexterityFindTrapsModifier(
                        dexterity);

        basePickPockets +=
                getDexterityPickPocketsModifier(
                        dexterity);

        baseMoveSilently +=
                getDexterityMoveSilentlyModifier(
                        dexterity);

        baseHideInShadows +=
                getDexterityHideInShadowsModifier(
                        dexterity);

        /*
         * Detect Illusion receives no Dexterity
         * adjustment.
         */

        baseSetTraps +=
                getDexteritySetTrapsModifier(
                        dexterity);

        /*
         * Never allow a starting skill below zero.
         */
        baseOpenLocks =
                Math.max(0, baseOpenLocks);

        baseFindTraps =
                Math.max(0, baseFindTraps);

        basePickPockets =
                Math.max(0, basePickPockets);

        baseMoveSilently =
                Math.max(0, baseMoveSilently);

        baseHideInShadows =
                Math.max(0, baseHideInShadows);

        baseDetectIllusion =
                Math.max(0, baseDetectIllusion);

        baseSetTraps =
                Math.max(0, baseSetTraps);

        /*
         * Current scores begin at their calculated
         * racial + Dexterity starting values.
         */
        openLocks = baseOpenLocks;
        findTraps = baseFindTraps;
        pickPockets = basePickPockets;
        moveSilently = baseMoveSilently;
        hideInShadows = baseHideInShadows;
        detectIllusion = baseDetectIllusion;
        setTraps = baseSetTraps;

        /*
         * Baldur's Gate Thief:
         * 40 assignable points at level 1.
         *
         * Points are allocated in increments of 5.
         */
        availablePoints = 40;

        initialized = true;
    }

    public void reset()
    {
        openLocks = 0;
        findTraps = 0;
        pickPockets = 0;
        moveSilently = 0;
        hideInShadows = 0;
        detectIllusion = 0;
        setTraps = 0;

        baseOpenLocks = 0;
        baseFindTraps = 0;
        basePickPockets = 0;
        baseMoveSilently = 0;
        baseHideInShadows = 0;
        baseDetectIllusion = 0;
        baseSetTraps = 0;

        availablePoints = 0;

        initialized = false;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    public int getOpenLocks()
    {
        return openLocks;
    }

    public int getFindTraps()
    {
        return findTraps;
    }

    public int getPickPockets()
    {
        return pickPockets;
    }

    public int getMoveSilently()
    {
        return moveSilently;
    }

    public int getHideInShadows()
    {
        return hideInShadows;
    }

    public int getDetectIllusion()
    {
        return detectIllusion;
    }

    public int getSetTraps()
    {
        return setTraps;
    }

    public int getAvailablePoints()
    {
        return availablePoints;
    }

    // -------------------------------------------------
    // OPEN LOCKS
    // -------------------------------------------------

    public void increaseOpenLocks()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            openLocks += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreaseOpenLocks()
    {
        if (openLocks >= baseOpenLocks + SKILL_INCREMENT)
        {
            openLocks -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // FIND TRAPS
    // -------------------------------------------------

    public void increaseFindTraps()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            findTraps += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreaseFindTraps()
    {
        if (findTraps >= baseFindTraps + SKILL_INCREMENT)
        {
            findTraps -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // PICK POCKETS
    // -------------------------------------------------

    public void increasePickPockets()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            pickPockets += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreasePickPockets()
    {
        if (pickPockets >= basePickPockets + SKILL_INCREMENT)
        {
            pickPockets -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // MOVE SILENTLY
    // -------------------------------------------------

    public void increaseMoveSilently()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            moveSilently += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreaseMoveSilently()
    {
        if (moveSilently >= baseMoveSilently + SKILL_INCREMENT)
        {
            moveSilently -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // HIDE IN SHADOWS
    // -------------------------------------------------

    public void increaseHideInShadows()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            hideInShadows += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreaseHideInShadows()
    {
        if (hideInShadows >= baseHideInShadows + SKILL_INCREMENT)
        {
            hideInShadows -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // DETECT ILLUSION
    // -------------------------------------------------

    public void increaseDetectIllusion()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            detectIllusion += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreaseDetectIllusion()
    {
        if (detectIllusion >=
                baseDetectIllusion + SKILL_INCREMENT)
        {
            detectIllusion -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // SET TRAPS
    // -------------------------------------------------

    public void increaseSetTraps()
    {
        if (availablePoints >= SKILL_INCREMENT)
        {
            setTraps += SKILL_INCREMENT;
            availablePoints -= SKILL_INCREMENT;
        }
    }

    public void decreaseSetTraps()
    {
        if (setTraps >= baseSetTraps + SKILL_INCREMENT)
        {
            setTraps -= SKILL_INCREMENT;
            availablePoints += SKILL_INCREMENT;
        }
    }

    // -------------------------------------------------
    // DEXTERITY MODIFIERS
    // -------------------------------------------------

    private int getDexterityOpenLocksModifier(
            int dexterity)
    {
        return switch (dexterity)
        {
            case 9 -> -10;
            case 10 -> -5;
            case 11, 12, 13, 14, 15 -> 0;
            case 16 -> 5;
            case 17 -> 10;
            case 18 -> 15;
            case 19 -> 20;
            case 20 -> 25;
            case 21 -> 30;
            case 22 -> 35;
            case 23 -> 40;
            case 24 -> 45;
            default ->
                dexterity >= 25
                        ? 50
                        : -10;
        };
    }

    private int getDexterityPickPocketsModifier(
            int dexterity)
    {
        return switch (dexterity)
        {
            case 9 -> -15;
            case 10 -> -10;
            case 11 -> -5;
            case 12, 13, 14, 15, 16 -> 0;
            case 17 -> 5;
            case 18 -> 10;
            case 19 -> 15;
            case 20 -> 20;
            case 21 -> 25;
            case 22 -> 30;
            case 23 -> 35;
            case 24 -> 40;
            default ->
                dexterity >= 25
                        ? 45
                        : -15;
        };
    }

    private int getDexterityMoveSilentlyModifier(
            int dexterity)
    {
        return switch (dexterity)
        {
            case 9 -> -20;
            case 10 -> -15;
            case 11 -> -10;
            case 12 -> -5;
            case 13, 14, 15, 16 -> 0;
            case 17 -> 5;
            case 18 -> 10;
            case 19 -> 15;
            case 20 -> 18;
            case 21 -> 20;
            case 22 -> 23;
            case 23 -> 25;
            case 24 -> 30;
            default ->
                dexterity >= 25
                        ? 35
                        : -20;
        };
    }

    private int getDexterityHideInShadowsModifier(
            int dexterity)
    {
        return switch (dexterity)
        {
            case 9 -> -10;
            case 10 -> -5;
            case 11, 12, 13, 14, 15, 16 -> 0;
            case 17 -> 5;
            case 18 -> 10;
            case 19 -> 15;
            case 20 -> 18;
            case 21 -> 20;
            case 22 -> 23;
            case 23 -> 25;
            case 24 -> 30;
            default ->
                dexterity >= 25
                        ? 35
                        : -10;
        };
    }

    private int getDexterityFindTrapsModifier(
            int dexterity)
    {
        return switch (dexterity)
        {
            case 9, 10 -> -10;
            case 11, 12 -> -5;
            case 13, 14, 15, 16, 17 -> 0;
            case 18 -> 5;
            case 19 -> 10;
            case 20 -> 15;
            case 21 -> 20;
            case 22 -> 25;
            case 23 -> 30;
            case 24 -> 35;
            default ->
                dexterity >= 25
                        ? 40
                        : -10;
        };
    }

    private int getDexteritySetTrapsModifier(
            int dexterity)
    {
        return switch (dexterity)
        {
            case 9 -> -10;
            case 10, 11 -> -5;
            case 12, 13, 14, 15, 16, 17 -> 0;
            case 18 -> 5;
            case 19 -> 10;
            case 20 -> 15;
            case 21 -> 20;
            case 22 -> 25;
            case 23 -> 30;
            case 24 -> 35;
            default ->
                dexterity >= 25
                        ? 40
                        : -10;
        };
    }
}