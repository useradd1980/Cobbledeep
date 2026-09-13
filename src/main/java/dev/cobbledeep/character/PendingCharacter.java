package dev.cobbledeep.character;

public class PendingCharacter
{
    public enum Gender
    {
        MALE,
        FEMALE
    }

    private Gender gender;
    private CharacterRace race;
    private CharacterClass characterClass;
    private CharacterAlignment alignment;

    private final AbilityScores abilityScores =
            new AbilityScores();

    private final CharacterSkills skills =
            new CharacterSkills();

    private final CharacterProficiencies proficiencies =
            new CharacterProficiencies();

    private final CharacterSpells spells =
            new CharacterSpells();

    public Gender getGender()
    {
        return gender;
    }

    public void setGender(Gender gender)
    {
        this.gender = gender;
    }

    public CharacterRace getRace()
    {
        return race;
    }

    public void setRace(CharacterRace race)
    {
        this.race = race;
    }

    public CharacterClass getCharacterClass()
    {
        return characterClass;
    }

    public void setCharacterClass(
            CharacterClass characterClass)
    {
        this.characterClass = characterClass;
    }

    public CharacterAlignment getAlignment()
    {
        return alignment;
    }

    public void setAlignment(
            CharacterAlignment alignment)
    {
        this.alignment = alignment;
    }

    public AbilityScores getAbilityScores()
    {
        return abilityScores;
    }

    public CharacterSkills getSkills()
    {
        return skills;
    }

    public CharacterProficiencies getProficiencies()
    {
        return proficiencies;
    }

    public CharacterSpells getSpells()
    {
        return spells;
    }

    public void resetAfterClassChange()
    {
        alignment = null;

        abilityScores.reset();
        skills.reset();
        proficiencies.reset();
        spells.reset();
    }

    public void resetAfterAlignmentChange()
    {
        abilityScores.reset();
        skills.reset();
        proficiencies.reset();
        spells.reset();
    }

    public void resetAfterAbilitiesChange()
    {
        skills.reset();
        proficiencies.reset();
        spells.reset();
    }
}
