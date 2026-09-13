package dev.cobbledeep.character;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class CharacterData
{
    private boolean characterCreated = false;
    private String name = "";
    private PendingCharacter.Gender gender;
    private CharacterRace race;
    private CharacterClass characterClass;
    private CharacterAlignment alignment;
    private final CharacterAppearance appearance = new CharacterAppearance();

    public boolean isCharacterCreated() { return characterCreated; }
    public void setCharacterCreated(boolean characterCreated) { this.characterCreated = characterCreated; }

    public String getName() { return name; }
    public PendingCharacter.Gender getGender() { return gender; }
    public CharacterRace getRace() { return race; }
    public CharacterClass getCharacterClass() { return characterClass; }
    public CharacterAlignment getAlignment() { return alignment; }
    public CharacterAppearance getAppearance() { return appearance; }

    public void setIdentity(
            String name,
            PendingCharacter.Gender gender,
            CharacterRace race,
            CharacterClass characterClass,
            CharacterAlignment alignment,
            CharacterAppearance sourceAppearance)
    {
        this.name = name == null ? "" : name.trim();
        this.gender = gender;
        this.race = race;
        this.characterClass = characterClass;
        this.alignment = alignment;
        copyAppearance(sourceAppearance, appearance);
        this.characterCreated = !this.name.isBlank()
                && gender != null
                && race != null
                && characterClass != null
                && alignment != null;
    }

    public void copyFrom(CharacterData other)
    {
        if (other == null) return;
        characterCreated = other.characterCreated;
        name = other.name;
        gender = other.gender;
        race = other.race;
        characterClass = other.characterClass;
        alignment = other.alignment;
        copyAppearance(other.appearance, appearance);
    }

    private static void copyAppearance(CharacterAppearance source, CharacterAppearance target)
    {
        if (source == null || target == null) return;
        target.setSkinTone(source.getSkinTone());
        target.setHairStyle(source.getHairStyle());
        target.setHairColor(source.getHairColor());
        target.setEyeColor(source.getEyeColor());
        target.setFacialHair(source.getFacialHair());
        target.setShirtColor(source.getShirtColor());
        target.setTrouserColor(source.getTrouserColor());
    }

    public void saveNBTData(CompoundTag tag)
    {
        tag.putBoolean("characterCreated", characterCreated);
        tag.putString("name", name);
        putEnum(tag, "gender", gender);
        putEnum(tag, "race", race);
        putEnum(tag, "class", characterClass);
        putEnum(tag, "alignment", alignment);

        CompoundTag appearanceTag = new CompoundTag();
        putEnum(appearanceTag, "skinTone", appearance.getSkinTone());
        putEnum(appearanceTag, "hairStyle", appearance.getHairStyle());
        putEnum(appearanceTag, "hairColor", appearance.getHairColor());
        putEnum(appearanceTag, "eyeColor", appearance.getEyeColor());
        putEnum(appearanceTag, "facialHair", appearance.getFacialHair());
        putEnum(appearanceTag, "shirtColor", appearance.getShirtColor());
        putEnum(appearanceTag, "trouserColor", appearance.getTrouserColor());
        tag.put("appearance", appearanceTag);
    }

    public void loadNBTData(CompoundTag tag)
    {
        characterCreated = tag.getBoolean("characterCreated");
        name = tag.getString("name");
        gender = readEnum(tag, "gender", PendingCharacter.Gender.class, null);
        race = readEnum(tag, "race", CharacterRace.class, null);
        characterClass = readEnum(tag, "class", CharacterClass.class, null);
        alignment = readEnum(tag, "alignment", CharacterAlignment.class, null);

        appearance.reset();
        if (tag.contains("appearance"))
        {
            CompoundTag a = tag.getCompound("appearance");
            appearance.setSkinTone(readEnum(a, "skinTone", CharacterAppearance.SkinTone.class, appearance.getSkinTone()));
            appearance.setHairStyle(readEnum(a, "hairStyle", CharacterAppearance.HairStyle.class, appearance.getHairStyle()));
            appearance.setHairColor(readEnum(a, "hairColor", CharacterAppearance.HairColor.class, appearance.getHairColor()));
            appearance.setEyeColor(readEnum(a, "eyeColor", CharacterAppearance.EyeColor.class, appearance.getEyeColor()));
            appearance.setFacialHair(readEnum(a, "facialHair", CharacterAppearance.FacialHair.class, appearance.getFacialHair()));
            appearance.setShirtColor(readEnum(a, "shirtColor", CharacterAppearance.ClothingColor.class, appearance.getShirtColor()));
            appearance.setTrouserColor(readEnum(a, "trouserColor", CharacterAppearance.ClothingColor.class, appearance.getTrouserColor()));
        }
    }

    private static void putEnum(CompoundTag tag, String key, Enum<?> value)
    {
        if (value != null) tag.putString(key, value.name());
    }

    private static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, Class<E> type, E fallback)
    {
        if (!tag.contains(key)) return fallback;
        try
        {
            return Enum.valueOf(type, tag.getString(key));
        }
        catch (IllegalArgumentException ignored)
        {
            return fallback;
        }
    }
}
