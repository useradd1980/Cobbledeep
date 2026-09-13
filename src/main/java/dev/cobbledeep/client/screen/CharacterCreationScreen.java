package dev.cobbledeep.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dev.cobbledeep.character.AbilityScores;
import dev.cobbledeep.character.CharacterAbilityRules;
import dev.cobbledeep.character.CharacterAlignment;
import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterClass;
import dev.cobbledeep.character.CharacterProficiencies;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.CharacterSkills;
import dev.cobbledeep.character.CharacterSpells;
import dev.cobbledeep.character.DivineSpell;
import dev.cobbledeep.character.FightingStyle;
import dev.cobbledeep.character.MageSpell;
import dev.cobbledeep.character.PendingCharacter;
import dev.cobbledeep.character.WeaponProficiency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;

public class CharacterCreationScreen extends Screen
{
    private static final UUID APPEARANCE_PREVIEW_UUID = UUID.fromString("8f8d3f34-1486-4f24-8f8e-fb98f18d759f");

    private final PendingCharacter pendingCharacter;

    private CharacterCreationPage currentPage;
    private Button nextButton;
    private int proficiencyScrollOffset;
    private int spellScrollOffset;
    private MageSpell focusedMageSpell;
    private DivineSpell focusedDivineSpell;
    private PlayerSkinWidget appearanceSkinWidget;

    private record AppearanceColumns(
            int labelX,
            int previousX,
            int valueCenterX,
            int nextX,
            int swatchX,
            int buttonWidth)
    {
    }

    public CharacterCreationScreen()
    {
        super(Component.literal("Character Generation"));
        this.pendingCharacter = new PendingCharacter();
        this.currentPage = CharacterCreationPage.GENDER;
        this.proficiencyScrollOffset = 0;
        this.spellScrollOffset = 0;
        this.focusedMageSpell = MageSpell.ARMOR;
        this.focusedDivineSpell = DivineSpell.ARMOR_OF_FAITH;
    }

    @Override
    protected void init()
    {
        buildCurrentPage();
    }

    private boolean isCompactLayout() { return this.height < 360 || this.width < 560; }
    private int getHeaderY() { return isCompactLayout() ? 16 : 40; }
    private int getPageTitleY() { return isCompactLayout() ? 34 : 70; }
    private int getContentTop() { return isCompactLayout() ? 58 : 95; }
    private int getNavigationY() { return this.height - (isCompactLayout() ? 28 : 40); }
    private int getButtonHeight() { return isCompactLayout() ? 18 : 20; }
    private int getRowHeight() { return isCompactLayout() ? 20 : 22; }
    private int getAbilityRowHeight() { return isCompactLayout() ? 17 : getRowHeight(); }
    private int getDescriptionWidth() { return Math.max(180, Math.min(360, this.width - 30)); }

    private int getAbilityActionY(int startY)
    {
        return isCompactLayout()
                ? getNavigationY() - getButtonHeight() - 4
                : startY + 6 * getAbilityRowHeight() + 38;
    }

    private int getAbilityInfoY(int startY)
    {
        int naturalY = startY + getAbilityRowHeight() * 6 + (isCompactLayout() ? 1 : 7);
        return isCompactLayout() ? Math.min(naturalY, getAbilityActionY(startY) - 27) : naturalY;
    }

    private void buildCurrentPage()
    {
        this.clearWidgets();
        appearanceSkinWidget = null;

        switch (currentPage)
        {
            case GENDER -> buildGenderPage();
            case RACE -> buildRacePage();
            case CLASS -> buildClassPage();
            case ALIGNMENT -> buildAlignmentPage();
            case ABILITIES -> buildAbilitiesPage();
            case SKILLS -> buildSkillsPage();
            case PROFICIENCIES -> buildProficienciesPage();
            case SPELLS -> buildSpellsPage();
            case APPEARANCE -> buildAppearancePage();
            case NAME, REVIEW -> buildPlaceholderPage();
        }

        buildNavigationButtons();
    }

    private void buildGenderPage()
    {
        int centerX = this.width / 2;
        int y = Math.max(getContentTop() + 20, this.height / 2 - 10);
        int gap = 10;
        int buttonWidth = Math.min(100, Math.max(70, (this.width - 30 - gap) / 2));
        int buttonHeight = getButtonHeight();

        this.addRenderableWidget(Button.builder(Component.literal("Male"), button ->
        {
            pendingCharacter.setGender(PendingCharacter.Gender.MALE);
            updateNextButton();
        }).bounds(centerX - gap / 2 - buttonWidth, y, buttonWidth, buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal("Female"), button ->
        {
            pendingCharacter.setGender(PendingCharacter.Gender.FEMALE);
            updateNextButton();
        }).bounds(centerX + gap / 2, y, buttonWidth, buttonHeight).build());
    }

    private void buildRacePage()
    {
        int centerX = this.width / 2;
        CharacterRace[] races = CharacterRace.values();
        int columns = this.width < 340 ? 1 : 2;
        int gap = 8;
        int buttonHeight = getButtonHeight();
        int buttonWidth = columns == 1
                ? Math.min(130, this.width - 30)
                : Math.min(110, Math.max(80, (this.width - 30 - gap) / 2));
        int totalWidth = columns * buttonWidth + (columns - 1) * gap;
        int startX = centerX - totalWidth / 2;
        int startY = getContentTop();
        int rowGap = isCompactLayout() ? 4 : 8;

        for (int i = 0; i < races.length; i++)
        {
            CharacterRace race = races[i];
            int x = startX + (i % columns) * (buttonWidth + gap);
            int y = startY + (i / columns) * (buttonHeight + rowGap);

            this.addRenderableWidget(Button.builder(Component.literal(race.getDisplayName()), button ->
            {
                if (pendingCharacter.getRace() != race)
                {
                    pendingCharacter.setRace(race);
                    pendingCharacter.setCharacterClass(null);
                    pendingCharacter.setAlignment(null);
                    pendingCharacter.getAbilityScores().reset();
                    pendingCharacter.getSkills().reset();
                    pendingCharacter.getProficiencies().reset();
                    pendingCharacter.getSpells().reset();
                    pendingCharacter.getAppearance().reset();
                    proficiencyScrollOffset = 0;
                    spellScrollOffset = 0;
                }
                updateNextButton();
            }).bounds(x, y, buttonWidth, buttonHeight).build());
        }
    }

    private void buildClassPage()
    {
        CharacterRace race = pendingCharacter.getRace();
        if (race == null) return;

        int centerX = this.width / 2;
        int columns = this.width < 340 ? 1 : 2;
        int gap = 8;
        int buttonHeight = getButtonHeight();
        int buttonWidth = columns == 1
                ? Math.min(130, this.width - 30)
                : Math.min(110, Math.max(80, (this.width - 30 - gap) / 2));
        int totalWidth = columns * buttonWidth + (columns - 1) * gap;
        int startX = centerX - totalWidth / 2;
        int startY = getContentTop();
        int rowGap = isCompactLayout() ? 3 : 8;
        int visibleIndex = 0;

        for (CharacterClass characterClass : CharacterClass.values())
        {
            if (!race.canChooseClass(characterClass)) continue;
            int x = startX + (visibleIndex % columns) * (buttonWidth + gap);
            int y = startY + (visibleIndex / columns) * (buttonHeight + rowGap);

            this.addRenderableWidget(Button.builder(Component.literal(characterClass.getDisplayName()), button ->
            {
                if (pendingCharacter.getCharacterClass() != characterClass)
                {
                    pendingCharacter.setCharacterClass(characterClass);
                    pendingCharacter.resetAfterClassChange();
                    proficiencyScrollOffset = 0;
                    spellScrollOffset = 0;
                }
                updateNextButton();
            }).bounds(x, y, buttonWidth, buttonHeight).build());
            visibleIndex++;
        }
    }

