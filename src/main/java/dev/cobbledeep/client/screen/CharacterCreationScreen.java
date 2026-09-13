package dev.cobbledeep.client.screen;

import dev.cobbledeep.character.AbilityScores;
import dev.cobbledeep.character.CharacterAbilityRules;
import dev.cobbledeep.character.CharacterAlignment;
import dev.cobbledeep.character.CharacterClass;
import dev.cobbledeep.character.CharacterProficiencies;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.CharacterSkills;
import dev.cobbledeep.character.FightingStyle;
import dev.cobbledeep.character.PendingCharacter;
import dev.cobbledeep.character.WeaponProficiency;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

public class CharacterCreationScreen extends Screen
{
    private static final int PROFICIENCY_ROW_HEIGHT = 22;
    private static final int PROFICIENCY_START_Y = 112;

    private final PendingCharacter pendingCharacter;

    private CharacterCreationPage currentPage;
    private Button nextButton;
    private int proficiencyScrollOffset;

    public CharacterCreationScreen()
    {
        super(Component.literal("Character Generation"));
        this.pendingCharacter = new PendingCharacter();
        this.currentPage = CharacterCreationPage.GENDER;
        this.proficiencyScrollOffset = 0;
    }

    @Override
    protected void init()
    {
        buildCurrentPage();
    }

    private void buildCurrentPage()
    {
        this.clearWidgets();

        switch (currentPage)
        {
            case GENDER -> buildGenderPage();
            case RACE -> buildRacePage();
            case CLASS -> buildClassPage();
            case ALIGNMENT -> buildAlignmentPage();
            case ABILITIES -> buildAbilitiesPage();
            case SKILLS -> buildSkillsPage();
            case PROFICIENCIES -> buildProficienciesPage();
            case SPELLS, APPEARANCE, NAME, REVIEW -> buildPlaceholderPage();
        }

        buildNavigationButtons();
    }

