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

    public void resetAfterClassChange()
    {
        alignment = null;

        abilityScores.reset();
        skills.reset();
    }

    public void resetAfterAlignmentChange()
    {
        abilityScores.reset();
        skills.reset();
    }

    public void resetAfterAbilitiesChange()
    {
        skills.reset();
    }
}