    private void buildAlignmentPage()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (characterClass == null) return;

        int centerX = this.width / 2;
        int gap = isCompactLayout() ? 4 : 10;
        int buttonHeight = getButtonHeight();
        int buttonWidth = Math.min(120, Math.max(72, (this.width - 30 - gap * 2) / 3));
        int totalWidth = buttonWidth * 3 + gap * 2;
        int startX = centerX - totalWidth / 2;
        int startY = getContentTop();
        int rowGap = isCompactLayout() ? 4 : 10;
        CharacterAlignment[] alignments = CharacterAlignment.values();

        for (int i = 0; i < alignments.length; i++)
        {
            CharacterAlignment alignment = alignments[i];
            int x = startX + (i % 3) * (buttonWidth + gap);
            int y = startY + (i / 3) * (buttonHeight + rowGap);

            Button alignmentButton = Button.builder(Component.literal(alignment.getDisplayName()), button ->
            {
                if (pendingCharacter.getAlignment() != alignment)
                {
                    pendingCharacter.setAlignment(alignment);
                    pendingCharacter.resetAfterAlignmentChange();
                }
                updateNextButton();
            }).bounds(x, y, buttonWidth, buttonHeight).build();
            alignmentButton.active = characterClass.canChooseAlignment(alignment);
            this.addRenderableWidget(alignmentButton);
        }
    }

    private void buildAbilitiesPage()
    {
        int centerX = this.width / 2;
        int startY = getContentTop() + (isCompactLayout() ? 4 : 17);
        int rowHeight = getAbilityRowHeight();
        int buttonHeight = getButtonHeight();
        AbilityScores scores = pendingCharacter.getAbilityScores();
        CharacterRace race = pendingCharacter.getRace();
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (race == null || characterClass == null) return;

        if (!scores.isRolled())
        {
            scores.roll(race, characterClass);
            pendingCharacter.resetAfterAbilitiesChange();
        }

        int actionY = getAbilityActionY(startY);
        int actionGap = 5;
        int actionWidth = Math.min(100, Math.max(62, (this.width - 30 - actionGap * 2) / 3));

        Button storeButton = Button.builder(Component.literal("Store"), button ->
        {
            scores.storeCurrentRoll();
            buildCurrentPage();
        }).bounds(centerX - actionWidth - actionGap - actionWidth / 2, actionY, actionWidth, buttonHeight).build();
        storeButton.active = scores.isRolled();
        this.addRenderableWidget(storeButton);

        this.addRenderableWidget(Button.builder(Component.literal("Reroll"), button ->
        {
            scores.roll(race, characterClass);
            pendingCharacter.resetAfterAbilitiesChange();
            buildCurrentPage();
        }).bounds(centerX - actionWidth / 2, actionY, actionWidth, buttonHeight).build());

        Button recallButton = Button.builder(Component.literal("Recall"), button ->
        {
            scores.recallStoredRoll();
            pendingCharacter.resetAfterAbilitiesChange();
            buildCurrentPage();
        }).bounds(centerX + actionWidth / 2 + actionGap, actionY, actionWidth, buttonHeight).build();
        recallButton.active = scores.hasStoredRoll();
        this.addRenderableWidget(recallButton);

        addAbilityButtons(centerX, startY, () -> scores.decreaseStrength(race, characterClass), () -> scores.increaseStrength(race, characterClass));
        addAbilityButtons(centerX, startY + rowHeight, () -> scores.decreaseDexterity(race, characterClass), () -> scores.increaseDexterity(race));
        addAbilityButtons(centerX, startY + rowHeight * 2, () -> scores.decreaseConstitution(race, characterClass), () -> scores.increaseConstitution(race));
        addAbilityButtons(centerX, startY + rowHeight * 3, () -> scores.decreaseIntelligence(race, characterClass), () -> scores.increaseIntelligence(race));
        addAbilityButtons(centerX, startY + rowHeight * 4, () -> scores.decreaseWisdom(race, characterClass), () -> scores.increaseWisdom(race));
        addAbilityButtons(centerX, startY + rowHeight * 5, () -> scores.decreaseCharisma(race, characterClass), () -> scores.increaseCharisma(race));
    }

    private void addAbilityButtons(int centerX, int y, Runnable decrease, Runnable increase)
    {
        int size = isCompactLayout() ? 16 : getButtonHeight();
        this.addRenderableWidget(Button.builder(Component.literal("-"), button ->
        {
            int oldPool = pendingCharacter.getAbilityScores().getAvailablePoints();
            decrease.run();
            if (pendingCharacter.getAbilityScores().getAvailablePoints() != oldPool)
            {
                pendingCharacter.resetAfterAbilitiesChange();
            }
            buildCurrentPage();
        }).bounds(centerX - 20, y, size, size).build());

        this.addRenderableWidget(Button.builder(Component.literal("+"), button ->
        {
            int oldPool = pendingCharacter.getAbilityScores().getAvailablePoints();
            increase.run();
            if (pendingCharacter.getAbilityScores().getAvailablePoints() != oldPool)
            {
                pendingCharacter.resetAfterAbilitiesChange();
            }
            buildCurrentPage();
        }).bounds(centerX + 20, y, size, size).build());
    }

    private void buildSkillsPage()
    {
        int centerX = this.width / 2;
        int startY = getContentTop() + 5;
        int rowHeight = getRowHeight();
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        CharacterRace race = pendingCharacter.getRace();
        AbilityScores abilityScores = pendingCharacter.getAbilityScores();
        CharacterSkills skills = pendingCharacter.getSkills();
        if (characterClass == null || race == null || !abilityScores.isRolled()) return;
        if (!skills.isInitialized()) skills.initialize(race, characterClass, abilityScores);
        if (characterClass != CharacterClass.THIEF) return;

        addSkillButtons(centerX, startY, skills::decreaseOpenLocks, skills::increaseOpenLocks);
        addSkillButtons(centerX, startY + rowHeight, skills::decreaseFindTraps, skills::increaseFindTraps);
        addSkillButtons(centerX, startY + rowHeight * 2, skills::decreasePickPockets, skills::increasePickPockets);
        addSkillButtons(centerX, startY + rowHeight * 3, skills::decreaseMoveSilently, skills::increaseMoveSilently);
        addSkillButtons(centerX, startY + rowHeight * 4, skills::decreaseHideInShadows, skills::increaseHideInShadows);
        addSkillButtons(centerX, startY + rowHeight * 5, skills::decreaseDetectIllusion, skills::increaseDetectIllusion);
        addSkillButtons(centerX, startY + rowHeight * 6, skills::decreaseSetTraps, skills::increaseSetTraps);
    }

    private void addSkillButtons(int centerX, int y, Runnable decrease, Runnable increase)
    {
        int size = getButtonHeight();
        this.addRenderableWidget(Button.builder(Component.literal("-"), button ->
        {
            decrease.run();
            buildCurrentPage();
        }).bounds(centerX + 35, y, size, size).build());

        this.addRenderableWidget(Button.builder(Component.literal("+"), button ->
        {
            increase.run();
            buildCurrentPage();
        }).bounds(centerX + 60, y, size, size).build());
    }

    private int getProficiencyStartY() { return getContentTop() + 12; }

    private void buildProficienciesPage()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        CharacterProficiencies proficiencies = pendingCharacter.getProficiencies();
        if (characterClass == null) return;
        if (!proficiencies.isInitialized()) proficiencies.initialize(characterClass);
        clampProficiencyScroll();

        int centerX = this.width / 2;
        int panelWidth = Math.min(360, this.width - 24);
        int controlsX = centerX + panelWidth / 2 - 58;
        int firstRow = proficiencyScrollOffset;
        int lastRow = firstRow + getProficiencyVisibleRows() - 1;
        int weaponCount = WeaponProficiency.values().length;

        for (int i = 0; i < weaponCount; i++)
        {
            if (i < firstRow || i > lastRow) continue;
            WeaponProficiency proficiency = WeaponProficiency.values()[i];
            if (!proficiencies.canUseWeapon(characterClass, proficiency)) continue;
            addProficiencyButtons(characterClass, proficiencies, proficiency, controlsX, getProficiencyRowY(i));
        }

        int styleHeaderRow = weaponCount;
        FightingStyle[] styles = FightingStyle.values();
        for (int i = 0; i < styles.length; i++)
        {
            int logicalRow = styleHeaderRow + 1 + i;
            if (logicalRow < firstRow || logicalRow > lastRow) continue;
            FightingStyle style = styles[i];
            if (!proficiencies.canUseStyle(characterClass, style)) continue;

            int y = getProficiencyRowY(logicalRow);
            int minimumRank = characterClass == CharacterClass.RANGER && style == FightingStyle.TWO_WEAPON ? 2 : 0;
            int size = getButtonHeight();

            Button minusButton = Button.builder(Component.literal("-"), button ->
            {
                proficiencies.decreaseStyle(characterClass, style);
                buildCurrentPage();
            }).bounds(controlsX, y, size, size).build();
            minusButton.active = proficiencies.getStyleRank(style) > minimumRank;
            this.addRenderableWidget(minusButton);

            Button plusButton = Button.builder(Component.literal("+"), button ->
            {
                proficiencies.increaseStyle(characterClass, style);
                buildCurrentPage();
            }).bounds(controlsX + size + 5, y, size, size).build();
            plusButton.active = proficiencies.getAvailablePoints() > 0
                    && proficiencies.getStyleRank(style) < proficiencies.getMaximumStyleRank(characterClass, style);
            this.addRenderableWidget(plusButton);
        }
    }

    private void addProficiencyButtons(
            CharacterClass characterClass,
            CharacterProficiencies proficiencies,
            WeaponProficiency proficiency,
            int controlsX,
            int y)
    {
        int size = getButtonHeight();
        Button minusButton = Button.builder(Component.literal("-"), button ->
        {
            proficiencies.decreaseWeapon(proficiency);
            buildCurrentPage();
        }).bounds(controlsX, y, size, size).build();
        minusButton.active = proficiencies.getWeaponRank(proficiency) > 0;
        this.addRenderableWidget(minusButton);

        Button plusButton = Button.builder(Component.literal("+"), button ->
        {
            proficiencies.increaseWeapon(characterClass, proficiency);
            buildCurrentPage();
        }).bounds(controlsX + size + 5, y, size, size).build();
        plusButton.active = proficiencies.getAvailablePoints() > 0
                && proficiencies.getWeaponRank(proficiency) < proficiencies.getMaximumWeaponRank(characterClass);
        this.addRenderableWidget(plusButton);
    }

    private int getProficiencyVisibleRows()
    {
        return Math.max(3, Math.min(12, (getNavigationY() - getProficiencyStartY() - 42) / getRowHeight()));
    }

    private int getProficiencyContentRows() { return WeaponProficiency.values().length + 1 + FightingStyle.values().length; }
    private int getMaxProficiencyScroll() { return Math.max(0, getProficiencyContentRows() - getProficiencyVisibleRows()); }
    private void clampProficiencyScroll() { proficiencyScrollOffset = Math.max(0, Math.min(proficiencyScrollOffset, getMaxProficiencyScroll())); }
    private int getProficiencyRowY(int logicalRow) { return getProficiencyStartY() + (logicalRow - proficiencyScrollOffset) * getRowHeight(); }

    private int getSpellStartY() { return getContentTop() + 8; }

    private int getSpellVisibleRows()
    {
        int reservedBelowList = isCompactLayout() ? 112 : 132;
        int availableHeight = getNavigationY() - getSpellStartY() - reservedBelowList;
        return Math.max(3, Math.min(8, availableHeight / getRowHeight()));
    }

    private int getSpellContentRows()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (characterClass == CharacterClass.MAGE) return MageSpell.values().length;
        if (characterClass == CharacterClass.CLERIC || characterClass == CharacterClass.DRUID)
        {
            return getAvailableDivineSpells(characterClass).size();
        }
        return 0;
    }

    private int getMaxSpellScroll() { return Math.max(0, getSpellContentRows() - getSpellVisibleRows()); }
    private void clampSpellScroll() { spellScrollOffset = Math.max(0, Math.min(spellScrollOffset, getMaxSpellScroll())); }
    private int getSpellRowY(int logicalRow) { return getSpellStartY() + (logicalRow - spellScrollOffset) * getRowHeight(); }

    private List<DivineSpell> getAvailableDivineSpells(CharacterClass characterClass)
    {
        List<DivineSpell> spells = new ArrayList<>();
        for (DivineSpell spell : DivineSpell.values())
        {
            if (spell.isAvailableTo(characterClass)) spells.add(spell);
        }
        return spells;
    }

    private void buildSpellsPage()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (characterClass == CharacterClass.MAGE)
        {
            buildMageSpellsPage();
        }
        else if (characterClass == CharacterClass.CLERIC || characterClass == CharacterClass.DRUID)
        {
            buildDivineSpellsPage(characterClass);
        }
    }

    private void buildMageSpellsPage()
    {
        CharacterSpells spells = pendingCharacter.getSpells();
        MageSpell[] mageSpells = MageSpell.values();
        clampSpellScroll();

        int centerX = this.width / 2;
        int panelWidth = Math.min(440, this.width - 24);
        int rightX = centerX + panelWidth / 2;
        int controlsX = rightX - 146;
        int buttonHeight = Math.min(getButtonHeight(), getRowHeight() - 2);
        int firstRow = spellScrollOffset;
        int lastRow = firstRow + getSpellVisibleRows() - 1;

        for (int i = 0; i < mageSpells.length; i++)
        {
            if (i < firstRow || i > lastRow) continue;
            MageSpell spell = mageSpells[i];
            int y = getSpellRowY(i);

            this.addRenderableWidget(Button.builder(Component.literal("Info"), button -> focusedMageSpell = spell)
                    .bounds(controlsX, y, 34, buttonHeight).build());

            boolean known = spells.knowsMageSpell(spell);
            Button learnButton = Button.builder(Component.literal(known ? "Forget" : "Learn"), button ->
            {
                focusedMageSpell = spell;
                spells.toggleKnownMageSpell(spell);
                buildCurrentPage();
            }).bounds(controlsX + 38, y, 50, buttonHeight).build();
            learnButton.active = known || spells.canLearnAnotherMageSpell();
            this.addRenderableWidget(learnButton);

            boolean memorized = spells.getMemorizedMageSpell() == spell;
            Button memorizeButton = Button.builder(Component.literal(memorized ? "Mem" : "Memorize"), button ->
            {
                focusedMageSpell = spell;
                spells.setMemorizedMageSpell(spell);
                buildCurrentPage();
            }).bounds(controlsX + 92, y, 54, buttonHeight).build();
            memorizeButton.active = known && !memorized;
            this.addRenderableWidget(memorizeButton);
        }
    }

    private void buildDivineSpellsPage(CharacterClass characterClass)
    {
        CharacterRace race = pendingCharacter.getRace();
        if (race == null) return;

        CharacterSpells spells = pendingCharacter.getSpells();
        int finalWisdom = pendingCharacter.getAbilityScores().getFinalWisdom(race);
        List<DivineSpell> divineSpells = getAvailableDivineSpells(characterClass);
        clampSpellScroll();

        int centerX = this.width / 2;
        int panelWidth = Math.min(440, this.width - 24);
        int rightX = centerX + panelWidth / 2;
        int controlsX = rightX - 90;
        int buttonHeight = Math.min(getButtonHeight(), getRowHeight() - 2);
        int firstRow = spellScrollOffset;
        int lastRow = firstRow + getSpellVisibleRows() - 1;

        for (int i = 0; i < divineSpells.size(); i++)
        {
            if (i < firstRow || i > lastRow) continue;
            DivineSpell spell = divineSpells.get(i);
            int y = getSpellRowY(i);

            this.addRenderableWidget(Button.builder(Component.literal("Info"), button -> focusedDivineSpell = spell)
                    .bounds(controlsX, y, 34, buttonHeight).build());

            int count = spells.getMemorizedDivineSpellCount(spell);
            Button minusButton = Button.builder(Component.literal("-"), button ->
            {
                focusedDivineSpell = spell;
                spells.decreaseDivineSpell(spell);
                buildCurrentPage();
            }).bounds(controlsX + 38, y, 22, buttonHeight).build();
            minusButton.active = count > 0;
            this.addRenderableWidget(minusButton);

            Button plusButton = Button.builder(Component.literal("+"), button ->
            {
                focusedDivineSpell = spell;
                spells.increaseDivineSpell(characterClass, finalWisdom, spell);
                buildCurrentPage();
            }).bounds(controlsX + 64, y, 22, buttonHeight).build());
            plusButton.active = spells.canMemorizeAnotherDivineSpell(characterClass, finalWisdom);
            this.addRenderableWidget(plusButton);
        }
    }

    private boolean useStackedAppearanceLayout()
    {
        return this.width < 360;
    }

    private int getAppearanceRowHeight()
    {
        if (!isCompactLayout()) return 28;
        int availableHeight = getNavigationY() - getContentTop() - 18;
        return Math.max(19, Math.min(23, availableHeight / 6));
    }

    private AppearanceColumns getAppearanceColumns()
    {
        boolean stacked = useStackedAppearanceLayout();
        int regionWidth = stacked
                ? Math.max(250, this.width - 20)
                : Math.min(390, Math.max(280, this.width / 2 - 24));
        regionWidth = Math.min(regionWidth, this.width - 16);

        int regionLeft = stacked
                ? (this.width - regionWidth) / 2
                : Math.max(8, (this.width / 2 - regionWidth) / 2);

        int buttonWidth = isCompactLayout() ? 22 : 28;
        int gap = isCompactLayout() ? 5 : 7;
        int labelWidth = Math.max(74, Math.min(96, regionWidth / 4));
        int swatchWidth = 22;
        int remaining = regionWidth - labelWidth - buttonWidth * 2 - swatchWidth - gap * 4;
        int valueWidth = Math.max(82, remaining);

        int labelX = regionLeft;
        int previousX = labelX + labelWidth + gap;
        int valueLeft = previousX + buttonWidth + gap;
        int valueCenterX = valueLeft + valueWidth / 2;
        int nextX = valueLeft + valueWidth + gap;
        int swatchX = nextX + buttonWidth + gap;

        return new AppearanceColumns(labelX, previousX, valueCenterX, nextX, swatchX, buttonWidth);
    }

    private void buildAppearancePage()
    {
        CharacterAppearance appearance = pendingCharacter.getAppearance();
        boolean stacked = useStackedAppearanceLayout();
        AppearanceColumns columns = getAppearanceColumns();
        int rowHeight = getAppearanceRowHeight();
        int contentTop = getContentTop();
        int availableHeight = Math.max(80, getNavigationY() - contentTop);

        int previewWidth;
        int previewHeight;
        int previewX;
        int previewY = contentTop;
        int startY;

        if (stacked)
        {
            previewWidth = Math.max(90, Math.min(140, this.width - 40));
            previewHeight = Math.max(70, Math.min(105, availableHeight / 2 - 8));
            previewX = this.width / 2 - previewWidth / 2;
            startY = previewY + previewHeight + 15;
        }
        else
        {
            previewWidth = Math.max(105, Math.min(isCompactLayout() ? 145 : 210, this.width / 3));
            previewHeight = Math.max(85, Math.min(isCompactLayout() ? 150 : 235, availableHeight - 28));
            int previewCenterX = Math.min(this.width - previewWidth / 2 - 10, this.width * 3 / 4);
            previewX = previewCenterX - previewWidth / 2;
            startY = contentTop + (isCompactLayout() ? 7 : 22);
        }

        addAppearanceButtons(columns, startY, appearance::previousSkinTone, appearance::nextSkinTone);
        addAppearanceButtons(columns, startY + rowHeight, appearance::previousHairStyle, appearance::nextHairStyle);
        addAppearanceButtons(columns, startY + rowHeight * 2, appearance::previousHairColor, appearance::nextHairColor);
        addAppearanceButtons(columns, startY + rowHeight * 3, appearance::previousEyeColor, appearance::nextEyeColor);
        addAppearanceButtons(columns, startY + rowHeight * 4, appearance::previousFacialHair, appearance::nextFacialHair);

        appearanceSkinWidget = new PlayerSkinWidget(
                previewWidth,
                previewHeight,
                Minecraft.getInstance().getEntityModels(),
                () -> DefaultPlayerSkin.get(APPEARANCE_PREVIEW_UUID));
        appearanceSkinWidget.setPosition(previewX, previewY);
        this.addRenderableWidget(appearanceSkinWidget);
    }

    private void addAppearanceButtons(AppearanceColumns columns, int y, Runnable previous, Runnable next)
    {
        int buttonHeight = getButtonHeight();

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> previous.run())
                .bounds(columns.previousX(), y, columns.buttonWidth(), buttonHeight).build());

        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> next.run())
                .bounds(columns.nextX(), y, columns.buttonWidth(), buttonHeight).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (scrollY == 0.0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);

        if (currentPage == CharacterCreationPage.PROFICIENCIES)
        {
            int oldOffset = proficiencyScrollOffset;
            proficiencyScrollOffset += scrollY > 0.0 ? -1 : 1;
            clampProficiencyScroll();
            if (proficiencyScrollOffset != oldOffset) buildCurrentPage();
            return true;
        }

        if (currentPage == CharacterCreationPage.SPELLS)
        {
            int oldOffset = spellScrollOffset;
            spellScrollOffset += scrollY > 0.0 ? -1 : 1;
            clampSpellScroll();
            if (spellScrollOffset != oldOffset) buildCurrentPage();
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void buildPlaceholderPage() { }

    private void buildNavigationButtons()
    {
        int centerX = this.width / 2;
        int bottomY = getNavigationY();
        int gap = 10;
        int buttonWidth = Math.min(100, Math.max(70, (this.width - 30 - gap) / 2));
        int buttonHeight = getButtonHeight();

        this.addRenderableWidget(Button.builder(
                Component.literal(currentPage == CharacterCreationPage.GENDER ? "Back to Title" : "Back"),
                button -> previousPage())
                .bounds(centerX - gap / 2 - buttonWidth, bottomY, buttonWidth, buttonHeight).build());

        nextButton = Button.builder(
                Component.literal(currentPage == CharacterCreationPage.REVIEW ? "Finish" : "Next"),
                button -> nextPage())
                .bounds(centerX + gap / 2, bottomY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(nextButton);
        updateNextButton();
    }

    private void updateNextButton()
    {
        if (nextButton == null) return;

        switch (currentPage)
        {
            case GENDER -> nextButton.active = pendingCharacter.getGender() != null;
            case RACE -> nextButton.active = pendingCharacter.getRace() != null;
            case CLASS -> nextButton.active = pendingCharacter.getCharacterClass() != null;
            case ALIGNMENT -> nextButton.active = pendingCharacter.getAlignment() != null;
            case ABILITIES -> nextButton.active = pendingCharacter.getAbilityScores().isRolled()
                    && pendingCharacter.getAbilityScores().getAvailablePoints() == 0;
            case SKILLS ->
            {
                CharacterSkills skills = pendingCharacter.getSkills();
                nextButton.active = skills.isInitialized() && skills.getAvailablePoints() == 0;
            }
            case PROFICIENCIES ->
            {
                CharacterProficiencies proficiencies = pendingCharacter.getProficiencies();
                nextButton.active = proficiencies.isInitialized() && proficiencies.getAvailablePoints() == 0;
            }
            case SPELLS -> nextButton.active = isSpellSelectionComplete();
            default -> nextButton.active = true;
        }
    }

    private boolean isSpellSelectionComplete()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (characterClass == CharacterClass.MAGE)
        {
            return pendingCharacter.getSpells().isMageSelectionComplete();
        }
        if (characterClass == CharacterClass.CLERIC || characterClass == CharacterClass.DRUID)
        {
            CharacterRace race = pendingCharacter.getRace();
            if (race == null) return false;
            int finalWisdom = pendingCharacter.getAbilityScores().getFinalWisdom(race);
            return pendingCharacter.getSpells().isDivineSelectionComplete(characterClass, finalWisdom);
        }
        return true;
    }

    private void nextPage()
    {
        if (currentPage == CharacterCreationPage.REVIEW)
        {
            continueToWorldCreation();
            return;
        }

        CharacterCreationPage[] pages = CharacterCreationPage.values();
        int nextIndex = currentPage.ordinal() + 1;
        while (nextIndex < pages.length)
        {
            CharacterCreationPage candidate = pages[nextIndex];
            if (candidate == CharacterCreationPage.SKILLS && !usesSkillsPage()) { nextIndex++; continue; }
            if (candidate == CharacterCreationPage.SPELLS && !usesSpellsPage()) { nextIndex++; continue; }
            if (candidate == CharacterCreationPage.PROFICIENCIES) proficiencyScrollOffset = 0;
            if (candidate == CharacterCreationPage.SPELLS)
            {
                spellScrollOffset = 0;
                focusedMageSpell = MageSpell.ARMOR;
                focusedDivineSpell = DivineSpell.ARMOR_OF_FAITH;
            }
            currentPage = candidate;
            buildCurrentPage();
            return;
        }
    }

    private void previousPage()
    {
        if (currentPage == CharacterCreationPage.GENDER)
        {
            returnToTitle();
            return;
        }

        CharacterCreationPage[] pages = CharacterCreationPage.values();
        int previousIndex = currentPage.ordinal() - 1;
        while (previousIndex >= 0)
        {
            CharacterCreationPage candidate = pages[previousIndex];
            if (candidate == CharacterCreationPage.SKILLS && !usesSkillsPage()) { previousIndex--; continue; }
            if (candidate == CharacterCreationPage.SPELLS && !usesSpellsPage()) { previousIndex--; continue; }
            if (candidate == CharacterCreationPage.PROFICIENCIES) proficiencyScrollOffset = 0;
            if (candidate == CharacterCreationPage.SPELLS) spellScrollOffset = 0;
            currentPage = candidate;
            buildCurrentPage();
            return;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(this.font, "CHARACTER GENERATION", this.width / 2, getHeaderY(), 0xFFFFFF);
        graphics.drawCenteredString(this.font, getPageTitle(), this.width / 2, getPageTitleY(), 0xFFFFAA);

        switch (currentPage)
        {
            case GENDER -> renderGenderSelection(graphics);
            case RACE -> renderRaceSelection(graphics);
            case CLASS -> renderClassSelection(graphics);
            case ALIGNMENT -> renderAlignmentSelection(graphics);
            case ABILITIES -> renderAbilities(graphics);
            case SKILLS -> renderSkills(graphics);
            case PROFICIENCIES -> renderProficiencies(graphics);
            case SPELLS -> renderSpells(graphics);
            case APPEARANCE -> renderAppearance(graphics);
            default -> { }
        }
    }

    private void renderGenderSelection(GuiGraphics graphics)
    {
        PendingCharacter.Gender gender = pendingCharacter.getGender();
        if (gender == null)
        {
            graphics.drawCenteredString(this.font, "Choose your character's gender", this.width / 2, getContentTop(), 0xAAAAAA);
            return;
        }
        graphics.drawCenteredString(this.font, "Selected: " + formatGender(gender), this.width / 2,
                Math.min(getNavigationY() - 28, this.height / 2 + 25), 0xAAFFAA);
    }

    private void renderRaceSelection(GuiGraphics graphics)
    {
        CharacterRace race = pendingCharacter.getRace();
        if (race == null) return;
        renderSelectionDescription(graphics, race.getDisplayName(), race.getDescription(),
                isCompactLayout() ? getNavigationY() - 62 : this.height / 2 + 55);
    }

    private void renderClassSelection(GuiGraphics graphics)
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (characterClass == null) return;
        renderSelectionDescription(graphics, characterClass.getDisplayName(), characterClass.getDescription(),
                isCompactLayout() ? getNavigationY() - 62 : this.height / 2 + 65);
    }

    private void renderAlignmentSelection(GuiGraphics graphics)
    {
        CharacterAlignment alignment = pendingCharacter.getAlignment();
        if (alignment == null)
        {
            graphics.drawCenteredString(this.font, "Choose your character's alignment", this.width / 2, getContentTop() - 13, 0xAAAAAA);
            return;
        }
        int gridBottom = getContentTop() + 3 * (getButtonHeight() + (isCompactLayout() ? 4 : 10));
        renderSelectionDescription(graphics, alignment.getDisplayName(), alignment.getDescription(),
                Math.min(getNavigationY() - 58, gridBottom + 5));
    }

    private void renderSelectionDescription(GuiGraphics graphics, String name, String description, int requestedY)
    {
        int width = getDescriptionWidth();
        int centerX = this.width / 2;
        int y = Math.max(getContentTop(), Math.min(requestedY, getNavigationY() - 55));
        graphics.drawCenteredString(this.font, name, centerX, y, 0xAAFFAA);
        graphics.drawWordWrap(this.font, Component.literal(description), centerX - width / 2, y + 14, width, 0xCCCCCC);
    }

    private void renderAbilities(GuiGraphics graphics)
    {
        AbilityScores scores = pendingCharacter.getAbilityScores();
        CharacterRace race = pendingCharacter.getRace();
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        int centerX = this.width / 2;
        int startY = getContentTop() + (isCompactLayout() ? 4 : 17);
        int rowHeight = getAbilityRowHeight();

        if (!scores.isRolled())
        {
            graphics.drawCenteredString(this.font, "Roll your ability scores", centerX, getContentTop(), 0xAAAAAA);
            return;
        }
        if (race == null || characterClass == null) return;

        int labelX = Math.max(12, centerX - 120);
        int finalX = Math.min(this.width - 22, centerX + 168);
        int raceX = Math.min(this.width - 62, centerX + 118);
        int baseX = Math.min(this.width - 102, centerX + 68);

        if (!isCompactLayout())
        {
            graphics.drawString(this.font, "Ability", labelX, startY - 18, 0xAAAAAA);
            graphics.drawString(this.font, "Base", baseX - 12, startY - 18, 0xAAAAAA);
            graphics.drawString(this.font, "Race", raceX - 12, startY - 18, 0xAAAAAA);
            graphics.drawString(this.font, "Final", finalX - 12, startY - 18, 0xAAAAAA);
        }

        drawAbility(graphics, "STR", scores.getStrength(), CharacterAbilityRules.getStrengthModifier(race),
                formatFinalStrength(scores, race, characterClass), CharacterAbilityRules.getMinimumStrength(race, characterClass), startY);
        drawAbility(graphics, "DEX", scores.getDexterity(), CharacterAbilityRules.getDexterityModifier(race),
                Integer.toString(scores.getFinalDexterity(race)), CharacterAbilityRules.getMinimumDexterity(race, characterClass), startY + rowHeight);
        drawAbility(graphics, "CON", scores.getConstitution(), CharacterAbilityRules.getConstitutionModifier(race),
                Integer.toString(scores.getFinalConstitution(race)), CharacterAbilityRules.getMinimumConstitution(race, characterClass), startY + rowHeight * 2);
        drawAbility(graphics, "INT", scores.getIntelligence(), CharacterAbilityRules.getIntelligenceModifier(race),
                Integer.toString(scores.getFinalIntelligence(race)), CharacterAbilityRules.getMinimumIntelligence(race, characterClass), startY + rowHeight * 3);
        drawAbility(graphics, "WIS", scores.getWisdom(), CharacterAbilityRules.getWisdomModifier(race),
                Integer.toString(scores.getFinalWisdom(race)), CharacterAbilityRules.getMinimumWisdom(race, characterClass), startY + rowHeight * 4);
        drawAbility(graphics, "CHA", scores.getCharisma(), CharacterAbilityRules.getCharismaModifier(race),
                Integer.toString(scores.getFinalCharisma(race)), CharacterAbilityRules.getMinimumCharisma(race, characterClass), startY + rowHeight * 5);

        int infoY = getAbilityInfoY(startY);
        graphics.drawCenteredString(this.font, "Available Points: " + scores.getAvailablePoints(), centerX, infoY, 0xFFFFAA);
        if (isCompactLayout())
        {
            String totals = "Roll Total: " + scores.getTotal();
            if (scores.hasStoredRoll()) totals += "   Stored Total: " + scores.getStoredTotal();
            graphics.drawCenteredString(this.font, totals, centerX, infoY + 13, 0xAAAAAA);
        }
        else
        {
            graphics.drawCenteredString(this.font, "Roll Total: " + scores.getTotal(), centerX, infoY + 15, 0xAAAAAA);
            if (scores.hasStoredRoll())
            {
                graphics.drawCenteredString(this.font, "Stored Total: " + scores.getStoredTotal(), centerX, infoY + 30, 0xAAFFAA);
            }
        }
    }

    private void drawAbility(GuiGraphics graphics, String name, int baseValue, int racialModifier, String finalValue, int minimum, int y)
    {
        int centerX = this.width / 2;
        int labelX = Math.max(12, centerX - 120);
        graphics.drawString(this.font, name, labelX, y + 4, 0xFFFFFF);
        graphics.drawString(this.font, "Min " + minimum, labelX + 35, y + 4, 0x888888);
        graphics.drawCenteredString(this.font, Integer.toString(baseValue), Math.min(this.width - 102, centerX + 68), y + 4, 0xFFFFFF);
        graphics.drawCenteredString(this.font, formatModifier(racialModifier), Math.min(this.width - 62, centerX + 118), y + 4,
                racialModifier == 0 ? 0x888888 : 0xFFFFAA);
        graphics.drawCenteredString(this.font, finalValue, Math.min(this.width - 22, centerX + 168), y + 4, 0xAAFFAA);
    }

    private void renderSkills(GuiGraphics graphics)
    {
        CharacterSkills skills = pendingCharacter.getSkills();
        if (!skills.isInitialized()) return;
        int centerX = this.width / 2;
        int startY = getContentTop() + 5;
        int rowHeight = getRowHeight();

        drawSkill(graphics, "Open Locks", skills.getOpenLocks(), startY);
        drawSkill(graphics, "Find Traps", skills.getFindTraps(), startY + rowHeight);
        drawSkill(graphics, "Pick Pockets", skills.getPickPockets(), startY + rowHeight * 2);
        drawSkill(graphics, "Move Silently", skills.getMoveSilently(), startY + rowHeight * 3);
        drawSkill(graphics, "Hide in Shadows", skills.getHideInShadows(), startY + rowHeight * 4);
        drawSkill(graphics, "Detect Illusion", skills.getDetectIllusion(), startY + rowHeight * 5);
        drawSkill(graphics, "Set Traps", skills.getSetTraps(), startY + rowHeight * 6);
        graphics.drawCenteredString(this.font, "Points Remaining: " + skills.getAvailablePoints(), centerX,
                Math.min(getNavigationY() - 18, startY + rowHeight * 7 + 5), 0xFFFFAA);
    }

    private void drawSkill(GuiGraphics graphics, String name, int value, int y)
    {
        int centerX = this.width / 2;
        graphics.drawString(this.font, name, Math.max(12, centerX - 120), y + 5, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Integer.toString(value), centerX + 5, y + 5, 0xAAFFAA);
    }

    private void renderProficiencies(GuiGraphics graphics)
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        CharacterProficiencies proficiencies = pendingCharacter.getProficiencies();
        if (characterClass == null || !proficiencies.isInitialized()) return;
        clampProficiencyScroll();

        int centerX = this.width / 2;
        int panelWidth = Math.min(360, this.width - 24);
        int labelX = centerX - panelWidth / 2;
        int rankX = centerX + panelWidth / 2 - 78;
        int firstRow = proficiencyScrollOffset;
        int lastRow = firstRow + getProficiencyVisibleRows() - 1;
        int weaponCount = WeaponProficiency.values().length;
        int startY = getProficiencyStartY();

        graphics.drawString(this.font, "Weapon / Style", labelX, startY - 14, 0xAAAAAA);
        graphics.drawCenteredString(this.font, "Rank", rankX, startY - 14, 0xAAAAAA);

        for (int i = 0; i < weaponCount; i++)
        {
            if (i < firstRow || i > lastRow) continue;
            WeaponProficiency proficiency = WeaponProficiency.values()[i];
            boolean allowed = proficiencies.canUseWeapon(characterClass, proficiency);
            int y = getProficiencyRowY(i);
            graphics.drawString(this.font, proficiency.getDisplayName(), labelX, y + 5, allowed ? 0xFFFFFF : 0x777777);
            graphics.drawCenteredString(this.font, formatProficiencyRank(proficiencies.getWeaponRank(proficiency)),
                    rankX, y + 5, allowed ? 0xAAFFAA : 0x666666);
        }

        int styleHeaderRow = weaponCount;
        if (styleHeaderRow >= firstRow && styleHeaderRow <= lastRow)
        {
            graphics.drawString(this.font, "Weapon Styles", labelX, getProficiencyRowY(styleHeaderRow) + 5, 0xFFFFAA);
        }

        FightingStyle[] styles = FightingStyle.values();
        for (int i = 0; i < styles.length; i++)
        {
            int logicalRow = styleHeaderRow + 1 + i;
            if (logicalRow < firstRow || logicalRow > lastRow) continue;
            FightingStyle style = styles[i];
            boolean allowed = proficiencies.canUseStyle(characterClass, style);
            int y = getProficiencyRowY(logicalRow);
            graphics.drawString(this.font, style.getDisplayName(), labelX, y + 5, allowed ? 0xFFFFFF : 0x777777);
            graphics.drawCenteredString(this.font, formatProficiencyRank(proficiencies.getStyleRank(style)),
                    rankX, y + 5, allowed ? 0xAAFFAA : 0x666666);
        }

        renderProficiencyScrollbar(graphics);
        int footerY = getNavigationY() - (isCompactLayout() ? 30 : 36);
        graphics.drawCenteredString(this.font, "Proficiency Points Remaining: " + proficiencies.getAvailablePoints(), centerX, footerY, 0xFFFFAA);
        if (!isCompactLayout())
        {
            graphics.drawCenteredString(this.font, "Mouse wheel to scroll", centerX, footerY + 13, 0x888888);
        }
    }

    private void renderProficiencyScrollbar(GuiGraphics graphics)
    {
        int totalRows = getProficiencyContentRows();
        int visibleRows = getProficiencyVisibleRows();
        if (totalRows <= visibleRows) return;

        int centerX = this.width / 2;
        int panelWidth = Math.min(360, this.width - 24);
        int trackX = centerX + panelWidth / 2 - 4;
        int trackTop = getProficiencyStartY();
        int trackHeight = visibleRows * getRowHeight() - 2;
        graphics.fill(trackX, trackTop, trackX + 4, trackTop + trackHeight, 0xFF333333);

        int thumbHeight = Math.max(18, trackHeight * visibleRows / totalRows);
        int movable = trackHeight - thumbHeight;
        int thumbTop = trackTop;
        int maxScroll = getMaxProficiencyScroll();
        if (maxScroll > 0) thumbTop += movable * proficiencyScrollOffset / maxScroll;
        graphics.fill(trackX, thumbTop, trackX + 4, thumbTop + thumbHeight, 0xFFAAAAAA);
    }

    private void renderSpells(GuiGraphics graphics)
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        if (characterClass == CharacterClass.MAGE)
        {
            renderMageSpells(graphics);
        }
        else if (characterClass == CharacterClass.CLERIC || characterClass == CharacterClass.DRUID)
        {
            renderDivineSpells(graphics, characterClass);
        }
    }

    private void renderMageSpells(GuiGraphics graphics)
    {
        CharacterSpells spells = pendingCharacter.getSpells();
        MageSpell[] mageSpells = MageSpell.values();
        clampSpellScroll();

        int centerX = this.width / 2;
        int panelWidth = Math.min(440, this.width - 24);
        int labelX = centerX - panelWidth / 2;
        int firstRow = spellScrollOffset;
        int lastRow = firstRow + getSpellVisibleRows() - 1;

        graphics.drawString(this.font, "Level 1 Mage Spells", labelX, getSpellStartY() - 13, 0xAAAAAA);
        for (int i = 0; i < mageSpells.length; i++)
        {
            if (i < firstRow || i > lastRow) continue;
            MageSpell spell = mageSpells[i];
            int y = getSpellRowY(i);
            boolean known = spells.knowsMageSpell(spell);
            boolean memorized = spells.getMemorizedMageSpell() == spell;
            int color = memorized ? 0xFFFFAA : known ? 0xAAFFAA : 0xFFFFFF;
            graphics.drawString(this.font, spell.getDisplayName(), labelX, y + 5, color);
        }

        renderSpellScrollbar(graphics);
        MageSpell focused = focusedMageSpell == null ? MageSpell.ARMOR : focusedMageSpell;
        int descriptionY = getSpellStartY() + getSpellVisibleRows() * getRowHeight() + 4;
        renderSpellDescription(graphics, focused.getDisplayName(), focused.getSchool(), focused.getStatistics(), focused.getDescription(), descriptionY, panelWidth);

        int statusY = getNavigationY() - (isCompactLayout() ? 25 : 36);
        String memorizedName = spells.getMemorizedMageSpell() == null ? "None" : spells.getMemorizedMageSpell().getDisplayName();
        graphics.drawCenteredString(this.font,
                "Spellbook: " + spells.getKnownMageSpellCount() + "/" + CharacterSpells.STARTING_MAGE_KNOWN_SPELLS
                        + "   Memorized: " + memorizedName,
                centerX, statusY, 0xAAFFAA);
        if (!isCompactLayout())
        {
            graphics.drawCenteredString(this.font, "Learn two spells and memorize one. Mouse wheel scrolls the list.", centerX, statusY + 13, 0x888888);
        }
    }

    private void renderDivineSpells(GuiGraphics graphics, CharacterClass characterClass)
    {
        CharacterRace race = pendingCharacter.getRace();
        if (race == null) return;

        CharacterSpells spells = pendingCharacter.getSpells();
        List<DivineSpell> divineSpells = getAvailableDivineSpells(characterClass);
        int finalWisdom = pendingCharacter.getAbilityScores().getFinalWisdom(race);
        int slots = spells.getStartingDivineSpellSlots(characterClass, finalWisdom);
        clampSpellScroll();

        int centerX = this.width / 2;
        int panelWidth = Math.min(440, this.width - 24);
        int labelX = centerX - panelWidth / 2;
        int countX = centerX + panelWidth / 2 - 108;
        int firstRow = spellScrollOffset;
        int lastRow = firstRow + getSpellVisibleRows() - 1;

        graphics.drawString(this.font, "Level 1 " + characterClass.getDisplayName() + " Spells", labelX, getSpellStartY() - 13, 0xAAAAAA);
        graphics.drawString(this.font, "Mem", countX, getSpellStartY() - 13, 0xAAAAAA);

        for (int i = 0; i < divineSpells.size(); i++)
        {
            if (i < firstRow || i > lastRow) continue;
            DivineSpell spell = divineSpells.get(i);
            int y = getSpellRowY(i);
            int count = spells.getMemorizedDivineSpellCount(spell);
            graphics.drawString(this.font, spell.getDisplayName(), labelX, y + 5, count > 0 ? 0xFFFFAA : 0xFFFFFF);
            graphics.drawCenteredString(this.font, Integer.toString(count), countX + 12, y + 5, count > 0 ? 0xAAFFAA : 0x888888);
        }

        renderSpellScrollbar(graphics);

        DivineSpell focused = focusedDivineSpell;
        if (focused == null || !focused.isAvailableTo(characterClass))
        {
            focused = divineSpells.isEmpty() ? DivineSpell.ARMOR_OF_FAITH : divineSpells.get(0);
        }
        int descriptionY = getSpellStartY() + getSpellVisibleRows() * getRowHeight() + 4;
        renderSpellDescription(graphics, focused.getDisplayName(), focused.getSchool(), focused.getStatistics(), focused.getDescription(), descriptionY, panelWidth);

        int statusY = getNavigationY() - (isCompactLayout() ? 25 : 36);
        graphics.drawCenteredString(this.font,
                "Memorized: " + spells.getTotalMemorizedDivineSpells() + "/" + slots + "   Wisdom: " + finalWisdom,
                centerX, statusY, 0xAAFFAA);
        if (!isCompactLayout())
        {
            graphics.drawCenteredString(this.font,
                    "Divine casters know their full spell list; choose what to memorize. Mouse wheel scrolls the list.",
                    centerX, statusY + 13, 0x888888);
        }
    }

    private void renderSpellDescription(
            GuiGraphics graphics,
            String name,
            String school,
            String statistics,
            String description,
            int descriptionY,
            int panelWidth)
    {
        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, name + " - " + school, centerX, descriptionY, 0xFFFFAA);
        graphics.drawWordWrap(this.font, Component.literal(statistics), centerX - panelWidth / 2, descriptionY + 13, panelWidth, 0xAAFFAA);
        int statsLines = Math.max(1, (this.font.width(statistics) + panelWidth - 1) / panelWidth);
        graphics.drawWordWrap(this.font, Component.literal(description), centerX - panelWidth / 2,
                descriptionY + 13 + statsLines * 10, panelWidth, 0xCCCCCC);
    }

    private void renderSpellScrollbar(GuiGraphics graphics)
    {
        int totalRows = getSpellContentRows();
        int visibleRows = getSpellVisibleRows();
        if (totalRows <= visibleRows) return;

        int centerX = this.width / 2;
        int panelWidth = Math.min(440, this.width - 24);
        int trackX = centerX + panelWidth / 2 - 4;
        int trackTop = getSpellStartY();
        int trackHeight = visibleRows * getRowHeight() - 2;
        graphics.fill(trackX, trackTop, trackX + 4, trackTop + trackHeight, 0xFF333333);

        int thumbHeight = Math.max(18, trackHeight * visibleRows / totalRows);
        int movable = trackHeight - thumbHeight;
        int thumbTop = trackTop;
        int maxScroll = getMaxSpellScroll();
        if (maxScroll > 0) thumbTop += movable * spellScrollOffset / maxScroll;
        graphics.fill(trackX, thumbTop, trackX + 4, thumbTop + thumbHeight, 0xFFAAAAAA);
    }

    private void renderAppearance(GuiGraphics graphics)
    {
        CharacterAppearance appearance = pendingCharacter.getAppearance();
        boolean stacked = useStackedAppearanceLayout();
        AppearanceColumns columns = getAppearanceColumns();
        int rowHeight = getAppearanceRowHeight();
        int startY = stacked && appearanceSkinWidget != null
                ? appearanceSkinWidget.getBottom() + 15
                : getContentTop() + (isCompactLayout() ? 7 : 22);

        drawAppearanceRow(graphics, "Skin Tone", appearance.getSkinTone().getDisplayName(), columns, startY);
        drawAppearanceRow(graphics, "Hair Style", appearance.getHairStyle().getDisplayName(), columns, startY + rowHeight);
        drawAppearanceRow(graphics, "Hair Color", appearance.getHairColor().getDisplayName(), columns, startY + rowHeight * 2);
        drawAppearanceRow(graphics, "Eye Color", appearance.getEyeColor().getDisplayName(), columns, startY + rowHeight * 3);
        drawAppearanceRow(graphics, "Facial Hair", appearance.getFacialHair().getDisplayName(), columns, startY + rowHeight * 4);

        drawColorSwatch(graphics, columns.swatchX(), startY + 3, appearance.getSkinTone().getRgb());
        drawColorSwatch(graphics, columns.swatchX(), startY + rowHeight * 2 + 3, appearance.getHairColor().getRgb());
        drawColorSwatch(graphics, columns.swatchX(), startY + rowHeight * 3 + 3, appearance.getEyeColor().getRgb());

        if (appearanceSkinWidget != null)
        {
            int hintY = Math.min(getNavigationY() - 12, appearanceSkinWidget.getBottom() + 5);
            graphics.drawCenteredString(this.font, "Click and drag the model to rotate",
                    appearanceSkinWidget.getX() + appearanceSkinWidget.getWidth() / 2, hintY, 0x888888);
        }
    }

    private void drawAppearanceRow(GuiGraphics graphics, String label, String value, AppearanceColumns columns, int y)
    {
        graphics.drawString(this.font, label, columns.labelX(), y + 5, 0xAAAAAA);
        graphics.drawCenteredString(this.font, value, columns.valueCenterX(), y + 5, 0xFFFFFF);
    }

    private void drawColorSwatch(GuiGraphics graphics, int x, int y, int rgb)
    {
        int argb = 0xFF000000 | rgb;
        graphics.fill(x, y, x + 18, y + 12, 0xFF222222);
        graphics.fill(x + 2, y + 2, x + 16, y + 10, argb);
    }

    private String formatProficiencyRank(int rank) { return rank <= 0 ? "-" : "*".repeat(rank); }
    private String formatModifier(int modifier) { return modifier > 0 ? "+" + modifier : modifier < 0 ? Integer.toString(modifier) : "-"; }

    private String formatFinalStrength(AbilityScores scores, CharacterRace race, CharacterClass characterClass)
    {
        int finalStrength = scores.getFinalStrength(race);
        if (!CharacterAbilityRules.canHaveExceptionalStrength(race, characterClass, finalStrength)) return Integer.toString(finalStrength);
        int exceptional = scores.getExceptionalStrength();
        if (exceptional <= 0) return Integer.toString(finalStrength);
        return "18/" + (exceptional == 100 ? "00" : String.format("%02d", exceptional));
    }

    private String formatGender(PendingCharacter.Gender gender)
    {
        return gender == PendingCharacter.Gender.MALE ? "Male" : "Female";
    }

    private String getPageTitle()
    {
        return switch (currentPage)
        {
            case GENDER -> "Gender";
            case RACE -> "Race";
            case CLASS -> "Class";
            case ALIGNMENT -> "Alignment";
            case ABILITIES -> "Ability Scores";
            case SKILLS -> "Skills / Class Abilities";
            case PROFICIENCIES -> "Weapon Proficiencies";
            case SPELLS ->
            {
                CharacterClass characterClass = pendingCharacter.getCharacterClass();
                if (characterClass == CharacterClass.MAGE) yield "Mage Spells";
                if (characterClass == CharacterClass.CLERIC) yield "Cleric Spells";
                if (characterClass == CharacterClass.DRUID) yield "Druid Spells";
                yield "Spells";
            }
            case APPEARANCE -> "Appearance";
            case NAME -> "Name";
            case REVIEW -> "Review Character";
        };
    }

    private boolean usesSkillsPage()
    {
        return pendingCharacter.getCharacterClass() == CharacterClass.THIEF;
    }

    private boolean usesSpellsPage()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        return characterClass == CharacterClass.MAGE
                || characterClass == CharacterClass.CLERIC
                || characterClass == CharacterClass.DRUID;
    }

    @Override
    public void onClose()
    {
        previousPage();
    }

    private void returnToTitle()
    {
        Minecraft.getInstance().setScreen(new TitleScreen());
    }

    private void continueToWorldCreation()
    {
        CreateWorldScreen.openFresh(Minecraft.getInstance(), this);
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