    private void buildGenderPage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Male"),
                        button ->
                        {
                            pendingCharacter.setGender(PendingCharacter.Gender.MALE);
                            updateNextButton();
                        })
                .bounds(centerX - 105, centerY - 10, 100, 20)
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Female"),
                        button ->
                        {
                            pendingCharacter.setGender(PendingCharacter.Gender.FEMALE);
                            updateNextButton();
                        })
                .bounds(centerX + 5, centerY - 10, 100, 20)
                .build()
        );
    }

    private void buildRacePage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        CharacterRace[] races = CharacterRace.values();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int horizontalSpacing = 10;
        int verticalSpacing = 10;
        int leftX = centerX - buttonWidth - horizontalSpacing / 2;
        int rightX = centerX + horizontalSpacing / 2;
        int startY = centerY - 40;

        for (int i = 0; i < races.length; i++)
        {
            CharacterRace race = races[i];
            int column = i % 2;
            int row = i / 2;
            int x = column == 0 ? leftX : rightX;
            int y = startY + row * (buttonHeight + verticalSpacing);

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(race.getDisplayName()),
                            button ->
                            {
                                if (pendingCharacter.getRace() != race)
                                {
                                    pendingCharacter.setRace(race);
                                    pendingCharacter.setCharacterClass(null);
                                    pendingCharacter.setAlignment(null);
                                    pendingCharacter.getAbilityScores().reset();
                                    pendingCharacter.getSkills().reset();
                                    pendingCharacter.getProficiencies().reset();
                                    proficiencyScrollOffset = 0;
                                }
                                updateNextButton();
                            })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build()
            );
        }
    }

    private void buildClassPage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        CharacterRace race = pendingCharacter.getRace();

        if (race == null)
        {
            return;
        }

        int buttonWidth = 100;
        int buttonHeight = 20;
        int horizontalSpacing = 10;
        int verticalSpacing = 8;
        int leftX = centerX - buttonWidth - horizontalSpacing / 2;
        int rightX = centerX + horizontalSpacing / 2;
        int startY = centerY - 55;
        int visibleIndex = 0;

        for (CharacterClass characterClass : CharacterClass.values())
        {
            if (!race.canChooseClass(characterClass))
            {
                continue;
            }

            int column = visibleIndex % 2;
            int row = visibleIndex / 2;
            int x = column == 0 ? leftX : rightX;
            int y = startY + row * (buttonHeight + verticalSpacing);

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(characterClass.getDisplayName()),
                            button ->
                            {
                                if (pendingCharacter.getCharacterClass() != characterClass)
                                {
                                    pendingCharacter.setCharacterClass(characterClass);
                                    pendingCharacter.resetAfterClassChange();
                                    proficiencyScrollOffset = 0;
                                }
                                updateNextButton();
                            })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build()
            );

            visibleIndex++;
        }
    }

    private void buildAlignmentPage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        CharacterClass characterClass = pendingCharacter.getCharacterClass();

        if (characterClass == null)
        {
            return;
        }

        int buttonWidth = 120;
        int buttonHeight = 20;
        int horizontalSpacing = 10;
        int verticalSpacing = 8;
        int leftX = centerX - buttonWidth - horizontalSpacing / 2;
        int rightX = centerX + horizontalSpacing / 2;
        int startY = centerY - 70;
        int visibleIndex = 0;

        for (CharacterAlignment alignment : CharacterAlignment.values())
        {
            if (!characterClass.canChooseAlignment(alignment))
            {
                continue;
            }

            int column = visibleIndex % 2;
            int row = visibleIndex / 2;
            int x = column == 0 ? leftX : rightX;
            int y = startY + row * (buttonHeight + verticalSpacing);

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(alignment.getDisplayName()),
                            button ->
                            {
                                if (pendingCharacter.getAlignment() != alignment)
                                {
                                    pendingCharacter.setAlignment(alignment);
                                    pendingCharacter.resetAfterAlignmentChange();
                                }
                                updateNextButton();
                            })
                    .bounds(x, y, buttonWidth, buttonHeight)
                    .build()
            );

            visibleIndex++;
        }
    }

    private void buildAbilitiesPage()
    {
        int centerX = this.width / 2;
        int startY = 112;
        AbilityScores scores = pendingCharacter.getAbilityScores();
        CharacterRace race = pendingCharacter.getRace();
        CharacterClass characterClass = pendingCharacter.getCharacterClass();

        if (race == null || characterClass == null)
        {
            return;
        }

        Button storeButton = Button.builder(
                Component.literal("Store"),
                button ->
                {
                    scores.storeCurrentRoll();
                    buildCurrentPage();
                })
                .bounds(centerX - 155, startY + 170, 100, 20)
                .build();
        storeButton.active = scores.isRolled();
        this.addRenderableWidget(storeButton);

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Reroll"),
                        button ->
                        {
                            scores.roll(race, characterClass);
                            pendingCharacter.resetAfterAbilitiesChange();
                            buildCurrentPage();
                        })
                .bounds(centerX - 50, startY + 170, 100, 20)
                .build()
        );

        Button recallButton = Button.builder(
                Component.literal("Recall"),
                button ->
                {
                    scores.recallStoredRoll();
                    pendingCharacter.resetAfterAbilitiesChange();
                    buildCurrentPage();
                })
                .bounds(centerX + 55, startY + 170, 100, 20)
                .build();
        recallButton.active = scores.hasStoredRoll();
        this.addRenderableWidget(recallButton);

        if (!scores.isRolled())
        {
            return;
        }

        addAbilityButtons(centerX, startY,
                () -> scores.decreaseStrength(race, characterClass),
                () -> scores.increaseStrength(race, characterClass));
        addAbilityButtons(centerX, startY + 22,
                () -> scores.decreaseDexterity(race, characterClass),
                () -> scores.increaseDexterity(race));
        addAbilityButtons(centerX, startY + 44,
                () -> scores.decreaseConstitution(race, characterClass),
                () -> scores.increaseConstitution(race));
        addAbilityButtons(centerX, startY + 66,
                () -> scores.decreaseIntelligence(race, characterClass),
                () -> scores.increaseIntelligence(race));
        addAbilityButtons(centerX, startY + 88,
                () -> scores.decreaseWisdom(race, characterClass),
                () -> scores.increaseWisdom(race));
        addAbilityButtons(centerX, startY + 110,
                () -> scores.decreaseCharisma(race, characterClass),
                () -> scores.increaseCharisma(race));
    }

    private void addAbilityButtons(
            int centerX,
            int y,
            Runnable decrease,
            Runnable increase)
    {
        this.addRenderableWidget(
                Button.builder(Component.literal("-"), button ->
                {
                    int oldPool = pendingCharacter.getAbilityScores().getAvailablePoints();
                    decrease.run();
                    int newPool = pendingCharacter.getAbilityScores().getAvailablePoints();
                    if (newPool != oldPool)
                    {
                        pendingCharacter.resetAfterAbilitiesChange();
                    }
                    buildCurrentPage();
                })
                .bounds(centerX - 20, y, 20, 20)
                .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("+"), button ->
                {
                    int oldPool = pendingCharacter.getAbilityScores().getAvailablePoints();
                    increase.run();
                    int newPool = pendingCharacter.getAbilityScores().getAvailablePoints();
                    if (newPool != oldPool)
                    {
                        pendingCharacter.resetAfterAbilitiesChange();
                    }
                    buildCurrentPage();
                })
                .bounds(centerX + 20, y, 20, 20)
                .build()
        );
    }

    private void buildSkillsPage()
    {
        int centerX = this.width / 2;
        int startY = 105;
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        CharacterRace race = pendingCharacter.getRace();
        AbilityScores abilityScores = pendingCharacter.getAbilityScores();
        CharacterSkills skills = pendingCharacter.getSkills();

        if (characterClass == null || race == null || !abilityScores.isRolled())
        {
            return;
        }

        if (!skills.isInitialized())
        {
            skills.initialize(race, characterClass, abilityScores);
        }

        if (characterClass != CharacterClass.THIEF)
        {
            return;
        }

        addSkillButtons(centerX, startY, skills::decreaseOpenLocks, skills::increaseOpenLocks);
        addSkillButtons(centerX, startY + 22, skills::decreaseFindTraps, skills::increaseFindTraps);
        addSkillButtons(centerX, startY + 44, skills::decreasePickPockets, skills::increasePickPockets);
        addSkillButtons(centerX, startY + 66, skills::decreaseMoveSilently, skills::increaseMoveSilently);
        addSkillButtons(centerX, startY + 88, skills::decreaseHideInShadows, skills::increaseHideInShadows);
        addSkillButtons(centerX, startY + 110, skills::decreaseDetectIllusion, skills::increaseDetectIllusion);
        addSkillButtons(centerX, startY + 132, skills::decreaseSetTraps, skills::increaseSetTraps);
    }

    private void addSkillButtons(
            int centerX,
            int y,
            Runnable decrease,
            Runnable increase)
    {
        this.addRenderableWidget(
                Button.builder(Component.literal("-"), button ->
                {
                    decrease.run();
                    buildCurrentPage();
                })
                .bounds(centerX + 35, y, 20, 20)
                .build()
        );

        this.addRenderableWidget(
                Button.builder(Component.literal("+"), button ->
                {
                    increase.run();
                    buildCurrentPage();
                })
                .bounds(centerX + 60, y, 20, 20)
                .build()
        );
    }

    private void buildProficienciesPage()
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        CharacterProficiencies proficiencies = pendingCharacter.getProficiencies();

        if (characterClass == null)
        {
            return;
        }

        if (!proficiencies.isInitialized())
        {
            proficiencies.initialize(characterClass);
        }

        clampProficiencyScroll();

        int centerX = this.width / 2;
        int controlsX = centerX + 80;
        int visibleRows = getProficiencyVisibleRows();
        int firstRow = proficiencyScrollOffset;
        int lastRow = firstRow + visibleRows - 1;
        int weaponCount = WeaponProficiency.values().length;

        for (int i = 0; i < weaponCount; i++)
        {
            if (i < firstRow || i > lastRow)
            {
                continue;
            }

            WeaponProficiency proficiency = WeaponProficiency.values()[i];
            if (!proficiencies.canUseWeapon(characterClass, proficiency))
            {
                continue;
            }

            int y = getProficiencyRowY(i);

            Button minusButton = Button.builder(
                    Component.literal("-"),
                    button ->
                    {
                        proficiencies.decreaseWeapon(proficiency);
                        buildCurrentPage();
                    })
                    .bounds(controlsX, y, 20, 20)
                    .build();
            minusButton.active = proficiencies.getWeaponRank(proficiency) > 0;
            this.addRenderableWidget(minusButton);

            Button plusButton = Button.builder(
                    Component.literal("+"),
                    button ->
                    {
                        proficiencies.increaseWeapon(characterClass, proficiency);
                        buildCurrentPage();
                    })
                    .bounds(controlsX + 25, y, 20, 20)
                    .build();
            plusButton.active = proficiencies.getAvailablePoints() > 0
                    && proficiencies.getWeaponRank(proficiency)
                    < proficiencies.getMaximumWeaponRank(characterClass);
            this.addRenderableWidget(plusButton);
        }

        int styleHeaderRow = weaponCount;
        FightingStyle[] styles = FightingStyle.values();

        for (int i = 0; i < styles.length; i++)
        {
            int logicalRow = styleHeaderRow + 1 + i;
            if (logicalRow < firstRow || logicalRow > lastRow)
            {
                continue;
            }

            FightingStyle style = styles[i];
            if (!proficiencies.canUseStyle(characterClass, style))
            {
                continue;
            }

            int y = getProficiencyRowY(logicalRow);
            int minimumRank = characterClass == CharacterClass.RANGER
                    && style == FightingStyle.TWO_WEAPON ? 2 : 0;

            Button minusButton = Button.builder(
                    Component.literal("-"),
                    button ->
                    {
                        proficiencies.decreaseStyle(characterClass, style);
                        buildCurrentPage();
                    })
                    .bounds(controlsX, y, 20, 20)
                    .build();
            minusButton.active = proficiencies.getStyleRank(style) > minimumRank;
            this.addRenderableWidget(minusButton);

            Button plusButton = Button.builder(
                    Component.literal("+"),
                    button ->
                    {
                        proficiencies.increaseStyle(characterClass, style);
                        buildCurrentPage();
                    })
                    .bounds(controlsX + 25, y, 20, 20)
                    .build();
            plusButton.active = proficiencies.getAvailablePoints() > 0
                    && proficiencies.getStyleRank(style)
                    < proficiencies.getMaximumStyleRank(characterClass, style);
            this.addRenderableWidget(plusButton);
        }
    }

    private int getProficiencyVisibleRows()
    {
        int availableHeight = this.height - PROFICIENCY_START_Y - 105;
        return Math.max(4, Math.min(12, availableHeight / PROFICIENCY_ROW_HEIGHT));
    }

    private int getProficiencyContentRows()
    {
        return WeaponProficiency.values().length
                + 1
                + FightingStyle.values().length;
    }

    private int getMaxProficiencyScroll()
    {
        return Math.max(0, getProficiencyContentRows() - getProficiencyVisibleRows());
    }

    private void clampProficiencyScroll()
    {
        proficiencyScrollOffset = Math.max(
                0,
                Math.min(proficiencyScrollOffset, getMaxProficiencyScroll())
        );
    }

    private int getProficiencyRowY(int logicalRow)
    {
        return PROFICIENCY_START_Y
                + (logicalRow - proficiencyScrollOffset) * PROFICIENCY_ROW_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY)
    {
        if (currentPage == CharacterCreationPage.PROFICIENCIES
                && scrollY != 0.0)
        {
            int oldOffset = proficiencyScrollOffset;
            proficiencyScrollOffset += scrollY > 0.0 ? -1 : 1;
            clampProficiencyScroll();

            if (proficiencyScrollOffset != oldOffset)
            {
                buildCurrentPage();
            }

            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void buildPlaceholderPage()
    {
        // Placeholder for pages not implemented yet.
    }

    private void buildNavigationButtons()
    {
        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        this.addRenderableWidget(
                Button.builder(
                        Component.literal(currentPage == CharacterCreationPage.GENDER
                                ? "Back to Title"
                                : "Back"),
                        button -> previousPage())
                .bounds(centerX - 105, bottomY, 100, 20)
                .build()
        );

        nextButton = Button.builder(
                Component.literal(currentPage == CharacterCreationPage.REVIEW
                        ? "Finish"
                        : "Next"),
                button -> nextPage())
                .bounds(centerX + 5, bottomY, 100, 20)
                .build();

        this.addRenderableWidget(nextButton);
        updateNextButton();
    }

    private void updateNextButton()
    {
        if (nextButton == null)
        {
            return;
        }

        switch (currentPage)
        {
            case GENDER -> nextButton.active = pendingCharacter.getGender() != null;
            case RACE -> nextButton.active = pendingCharacter.getRace() != null;
            case CLASS -> nextButton.active = pendingCharacter.getCharacterClass() != null;
            case ALIGNMENT -> nextButton.active = pendingCharacter.getAlignment() != null;
            case ABILITIES -> nextButton.active =
                    pendingCharacter.getAbilityScores().isRolled()
                    && pendingCharacter.getAbilityScores().getAvailablePoints() == 0;
            case SKILLS ->
            {
                CharacterSkills skills = pendingCharacter.getSkills();
                nextButton.active = skills.isInitialized()
                        && skills.getAvailablePoints() == 0;
            }
            case PROFICIENCIES ->
            {
                CharacterProficiencies proficiencies = pendingCharacter.getProficiencies();
                nextButton.active = proficiencies.isInitialized()
                        && proficiencies.getAvailablePoints() == 0;
            }
            default -> nextButton.active = true;
        }
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

            if (candidate == CharacterCreationPage.SKILLS && !usesSkillsPage())
            {
                nextIndex++;
                continue;
            }

            if (candidate == CharacterCreationPage.PROFICIENCIES)
            {
                proficiencyScrollOffset = 0;
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

            if (candidate == CharacterCreationPage.SKILLS && !usesSkillsPage())
            {
                previousIndex--;
                continue;
            }

            if (candidate == CharacterCreationPage.PROFICIENCIES)
            {
                proficiencyScrollOffset = 0;
            }

            currentPage = candidate;
            buildCurrentPage();
            return;
        }
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick)
    {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                this.font,
                "CHARACTER GENERATION",
                this.width / 2,
                40,
                0xFFFFFF);

        graphics.drawCenteredString(
                this.font,
                getPageTitle(),
                this.width / 2,
                70,
                0xFFFFAA);

        switch (currentPage)
        {
            case GENDER -> renderGenderSelection(graphics);
            case RACE -> renderRaceSelection(graphics);
            case CLASS -> renderClassSelection(graphics);
            case ALIGNMENT -> renderAlignmentSelection(graphics);
            case ABILITIES -> renderAbilities(graphics);
            case SKILLS -> renderSkills(graphics);
            case PROFICIENCIES -> renderProficiencies(graphics);
            default -> { }
        }
    }

    private void renderGenderSelection(GuiGraphics graphics)
    {
        PendingCharacter.Gender gender = pendingCharacter.getGender();

        if (gender == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's gender",
                    this.width / 2,
                    this.height / 2 - 40,
                    0xAAAAAA);
            return;
        }

        graphics.drawCenteredString(
                this.font,
                "Selected: " + formatGender(gender),
                this.width / 2,
                this.height / 2 + 25,
                0xAAFFAA);
    }

    private void renderRaceSelection(GuiGraphics graphics)
    {
        CharacterRace race = pendingCharacter.getRace();

        if (race == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's race",
                    this.width / 2,
                    this.height / 2 - 75,
                    0xAAAAAA);
            return;
        }

        int centerX = this.width / 2;
        int descriptionY = this.height / 2 + 55;

        graphics.drawCenteredString(this.font, race.getDisplayName(), centerX, descriptionY, 0xAAFFAA);
        graphics.drawWordWrap(
                this.font,
                Component.literal(race.getDescription()),
                centerX - 150,
                descriptionY + 18,
                300,
                0xCCCCCC);
    }

    private void renderClassSelection(GuiGraphics graphics)
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();

        if (characterClass == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's class",
                    this.width / 2,
                    this.height / 2 - 85,
                    0xAAAAAA);
            return;
        }

        int centerX = this.width / 2;
        int descriptionY = this.height / 2 + 65;

        graphics.drawCenteredString(this.font, characterClass.getDisplayName(), centerX, descriptionY, 0xAAFFAA);
        graphics.drawWordWrap(
                this.font,
                Component.literal(characterClass.getDescription()),
                centerX - 150,
                descriptionY + 18,
                300,
                0xCCCCCC);
    }

    private void renderAlignmentSelection(GuiGraphics graphics)
    {
        CharacterAlignment alignment = pendingCharacter.getAlignment();

        if (alignment == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's alignment",
                    this.width / 2,
                    this.height / 2 - 95,
                    0xAAAAAA);
            return;
        }

        int centerX = this.width / 2;
        int descriptionY = this.height / 2 + 85;

        graphics.drawCenteredString(this.font, alignment.getDisplayName(), centerX, descriptionY, 0xAAFFAA);
        graphics.drawWordWrap(
                this.font,
                Component.literal(alignment.getDescription()),
                centerX - 150,
                descriptionY + 18,
                300,
                0xCCCCCC);
    }

    private void renderAbilities(GuiGraphics graphics)
    {
        AbilityScores scores = pendingCharacter.getAbilityScores();
        CharacterRace race = pendingCharacter.getRace();
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        int centerX = this.width / 2;

        if (!scores.isRolled())
        {
            graphics.drawCenteredString(this.font, "Roll your ability scores", centerX, 105, 0xAAAAAA);
            return;
        }

        if (race == null || characterClass == null)
        {
            return;
        }

        int startY = 112;
        graphics.drawString(this.font, "Ability", centerX - 120, startY - 20, 0xAAAAAA);
        graphics.drawString(this.font, "Base", centerX + 55, startY - 20, 0xAAAAAA);
        graphics.drawString(this.font, "Race", centerX + 105, startY - 20, 0xAAAAAA);
        graphics.drawString(this.font, "Final", centerX + 155, startY - 20, 0xAAAAAA);

        drawAbility(graphics, "STR", scores.getStrength(),
                CharacterAbilityRules.getStrengthModifier(race),
                formatFinalStrength(scores, race, characterClass),
                CharacterAbilityRules.getMinimumStrength(race, characterClass), startY);
        drawAbility(graphics, "DEX", scores.getDexterity(),
                CharacterAbilityRules.getDexterityModifier(race),
                Integer.toString(scores.getFinalDexterity(race)),
                CharacterAbilityRules.getMinimumDexterity(race, characterClass), startY + 22);
        drawAbility(graphics, "CON", scores.getConstitution(),
                CharacterAbilityRules.getConstitutionModifier(race),
                Integer.toString(scores.getFinalConstitution(race)),
                CharacterAbilityRules.getMinimumConstitution(race, characterClass), startY + 44);
        drawAbility(graphics, "INT", scores.getIntelligence(),
                CharacterAbilityRules.getIntelligenceModifier(race),
                Integer.toString(scores.getFinalIntelligence(race)),
                CharacterAbilityRules.getMinimumIntelligence(race, characterClass), startY + 66);
        drawAbility(graphics, "WIS", scores.getWisdom(),
                CharacterAbilityRules.getWisdomModifier(race),
                Integer.toString(scores.getFinalWisdom(race)),
                CharacterAbilityRules.getMinimumWisdom(race, characterClass), startY + 88);
        drawAbility(graphics, "CHA", scores.getCharisma(),
                CharacterAbilityRules.getCharismaModifier(race),
                Integer.toString(scores.getFinalCharisma(race)),
                CharacterAbilityRules.getMinimumCharisma(race, characterClass), startY + 110);

        graphics.drawCenteredString(this.font,
                "Available Points: " + scores.getAvailablePoints(),
                centerX, startY + 138, 0xFFFFAA);
        graphics.drawCenteredString(this.font,
                "Roll Total: " + scores.getTotal(),
                centerX, startY + 153, 0xAAAAAA);

        if (scores.hasStoredRoll())
        {
            graphics.drawCenteredString(this.font,
                    "Stored Total: " + scores.getStoredTotal(),
                    centerX, startY + 168, 0xAAFFAA);
        }
    }

    private void drawAbility(
            GuiGraphics graphics,
            String name,
            int baseValue,
            int racialModifier,
            String finalValue,
            int minimum,
            int y)
    {
        int centerX = this.width / 2;
        graphics.drawString(this.font, name, centerX - 120, y + 6, 0xFFFFFF);
        graphics.drawString(this.font, "Min " + minimum, centerX - 85, y + 6, 0x888888);
        graphics.drawCenteredString(this.font, Integer.toString(baseValue), centerX + 68, y + 6, 0xFFFFFF);
        graphics.drawCenteredString(this.font, formatModifier(racialModifier), centerX + 118, y + 6,
                racialModifier == 0 ? 0x888888 : 0xFFFFAA);
        graphics.drawCenteredString(this.font, finalValue, centerX + 168, y + 6, 0xAAFFAA);
    }

    private void renderSkills(GuiGraphics graphics)
    {
        CharacterSkills skills = pendingCharacter.getSkills();

        if (!skills.isInitialized())
        {
            return;
        }

        int centerX = this.width / 2;
        int startY = 105;

        drawSkill(graphics, "Open Locks", skills.getOpenLocks(), startY);
        drawSkill(graphics, "Find Traps", skills.getFindTraps(), startY + 22);
        drawSkill(graphics, "Pick Pockets", skills.getPickPockets(), startY + 44);
        drawSkill(graphics, "Move Silently", skills.getMoveSilently(), startY + 66);
        drawSkill(graphics, "Hide in Shadows", skills.getHideInShadows(), startY + 88);
        drawSkill(graphics, "Detect Illusion", skills.getDetectIllusion(), startY + 110);
        drawSkill(graphics, "Set Traps", skills.getSetTraps(), startY + 132);

        graphics.drawCenteredString(this.font,
                "Points Remaining: " + skills.getAvailablePoints(),
                centerX, startY + 165, 0xFFFFAA);
    }

    private void drawSkill(GuiGraphics graphics, String name, int value, int y)
    {
        int centerX = this.width / 2;
        graphics.drawString(this.font, name, centerX - 120, y + 6, 0xFFFFFF);
        graphics.drawCenteredString(this.font, Integer.toString(value), centerX + 5, y + 6, 0xAAFFAA);
    }

    private void renderProficiencies(GuiGraphics graphics)
    {
        CharacterClass characterClass = pendingCharacter.getCharacterClass();
        CharacterProficiencies proficiencies = pendingCharacter.getProficiencies();

        if (characterClass == null || !proficiencies.isInitialized())
        {
            return;
        }

        clampProficiencyScroll();

        int centerX = this.width / 2;
        int labelX = centerX - 155;
        int rankX = centerX + 50;
        int visibleRows = getProficiencyVisibleRows();
        int firstRow = proficiencyScrollOffset;
        int lastRow = firstRow + visibleRows - 1;
        int weaponCount = WeaponProficiency.values().length;

        graphics.drawString(this.font, "Weapon / Style", labelX, PROFICIENCY_START_Y - 18, 0xAAAAAA);
        graphics.drawCenteredString(this.font, "Rank", rankX, PROFICIENCY_START_Y - 18, 0xAAAAAA);

        for (int i = 0; i < weaponCount; i++)
        {
            if (i < firstRow || i > lastRow)
            {
                continue;
            }

            WeaponProficiency proficiency = WeaponProficiency.values()[i];
            boolean allowed = proficiencies.canUseWeapon(characterClass, proficiency);
            int y = getProficiencyRowY(i);

            graphics.drawString(
                    this.font,
                    proficiency.getDisplayName(),
                    labelX,
                    y + 6,
                    allowed ? 0xFFFFFF : 0x777777);

            graphics.drawCenteredString(
                    this.font,
                    formatProficiencyRank(proficiencies.getWeaponRank(proficiency)),
                    rankX,
                    y + 6,
                    allowed ? 0xAAFFAA : 0x666666);
        }

        int styleHeaderRow = weaponCount;
        if (styleHeaderRow >= firstRow && styleHeaderRow <= lastRow)
        {
            int y = getProficiencyRowY(styleHeaderRow);
            graphics.drawString(this.font, "Weapon Styles", labelX, y + 6, 0xFFFFAA);
        }

        FightingStyle[] styles = FightingStyle.values();
        for (int i = 0; i < styles.length; i++)
        {
            int logicalRow = styleHeaderRow + 1 + i;
            if (logicalRow < firstRow || logicalRow > lastRow)
            {
                continue;
            }

            FightingStyle style = styles[i];
            boolean allowed = proficiencies.canUseStyle(characterClass, style);
            int y = getProficiencyRowY(logicalRow);

            graphics.drawString(
                    this.font,
                    style.getDisplayName(),
                    labelX,
                    y + 6,
                    allowed ? 0xFFFFFF : 0x777777);

            graphics.drawCenteredString(
                    this.font,
                    formatProficiencyRank(proficiencies.getStyleRank(style)),
                    rankX,
                    y + 6,
                    allowed ? 0xAAFFAA : 0x666666);
        }

        renderProficiencyScrollbar(graphics);

        int footerY = this.height - 76;
        graphics.drawCenteredString(
                this.font,
                "Proficiency Points Remaining: " + proficiencies.getAvailablePoints(),
                centerX,
                footerY,
                0xFFFFAA);

        graphics.drawCenteredString(
                this.font,
                "Mouse wheel to scroll",
                centerX,
                footerY + 13,
                0x888888);
    }

    private void renderProficiencyScrollbar(GuiGraphics graphics)
    {
        int totalRows = getProficiencyContentRows();
        int visibleRows = getProficiencyVisibleRows();

        if (totalRows <= visibleRows)
        {
            return;
        }

        int centerX = this.width / 2;
        int trackX = centerX + 145;
        int trackTop = PROFICIENCY_START_Y;
        int trackHeight = visibleRows * PROFICIENCY_ROW_HEIGHT - 2;
        int trackBottom = trackTop + trackHeight;

        graphics.fill(trackX, trackTop, trackX + 4, trackBottom, 0xFF333333);

        int thumbHeight = Math.max(18, trackHeight * visibleRows / totalRows);
        int maxScroll = getMaxProficiencyScroll();
        int movable = trackHeight - thumbHeight;
        int thumbTop = trackTop;

        if (maxScroll > 0)
        {
            thumbTop += movable * proficiencyScrollOffset / maxScroll;
        }

        graphics.fill(trackX, thumbTop, trackX + 4, thumbTop + thumbHeight, 0xFFAAAAAA);
    }

    private String formatProficiencyRank(int rank)
    {
        if (rank <= 0)
        {
            return "-";
        }

        return "*".repeat(rank);
    }

    private String formatModifier(int modifier)
    {
        if (modifier > 0)
        {
            return "+" + modifier;
        }
        if (modifier < 0)
        {
            return Integer.toString(modifier);
        }
        return "-";
    }

    private String formatFinalStrength(
            AbilityScores scores,
            CharacterRace race,
            CharacterClass characterClass)
    {
        int finalStrength = scores.getFinalStrength(race);

        if (!CharacterAbilityRules.canHaveExceptionalStrength(race, characterClass, finalStrength))
        {
            return Integer.toString(finalStrength);
        }

        int exceptional = scores.getExceptionalStrength();
        if (exceptional <= 0)
        {
            return Integer.toString(finalStrength);
        }

        String percentile = exceptional == 100
                ? "00"
                : String.format("%02d", exceptional);
        return "18/" + percentile;
    }

    private String formatGender(PendingCharacter.Gender gender)
    {
        return switch (gender)
        {
            case MALE -> "Male";
            case FEMALE -> "Female";
        };
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
            case SPELLS -> "Spells / Divine Abilities";
            case APPEARANCE -> "Appearance";
            case NAME -> "Name";
            case REVIEW -> "Review Character";
        };
    }

    private boolean usesSkillsPage()
    {
        return pendingCharacter.getCharacterClass() == CharacterClass.THIEF;
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
