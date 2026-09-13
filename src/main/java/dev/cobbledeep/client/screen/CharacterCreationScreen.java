package dev.cobbledeep.client.screen;

import dev.cobbledeep.character.AbilityScores;
import dev.cobbledeep.character.CharacterAbilityRules;
import dev.cobbledeep.character.CharacterAlignment;
import dev.cobbledeep.character.CharacterClass;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.CharacterSkills;
import dev.cobbledeep.character.PendingCharacter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

public class CharacterCreationScreen extends Screen
{
    private final PendingCharacter pendingCharacter;

    private CharacterCreationPage currentPage;

    private Button nextButton;

    public CharacterCreationScreen()
    {
        super(Component.literal("Character Generation"));

        this.pendingCharacter = new PendingCharacter();
        this.currentPage = CharacterCreationPage.GENDER;
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
            case GENDER:
                buildGenderPage();
                break;

            case RACE:
                buildRacePage();
                break;

            case CLASS:
                buildClassPage();
                break;

            case ALIGNMENT:
                buildAlignmentPage();
                break;

            case ABILITIES:
                buildAbilitiesPage();
                break;

            case SKILLS:
                buildSkillsPage();
                break;
                
            case PROFICIENCIES:
            case SPELLS:
            case APPEARANCE:
            case NAME:
            case REVIEW:
                buildPlaceholderPage();
                break;
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
                            PendingCharacter.Gender oldGender =
                                    pendingCharacter.getGender();

                            if (oldGender != PendingCharacter.Gender.MALE)
                            {
                                pendingCharacter.setGender(
                                        PendingCharacter.Gender.MALE
                                );
                            }

                            updateNextButton();
                        }
                )
                .bounds(
                        centerX - 105,
                        centerY - 10,
                        100,
                        20
                )
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Female"),
                        button ->
                        {
                            PendingCharacter.Gender oldGender =
                                    pendingCharacter.getGender();

                            if (oldGender != PendingCharacter.Gender.FEMALE)
                            {
                                pendingCharacter.setGender(
                                        PendingCharacter.Gender.FEMALE
                                );
                            }

                            updateNextButton();
                        }
                )
                .bounds(
                        centerX + 5,
                        centerY - 10,
                        100,
                        20
                )
                .build()
        );
    }

    private void buildRacePage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        CharacterRace[] races =
                CharacterRace.values();

        int buttonWidth = 100;
        int buttonHeight = 20;

        int horizontalSpacing = 10;
        int verticalSpacing = 10;

        int leftX =
                centerX
                - buttonWidth
                - horizontalSpacing / 2;

        int rightX =
                centerX
                + horizontalSpacing / 2;

        int startY = centerY - 40;

        for (int i = 0; i < races.length; i++)
        {
            CharacterRace race = races[i];

            int column = i % 2;
            int row = i / 2;

            int x =
                    column == 0
                            ? leftX
                            : rightX;

            int y =
                    startY
                    + row
                    * (buttonHeight + verticalSpacing);

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(
                                    race.getDisplayName()
                            ),
                            button ->
                            {
                                CharacterRace oldRace =
                                        pendingCharacter.getRace();

                                if (oldRace != race)
                                {
                                    pendingCharacter.setRace(race);

                                    pendingCharacter
                                            .setCharacterClass(null);

                                    pendingCharacter
                                            .setAlignment(null);

                                    pendingCharacter
                                            .getAbilityScores()
                                            .reset();
                                }

                                updateNextButton();
                            }
                    )
                    .bounds(
                            x,
                            y,
                            buttonWidth,
                            buttonHeight
                    )
                    .build()
            );
        }
    }

    private void buildClassPage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        CharacterRace race =
                pendingCharacter.getRace();

        if (race == null)
        {
            return;
        }

        CharacterClass[] classes =
                CharacterClass.values();

        int buttonWidth = 100;
        int buttonHeight = 20;

        int horizontalSpacing = 10;
        int verticalSpacing = 8;

        int leftX =
                centerX
                - buttonWidth
                - horizontalSpacing / 2;

        int rightX =
                centerX
                + horizontalSpacing / 2;

        int startY = centerY - 55;

        int visibleIndex = 0;

        for (CharacterClass characterClass : classes)
        {
            if (!race.canChooseClass(characterClass))
            {
                continue;
            }

            int column = visibleIndex % 2;
            int row = visibleIndex / 2;

            int x =
                    column == 0
                            ? leftX
                            : rightX;

            int y =
                    startY
                    + row
                    * (buttonHeight + verticalSpacing);

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(
                                    characterClass.getDisplayName()
                            ),
                            button ->
                            {
                                CharacterClass oldClass =
                                        pendingCharacter
                                                .getCharacterClass();

                                if (oldClass != characterClass)
                                {
                                    pendingCharacter
                                            .setCharacterClass(
                                                    characterClass
                                            );

                                    pendingCharacter
                                            .setAlignment(null);

                                    pendingCharacter
                                            .getAbilityScores()
                                            .reset();
                                }

                                updateNextButton();
                            }
                    )
                    .bounds(
                            x,
                            y,
                            buttonWidth,
                            buttonHeight
                    )
                    .build()
            );

            visibleIndex++;
        }
    }

    private void buildAlignmentPage()
    {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        CharacterClass characterClass =
                pendingCharacter.getCharacterClass();

        if (characterClass == null)
        {
            return;
        }

        CharacterAlignment[] alignments =
                CharacterAlignment.values();

        int buttonWidth = 120;
        int buttonHeight = 20;

        int horizontalSpacing = 10;
        int verticalSpacing = 8;

        int leftX =
                centerX
                - buttonWidth
                - horizontalSpacing / 2;

        int rightX =
                centerX
                + horizontalSpacing / 2;

        int startY = centerY - 70;

        int visibleIndex = 0;

        for (CharacterAlignment alignment : alignments)
        {
            if (!characterClass.canChooseAlignment(alignment))
            {
                continue;
            }

            int column = visibleIndex % 2;
            int row = visibleIndex / 2;

            int x =
                    column == 0
                            ? leftX
                            : rightX;

            int y =
                    startY
                    + row
                    * (buttonHeight + verticalSpacing);

            this.addRenderableWidget(
                    Button.builder(
                            Component.literal(
                                    alignment.getDisplayName()
                            ),
                            button ->
                            {
                                CharacterAlignment oldAlignment =
                                        pendingCharacter
                                                .getAlignment();

                                if (oldAlignment != alignment)
                                {
                                    pendingCharacter
                                            .setAlignment(alignment);

                                    pendingCharacter
                                            .getAbilityScores()
                                            .reset();
                                }

                                updateNextButton();
                            }
                    )
                    .bounds(
                            x,
                            y,
                            buttonWidth,
                            buttonHeight
                    )
                    .build()
            );

            visibleIndex++;
        }
    }

    private void buildAbilitiesPage()
    {
        int centerX = this.width / 2;
        int startY = 112;

        AbilityScores scores =
                pendingCharacter.getAbilityScores();

        CharacterRace race =
                pendingCharacter.getRace();

        CharacterClass characterClass =
                pendingCharacter.getCharacterClass();

        if (race == null || characterClass == null)
        {
            return;
        }

        Button storeButton =
                Button.builder(
                        Component.literal("Store"),
                        button ->
                        {
                            scores.storeCurrentRoll();
                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX - 155,
                        startY + 170,
                        100,
                        20
                )
                .build();

        storeButton.active =
                scores.isRolled();

        this.addRenderableWidget(storeButton);

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("Reroll"),
                        button ->
                        {
                            scores.roll(
                                    race,
                                    characterClass
                            );

                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX - 50,
                        startY + 170,
                        100,
                        20
                )
                .build()
        );

        Button recallButton =
                Button.builder(
                        Component.literal("Recall"),
                        button ->
                        {
                            scores.recallStoredRoll();
                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX + 55,
                        startY + 170,
                        100,
                        20
                )
                .build();

        recallButton.active =
                scores.hasStoredRoll();

        this.addRenderableWidget(recallButton);

        if (!scores.isRolled())
        {
            return;
        }

        addAbilityButtons(
                centerX,
                startY,
                () -> scores.decreaseStrength(
                        race,
                        characterClass
                ),
                () -> scores.increaseStrength(
                        race,
                        characterClass
                )
        );

        addAbilityButtons(
                centerX,
                startY + 22,
                () -> scores.decreaseDexterity(
                        race,
                        characterClass
                ),
                () -> scores.increaseDexterity(race)
        );

        addAbilityButtons(
                centerX,
                startY + 44,
                () -> scores.decreaseConstitution(
                        race,
                        characterClass
                ),
                () -> scores.increaseConstitution(race)
        );

        addAbilityButtons(
                centerX,
                startY + 66,
                () -> scores.decreaseIntelligence(
                        race,
                        characterClass
                ),
                () -> scores.increaseIntelligence(race)
        );

        addAbilityButtons(
                centerX,
                startY + 88,
                () -> scores.decreaseWisdom(
                        race,
                        characterClass
                ),
                () -> scores.increaseWisdom(race)
        );

        addAbilityButtons(
                centerX,
                startY + 110,
                () -> scores.decreaseCharisma(
                        race,
                        characterClass
                ),
                () -> scores.increaseCharisma(race)
        );
    }

    private void addAbilityButtons(
            int centerX,
            int y,
            Runnable decrease,
            Runnable increase)
    {
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("-"),
                        button ->
                        {
                            int oldPool =
                                    pendingCharacter
                                            .getAbilityScores()
                                            .getAvailablePoints();

                            decrease.run();

                            int newPool =
                                    pendingCharacter
                                            .getAbilityScores()
                                            .getAvailablePoints();

                            if (newPool != oldPool)
                            {
                                pendingCharacter
                                        .resetAfterAbilitiesChange();
                            }

                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX - 20,
                        y,
                        20,
                        20
                )
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("+"),
                        button ->
                        {
                            int oldPool =
                                    pendingCharacter
                                            .getAbilityScores()
                                            .getAvailablePoints();

                            increase.run();

                            int newPool =
                                    pendingCharacter
                                            .getAbilityScores()
                                            .getAvailablePoints();

                            if (newPool != oldPool)
                            {
                                pendingCharacter
                                        .resetAfterAbilitiesChange();
                            }

                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX + 20,
                        y,
                        20,
                        20
                )
                .build()
        );
    }

    private void buildPlaceholderPage()
    {
        // Placeholder for pages not implemented yet.
    }

    private void buildNavigationButtons()
    {
        int centerX = this.width / 2;
        int bottomY = this.height - 40;

        Button backButton =
                Button.builder(
                        Component.literal(
                                currentPage
                                        == CharacterCreationPage.GENDER
                                                ? "Back to Title"
                                                : "Back"
                        ),
                        button -> previousPage()
                )
                .bounds(
                        centerX - 105,
                        bottomY,
                        100,
                        20
                )
                .build();

        this.addRenderableWidget(backButton);

        nextButton =
                Button.builder(
                        Component.literal(
                                currentPage
                                        == CharacterCreationPage.REVIEW
                                                ? "Finish"
                                                : "Next"
                        ),
                        button -> nextPage()
                )
                .bounds(
                        centerX + 5,
                        bottomY,
                        100,
                        20
                )
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
            case GENDER:
                nextButton.active =
                        pendingCharacter.getGender() != null;
                break;

            case RACE:
                nextButton.active =
                        pendingCharacter.getRace() != null;
                break;

            case CLASS:
                nextButton.active =
                        pendingCharacter.getCharacterClass() != null;
                break;

            case ALIGNMENT:
                nextButton.active =
                        pendingCharacter.getAlignment() != null;
                break;

            case ABILITIES:
                nextButton.active =
                        pendingCharacter
                                .getAbilityScores()
                                .isRolled()
                        && pendingCharacter
                                .getAbilityScores()
                                .getAvailablePoints() == 0;
                break;
                      
            case SKILLS:
            {
                CharacterClass characterClass =
                        pendingCharacter.getCharacterClass();

                CharacterSkills skills =
                        pendingCharacter.getSkills();

                if (characterClass == CharacterClass.THIEF)
                {
                    nextButton.active =
                            skills.isInitialized()
                            && skills.getAvailablePoints() == 0;
                }
                else
                {
                    nextButton.active = true;
                }

                break;
            }

            default:
                nextButton.active = true;
                break;
        }
    }

    private void nextPage()
    {
        if (currentPage
                == CharacterCreationPage.REVIEW)
        {
            continueToWorldCreation();
            return;
        }

        CharacterCreationPage[] pages =
                CharacterCreationPage.values();

        int nextIndex =
                currentPage.ordinal() + 1;

        while (nextIndex < pages.length)
        {
            CharacterCreationPage candidate =
                    pages[nextIndex];

            if (candidate
                    == CharacterCreationPage.SKILLS
                    && !usesSkillsPage())
            {
                nextIndex++;
                continue;
            }

            currentPage = candidate;
            buildCurrentPage();
            return;
        }
    }

    private void previousPage()
    {
        if (currentPage
                == CharacterCreationPage.GENDER)
        {
            returnToTitle();
            return;
        }

        CharacterCreationPage[] pages =
                CharacterCreationPage.values();

        int previousIndex =
                currentPage.ordinal() - 1;

        while (previousIndex >= 0)
        {
            CharacterCreationPage candidate =
                    pages[previousIndex];

            if (candidate
                    == CharacterCreationPage.SKILLS
                    && !usesSkillsPage())
            {
                previousIndex--;
                continue;
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
        this.renderBackground(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );

        graphics.drawCenteredString(
                this.font,
                "CHARACTER GENERATION",
                this.width / 2,
                40,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                getPageTitle(),
                this.width / 2,
                70,
                0xFFFFAA
        );

        if (currentPage == CharacterCreationPage.GENDER)
        {
            renderGenderSelection(graphics);
        }

        if (currentPage == CharacterCreationPage.RACE)
        {
            renderRaceSelection(graphics);
        }

        if (currentPage == CharacterCreationPage.CLASS)
        {
            renderClassSelection(graphics);
        }

        if (currentPage == CharacterCreationPage.ALIGNMENT)
        {
            renderAlignmentSelection(graphics);
        }

        if (currentPage == CharacterCreationPage.ABILITIES)
        {
            renderAbilities(graphics);
        }
        if (currentPage == CharacterCreationPage.SKILLS)
        {
            renderSkills(graphics);
        }
    }

    private void renderGenderSelection(
            GuiGraphics graphics)
    {
        PendingCharacter.Gender gender =
                pendingCharacter.getGender();

        if (gender == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's gender",
                    this.width / 2,
                    this.height / 2 - 40,
                    0xAAAAAA
            );

            return;
        }

        graphics.drawCenteredString(
                this.font,
                "Selected: " + formatGender(gender),
                this.width / 2,
                this.height / 2 + 25,
                0xAAFFAA
        );
    }

    private void renderRaceSelection(
            GuiGraphics graphics)
    {
        CharacterRace race =
                pendingCharacter.getRace();

        if (race == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's race",
                    this.width / 2,
                    this.height / 2 - 75,
                    0xAAAAAA
            );

            return;
        }

        int centerX = this.width / 2;
        int descriptionY =
                this.height / 2 + 55;

        graphics.drawCenteredString(
                this.font,
                race.getDisplayName(),
                centerX,
                descriptionY,
                0xAAFFAA
        );

        graphics.drawWordWrap(
                this.font,
                Component.literal(
                        race.getDescription()
                ),
                centerX - 150,
                descriptionY + 18,
                300,
                0xCCCCCC
        );
    }

    private void renderClassSelection(
            GuiGraphics graphics)
    {
        CharacterClass characterClass =
                pendingCharacter.getCharacterClass();

        if (characterClass == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's class",
                    this.width / 2,
                    this.height / 2 - 85,
                    0xAAAAAA
            );

            return;
        }

        int centerX = this.width / 2;
        int descriptionY =
                this.height / 2 + 65;

        graphics.drawCenteredString(
                this.font,
                characterClass.getDisplayName(),
                centerX,
                descriptionY,
                0xAAFFAA
        );

        graphics.drawWordWrap(
                this.font,
                Component.literal(
                        characterClass.getDescription()
                ),
                centerX - 150,
                descriptionY + 18,
                300,
                0xCCCCCC
        );
    }

    private void renderAlignmentSelection(
            GuiGraphics graphics)
    {
        CharacterAlignment alignment =
                pendingCharacter.getAlignment();

        if (alignment == null)
        {
            graphics.drawCenteredString(
                    this.font,
                    "Choose your character's alignment",
                    this.width / 2,
                    this.height / 2 - 95,
                    0xAAAAAA
            );

            return;
        }

        int centerX = this.width / 2;
        int descriptionY =
                this.height / 2 + 85;

        graphics.drawCenteredString(
                this.font,
                alignment.getDisplayName(),
                centerX,
                descriptionY,
                0xAAFFAA
        );

        graphics.drawWordWrap(
                this.font,
                Component.literal(
                        alignment.getDescription()
                ),
                centerX - 150,
                descriptionY + 18,
                300,
                0xCCCCCC
        );
    }

    private void renderAbilities(
            GuiGraphics graphics)
    {
        AbilityScores scores =
                pendingCharacter.getAbilityScores();

        CharacterRace race =
                pendingCharacter.getRace();

        CharacterClass characterClass =
                pendingCharacter.getCharacterClass();

        int centerX = this.width / 2;

        if (!scores.isRolled())
        {
            graphics.drawCenteredString(
                    this.font,
                    "Roll your ability scores",
                    centerX,
                    105,
                    0xAAAAAA
            );

            return;
        }

        if (race == null || characterClass == null)
        {
            return;
        }

        int startY = 112;

        graphics.drawString(
                this.font,
                "Ability",
                centerX - 120,
                startY - 20,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                "Base",
                centerX + 55,
                startY - 20,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                "Race",
                centerX + 105,
                startY - 20,
                0xAAAAAA
        );

        graphics.drawString(
                this.font,
                "Final",
                centerX + 155,
                startY - 20,
                0xAAAAAA
        );

        drawAbility(
                graphics,
                "STR",
                scores.getStrength(),
                CharacterAbilityRules
                        .getStrengthModifier(race),
                formatFinalStrength(
                        scores,
                        race,
                        characterClass
                ),
                CharacterAbilityRules
                        .getMinimumStrength(
                                race,
                                characterClass),
                startY
        );

        drawAbility(
                graphics,
                "DEX",
                scores.getDexterity(),
                CharacterAbilityRules
                        .getDexterityModifier(race),
                Integer.toString(
                        scores.getFinalDexterity(race)
                ),
                CharacterAbilityRules
                        .getMinimumDexterity(
                                race,
                                characterClass),
                startY + 22
        );

        drawAbility(
                graphics,
                "CON",
                scores.getConstitution(),
                CharacterAbilityRules
                        .getConstitutionModifier(race),
                Integer.toString(
                        scores.getFinalConstitution(race)
                ),
                CharacterAbilityRules
                        .getMinimumConstitution(
                                race,
                                characterClass),
                startY + 44
        );

        drawAbility(
                graphics,
                "INT",
                scores.getIntelligence(),
                CharacterAbilityRules
                        .getIntelligenceModifier(race),
                Integer.toString(
                        scores.getFinalIntelligence(race)
                ),
                CharacterAbilityRules
                        .getMinimumIntelligence(
                                race,
                                characterClass),
                startY + 66
        );

        drawAbility(
                graphics,
                "WIS",
                scores.getWisdom(),
                CharacterAbilityRules
                        .getWisdomModifier(race),
                Integer.toString(
                        scores.getFinalWisdom(race)
                ),
                CharacterAbilityRules
                        .getMinimumWisdom(
                                race,
                                characterClass),
                startY + 88
        );

        drawAbility(
                graphics,
                "CHA",
                scores.getCharisma(),
                CharacterAbilityRules
                        .getCharismaModifier(race),
                Integer.toString(
                        scores.getFinalCharisma(race)
                ),
                CharacterAbilityRules
                        .getMinimumCharisma(
                                race,
                                characterClass),
                startY + 110
        );

        graphics.drawCenteredString(
                this.font,
                "Available Points: "
                        + scores.getAvailablePoints(),
                centerX,
                startY + 138,
                0xFFFFAA
        );

        graphics.drawCenteredString(
                this.font,
                "Roll Total: "
                        + scores.getTotal(),
                centerX,
                startY + 153,
                0xAAAAAA
        );
        
        if (scores.hasStoredRoll())
        {
            graphics.drawCenteredString(
                    this.font,
                    "Stored Total: "
                            + scores.getStoredTotal(),
                    centerX,
                    startY + 168,
                    0xAAFFAA
            );
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

        graphics.drawString(
                this.font,
                name,
                centerX - 120,
                y + 6,
                0xFFFFFF
        );

        graphics.drawString(
                this.font,
                "Min " + minimum,
                centerX - 85,
                y + 6,
                0x888888
        );

        graphics.drawCenteredString(
                this.font,
                Integer.toString(baseValue),
                centerX + 68,
                y + 6,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                formatModifier(racialModifier),
                centerX + 118,
                y + 6,
                racialModifier == 0
                        ? 0x888888
                        : 0xFFFFAA
        );

        graphics.drawCenteredString(
                this.font,
                finalValue,
                centerX + 168,
                y + 6,
                0xAAFFAA
        );
    }

    private String formatModifier(
            int modifier)
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
        int finalStrength =
                scores.getFinalStrength(race);

        if (!CharacterAbilityRules
                .canHaveExceptionalStrength(
                        race,
                        characterClass,
                        finalStrength))
        {
            return Integer.toString(finalStrength);
        }

        int exceptional =
                scores.getExceptionalStrength();

        if (exceptional <= 0)
        {
            return Integer.toString(finalStrength);
        }

        String percentile =
                exceptional == 100
                        ? "00"
                        : String.format(
                                "%02d",
                                exceptional
                        );

        return "18/" + percentile;
    }

    private String formatGender(
            PendingCharacter.Gender gender)
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
            case GENDER ->
                "Gender";

            case RACE ->
                "Race";

            case CLASS ->
                "Class";

            case ALIGNMENT ->
                "Alignment";

            case ABILITIES ->
                "Ability Scores";

            case SKILLS ->
                "Skills / Class Abilities";

            case PROFICIENCIES ->
                "Weapon Proficiencies";

            case SPELLS ->
                "Spells / Divine Abilities";

            case APPEARANCE ->
                "Appearance";

            case NAME ->
                "Name";

            case REVIEW ->
                "Review Character";
        };
    }

    @Override
    public void onClose()
    {
        previousPage();
    }

    private void returnToTitle()
    {
        Minecraft.getInstance().setScreen(
                new TitleScreen()
        );
    }

    private void continueToWorldCreation()
    {
        CreateWorldScreen.openFresh(
                Minecraft.getInstance(),
                this
        );
    }
    
    private void buildSkillsPage()
    {
        int centerX = this.width / 2;
        int startY = 105;

        CharacterClass characterClass =
                pendingCharacter.getCharacterClass();

        CharacterRace race =
                pendingCharacter.getRace();

        AbilityScores abilityScores =
                pendingCharacter.getAbilityScores();

        CharacterSkills skills =
                pendingCharacter.getSkills();

        if (characterClass == null
                || race == null
                || !abilityScores.isRolled())
        {
            return;
        }

        if (!skills.isInitialized())
        {
            skills.initialize(
                    race,
                    characterClass,
                    abilityScores
            );
        }

        if (characterClass != CharacterClass.THIEF)
        {
            return;
        }

        addSkillButtons(
                centerX,
                startY,
                skills::decreaseOpenLocks,
                skills::increaseOpenLocks
        );

        addSkillButtons(
                centerX,
                startY + 22,
                skills::decreaseFindTraps,
                skills::increaseFindTraps
        );

        addSkillButtons(
                centerX,
                startY + 44,
                skills::decreasePickPockets,
                skills::increasePickPockets
        );

        addSkillButtons(
                centerX,
                startY + 66,
                skills::decreaseMoveSilently,
                skills::increaseMoveSilently
        );

        addSkillButtons(
                centerX,
                startY + 88,
                skills::decreaseHideInShadows,
                skills::increaseHideInShadows
        );

        addSkillButtons(
                centerX,
                startY + 110,
                skills::decreaseDetectIllusion,
                skills::increaseDetectIllusion
        );

        addSkillButtons(
                centerX,
                startY + 132,
                skills::decreaseSetTraps,
                skills::increaseSetTraps
        );
    }
    
    private void addSkillButtons(
            int centerX,
            int y,
            Runnable decrease,
            Runnable increase)
    {
        this.addRenderableWidget(
                Button.builder(
                        Component.literal("-"),
                        button ->
                        {
                            decrease.run();
                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX + 35,
                        y,
                        20,
                        20
                )
                .build()
        );

        this.addRenderableWidget(
                Button.builder(
                        Component.literal("+"),
                        button ->
                        {
                            increase.run();
                            buildCurrentPage();
                        }
                )
                .bounds(
                        centerX + 60,
                        y,
                        20,
                        20
                )
                .build()
        );
    }
    
    private void renderSkills(
            GuiGraphics graphics)
    {
        CharacterClass characterClass =
                pendingCharacter.getCharacterClass();

        CharacterSkills skills =
                pendingCharacter.getSkills();

        if (characterClass == null)
        {
            return;
        }

        int centerX = this.width / 2;

        if (characterClass != CharacterClass.THIEF)
        {
            renderClassAbilitySummary(
                    graphics,
                    characterClass
            );

            return;
        }

        if (!skills.isInitialized())
        {
            return;
        }

        int startY = 105;

        drawSkill(
                graphics,
                "Open Locks",
                skills.getOpenLocks(),
                startY
        );

        drawSkill(
                graphics,
                "Find Traps",
                skills.getFindTraps(),
                startY + 22
        );

        drawSkill(
                graphics,
                "Pick Pockets",
                skills.getPickPockets(),
                startY + 44
        );

        drawSkill(
                graphics,
                "Move Silently",
                skills.getMoveSilently(),
                startY + 66
        );

        drawSkill(
                graphics,
                "Hide in Shadows",
                skills.getHideInShadows(),
                startY + 88
        );

        drawSkill(
                graphics,
                "Detect Illusion",
                skills.getDetectIllusion(),
                startY + 110
        );

        drawSkill(
                graphics,
                "Set Traps",
                skills.getSetTraps(),
                startY + 132
        );

        graphics.drawCenteredString(
                this.font,
                "Points Remaining: "
                        + skills.getAvailablePoints(),
                centerX,
                startY + 165,
                0xFFFFAA
        );
    }
    
    private void drawSkill(
            GuiGraphics graphics,
            String name,
            int value,
            int y)
    {
        int centerX = this.width / 2;

        graphics.drawString(
                this.font,
                name,
                centerX - 120,
                y + 6,
                0xFFFFFF
        );

        graphics.drawCenteredString(
                this.font,
                Integer.toString(value),
                centerX + 5,
                y + 6,
                0xAAFFAA
        );
    }
    
    private void renderClassAbilitySummary(
            GuiGraphics graphics,
            CharacterClass characterClass)
    {
        int centerX = this.width / 2;
        int startY = 115;

        String title;
        String description;

        switch (characterClass)
        {
            case FIGHTER:
                title = "Fighter Abilities";
                description =
                        "Fighters specialize in combat and gain superior "
                        + "weapon proficiency progression. They have no "
                        + "skill points to allocate on this page.";
                break;

            case RANGER:
                title = "Ranger Abilities";
                description =
                        "Rangers are skilled wilderness warriors with "
                        + "tracking, stealth, and specialized combat abilities. "
                        + "Additional ranger choices will be added later.";
                break;

            case PALADIN:
                title = "Paladin Abilities";
                description =
                        "Paladins possess holy abilities such as Lay on Hands, "
                        + "divine protections, and eventually Turn Undead.";
                break;

            case CLERIC:
                title = "Cleric Abilities";
                description =
                        "Clerics channel divine power, cast priest spells, "
                        + "and can Turn Undead.";
                break;

            case DRUID:
                title = "Druid Abilities";
                description =
                        "Druids wield nature magic and later gain abilities "
                        + "such as shapeshifting.";
                break;

            case MAGE:
                title = "Mage Abilities";
                description =
                        "Mages cast arcane spells using a spellbook and "
                        + "memorization system. Spell selection occurs "
                        + "on the Spells page.";
                break;

            case BARD:
                title = "Bard Abilities";
                description =
                        "Bards combine combat, lore, music, thieving talents, "
                        + "and arcane spellcasting. More bard-specific choices "
                        + "will be added later.";
                break;

            case THIEF:
                return;

            default:
                return;
        }

        graphics.drawCenteredString(
                this.font,
                title,
                centerX,
                startY,
                0xAAFFAA
        );

        graphics.drawWordWrap(
                this.font,
                Component.literal(description),
                centerX - 150,
                startY + 25,
                300,
                0xCCCCCC
        );
    }
    
    private boolean usesSkillsPage()
    {
        return pendingCharacter.getCharacterClass()
                == CharacterClass.THIEF;
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}