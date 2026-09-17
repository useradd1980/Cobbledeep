package dev.cobbledeep.character;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
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
    private int level = 1;
    private final CharacterAppearance appearance = new CharacterAppearance();

    private int strength;
    private int dexterity;
    private int constitution;
    private int intelligence;
    private int wisdom;
    private int charisma;
    private int exceptionalStrength;

    private int openLocks;
    private int findTraps;
    private int pickPockets;
    private int moveSilently;
    private int hideInShadows;
    private int detectIllusion;
    private int setTraps;

    private final EnumMap<WeaponProficiency, Integer> weaponRanks =
            new EnumMap<>(WeaponProficiency.class);
    private final EnumMap<FightingStyle, Integer> styleRanks =
            new EnumMap<>(FightingStyle.class);

    private final EnumSet<MageSpell> knownMageSpells = EnumSet.noneOf(MageSpell.class);
    private MageSpell memorizedMageSpell;
    private final EnumMap<DivineSpell, Integer> memorizedDivineSpells =
            new EnumMap<>(DivineSpell.class);

    public CharacterData()
    {
        clearRanks();
    }

    public boolean isCharacterCreated() { return characterCreated; }
    public void setCharacterCreated(boolean characterCreated) { this.characterCreated = characterCreated; }

    public String getName() { return name; }
    public PendingCharacter.Gender getGender() { return gender; }
    public CharacterRace getRace() { return race; }
    public CharacterClass getCharacterClass() { return characterClass; }
    public CharacterAlignment getAlignment() { return alignment; }
    public int getLevel() { return level; }
    public CharacterAppearance getAppearance() { return appearance; }

    public int getStrength() { return strength; }
    public int getDexterity() { return dexterity; }
    public int getConstitution() { return constitution; }
    public int getIntelligence() { return intelligence; }
    public int getWisdom() { return wisdom; }
    public int getCharisma() { return charisma; }
    public int getExceptionalStrength() { return exceptionalStrength; }

    public int getOpenLocks() { return openLocks; }
    public int getFindTraps() { return findTraps; }
    public int getPickPockets() { return pickPockets; }
    public int getMoveSilently() { return moveSilently; }
    public int getHideInShadows() { return hideInShadows; }
    public int getDetectIllusion() { return detectIllusion; }
    public int getSetTraps() { return setTraps; }

    public int getWeaponRank(WeaponProficiency proficiency)
    {
        return weaponRanks.getOrDefault(proficiency, 0);
    }

    public int getStyleRank(FightingStyle style)
    {
        return styleRanks.getOrDefault(style, 0);
    }

    public Set<MageSpell> getKnownMageSpells()
    {
        return Collections.unmodifiableSet(knownMageSpells);
    }

    public MageSpell getMemorizedMageSpell() { return memorizedMageSpell; }

    public Map<DivineSpell, Integer> getMemorizedDivineSpells()
    {
        return Collections.unmodifiableMap(memorizedDivineSpells);
    }

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

    public void setFromPending(PendingCharacter pending)
    {
        if (pending == null) return;

        level = 1;

        setIdentity(
                pending.getName(),
                pending.getGender(),
                pending.getRace(),
                pending.getCharacterClass(),
                pending.getAlignment(),
                pending.getAppearance());

        AbilityScores abilities = pending.getAbilityScores();
        strength = abilities.getStrength();
        dexterity = abilities.getDexterity();
        constitution = abilities.getConstitution();
        intelligence = abilities.getIntelligence();
        wisdom = abilities.getWisdom();
        charisma = abilities.getCharisma();
        exceptionalStrength = abilities.getExceptionalStrength();

        CharacterSkills skills = pending.getSkills();
        openLocks = skills.getOpenLocks();
        findTraps = skills.getFindTraps();
        pickPockets = skills.getPickPockets();
        moveSilently = skills.getMoveSilently();
        hideInShadows = skills.getHideInShadows();
        detectIllusion = skills.getDetectIllusion();
        setTraps = skills.getSetTraps();

        clearRanks();
        CharacterProficiencies proficiencies = pending.getProficiencies();
        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            weaponRanks.put(proficiency, proficiencies.getWeaponRank(proficiency));
        }
        for (FightingStyle style : FightingStyle.values())
        {
            styleRanks.put(style, proficiencies.getStyleRank(style));
        }

        knownMageSpells.clear();
        knownMageSpells.addAll(pending.getSpells().getKnownMageSpells());
        memorizedMageSpell = pending.getSpells().getMemorizedMageSpell();
        memorizedDivineSpells.clear();
        memorizedDivineSpells.putAll(pending.getSpells().getMemorizedDivineSpells());
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
        level = other.level;
        copyAppearance(other.appearance, appearance);

        strength = other.strength;
        dexterity = other.dexterity;
        constitution = other.constitution;
        intelligence = other.intelligence;
        wisdom = other.wisdom;
        charisma = other.charisma;
        exceptionalStrength = other.exceptionalStrength;

        openLocks = other.openLocks;
        findTraps = other.findTraps;
        pickPockets = other.pickPockets;
        moveSilently = other.moveSilently;
        hideInShadows = other.hideInShadows;
        detectIllusion = other.detectIllusion;
        setTraps = other.setTraps;

        weaponRanks.clear();
        weaponRanks.putAll(other.weaponRanks);
        styleRanks.clear();
        styleRanks.putAll(other.styleRanks);

        knownMageSpells.clear();
        knownMageSpells.addAll(other.knownMageSpells);
        memorizedMageSpell = other.memorizedMageSpell;
        memorizedDivineSpells.clear();
        memorizedDivineSpells.putAll(other.memorizedDivineSpells);
    }

    public void writeNetwork(FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(characterCreated);
        buffer.writeUtf(name == null ? "" : name, 32);
        if (!characterCreated) return;

        buffer.writeEnum(gender);
        buffer.writeEnum(race);
        buffer.writeEnum(characterClass);
        buffer.writeEnum(alignment);
        buffer.writeInt(level);

        buffer.writeEnum(appearance.getSkinTone());
        buffer.writeEnum(appearance.getHairStyle());
        buffer.writeEnum(appearance.getHairColor());
        buffer.writeEnum(appearance.getEyeColor());
        buffer.writeEnum(appearance.getFacialHair());
        buffer.writeEnum(appearance.getShirtColor());
        buffer.writeEnum(appearance.getTrouserColor());

        buffer.writeInt(strength);
        buffer.writeInt(dexterity);
        buffer.writeInt(constitution);
        buffer.writeInt(intelligence);
        buffer.writeInt(wisdom);
        buffer.writeInt(charisma);
        buffer.writeInt(exceptionalStrength);

        buffer.writeInt(openLocks);
        buffer.writeInt(findTraps);
        buffer.writeInt(pickPockets);
        buffer.writeInt(moveSilently);
        buffer.writeInt(hideInShadows);
        buffer.writeInt(detectIllusion);
        buffer.writeInt(setTraps);

        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            buffer.writeInt(getWeaponRank(proficiency));
        }
        for (FightingStyle style : FightingStyle.values())
        {
            buffer.writeInt(getStyleRank(style));
        }

        for (MageSpell spell : MageSpell.values())
        {
            buffer.writeBoolean(knownMageSpells.contains(spell));
        }
        buffer.writeBoolean(memorizedMageSpell != null);
        if (memorizedMageSpell != null) buffer.writeEnum(memorizedMageSpell);

        for (DivineSpell spell : DivineSpell.values())
        {
            buffer.writeInt(memorizedDivineSpells.getOrDefault(spell, 0));
        }
    }

    public void readNetwork(FriendlyByteBuf buffer)
    {
        resetCharacterData();
        characterCreated = buffer.readBoolean();
        name = buffer.readUtf(32);
        if (!characterCreated) return;

        gender = buffer.readEnum(PendingCharacter.Gender.class);
        race = buffer.readEnum(CharacterRace.class);
        characterClass = buffer.readEnum(CharacterClass.class);
        alignment = buffer.readEnum(CharacterAlignment.class);
        level = Math.max(1, buffer.readInt());

        appearance.setSkinTone(buffer.readEnum(CharacterAppearance.SkinTone.class));
        appearance.setHairStyle(buffer.readEnum(CharacterAppearance.HairStyle.class));
        appearance.setHairColor(buffer.readEnum(CharacterAppearance.HairColor.class));
        appearance.setEyeColor(buffer.readEnum(CharacterAppearance.EyeColor.class));
        appearance.setFacialHair(buffer.readEnum(CharacterAppearance.FacialHair.class));
        appearance.setShirtColor(buffer.readEnum(CharacterAppearance.ClothingColor.class));
        appearance.setTrouserColor(buffer.readEnum(CharacterAppearance.ClothingColor.class));

        strength = buffer.readInt();
        dexterity = buffer.readInt();
        constitution = buffer.readInt();
        intelligence = buffer.readInt();
        wisdom = buffer.readInt();
        charisma = buffer.readInt();
        exceptionalStrength = buffer.readInt();

        openLocks = buffer.readInt();
        findTraps = buffer.readInt();
        pickPockets = buffer.readInt();
        moveSilently = buffer.readInt();
        hideInShadows = buffer.readInt();
        detectIllusion = buffer.readInt();
        setTraps = buffer.readInt();

        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            weaponRanks.put(proficiency, buffer.readInt());
        }
        for (FightingStyle style : FightingStyle.values())
        {
            styleRanks.put(style, buffer.readInt());
        }

        for (MageSpell spell : MageSpell.values())
        {
            if (buffer.readBoolean()) knownMageSpells.add(spell);
        }
        if (buffer.readBoolean()) memorizedMageSpell = buffer.readEnum(MageSpell.class);

        for (DivineSpell spell : DivineSpell.values())
        {
            int count = buffer.readInt();
            if (count > 0) memorizedDivineSpells.put(spell, count);
        }
    }

    public void saveNBTData(CompoundTag tag)
    {
        tag.putBoolean("characterCreated", characterCreated);
        tag.putString("name", name);
        putEnum(tag, "gender", gender);
        putEnum(tag, "race", race);
        putEnum(tag, "class", characterClass);
        putEnum(tag, "alignment", alignment);
        tag.putInt("level", level);

        CompoundTag appearanceTag = new CompoundTag();
        putEnum(appearanceTag, "skinTone", appearance.getSkinTone());
        putEnum(appearanceTag, "hairStyle", appearance.getHairStyle());
        putEnum(appearanceTag, "hairColor", appearance.getHairColor());
        putEnum(appearanceTag, "eyeColor", appearance.getEyeColor());
        putEnum(appearanceTag, "facialHair", appearance.getFacialHair());
        putEnum(appearanceTag, "shirtColor", appearance.getShirtColor());
        putEnum(appearanceTag, "trouserColor", appearance.getTrouserColor());
        tag.put("appearance", appearanceTag);

        CompoundTag abilityTag = new CompoundTag();
        abilityTag.putInt("strength", strength);
        abilityTag.putInt("dexterity", dexterity);
        abilityTag.putInt("constitution", constitution);
        abilityTag.putInt("intelligence", intelligence);
        abilityTag.putInt("wisdom", wisdom);
        abilityTag.putInt("charisma", charisma);
        abilityTag.putInt("exceptionalStrength", exceptionalStrength);
        tag.put("abilities", abilityTag);

        CompoundTag skillTag = new CompoundTag();
        skillTag.putInt("openLocks", openLocks);
        skillTag.putInt("findTraps", findTraps);
        skillTag.putInt("pickPockets", pickPockets);
        skillTag.putInt("moveSilently", moveSilently);
        skillTag.putInt("hideInShadows", hideInShadows);
        skillTag.putInt("detectIllusion", detectIllusion);
        skillTag.putInt("setTraps", setTraps);
        tag.put("skills", skillTag);

        CompoundTag weaponTag = new CompoundTag();
        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            weaponTag.putInt(proficiency.name(), getWeaponRank(proficiency));
        }
        tag.put("weaponProficiencies", weaponTag);

        CompoundTag styleTag = new CompoundTag();
        for (FightingStyle style : FightingStyle.values())
        {
            styleTag.putInt(style.name(), getStyleRank(style));
        }
        tag.put("fightingStyles", styleTag);

        CompoundTag spellTag = new CompoundTag();
        CompoundTag knownMageTag = new CompoundTag();
        for (MageSpell spell : MageSpell.values())
        {
            knownMageTag.putBoolean(spell.name(), knownMageSpells.contains(spell));
        }
        spellTag.put("knownMage", knownMageTag);
        putEnum(spellTag, "memorizedMage", memorizedMageSpell);

        CompoundTag divineTag = new CompoundTag();
        for (DivineSpell spell : DivineSpell.values())
        {
            divineTag.putInt(spell.name(), memorizedDivineSpells.getOrDefault(spell, 0));
        }
        spellTag.put("memorizedDivine", divineTag);
        tag.put("spells", spellTag);
    }

    public void loadNBTData(CompoundTag tag)
    {
        resetCharacterData();
        characterCreated = tag.getBoolean("characterCreated");
        name = tag.getString("name");
        gender = readEnum(tag, "gender", PendingCharacter.Gender.class, null);
        race = readEnum(tag, "race", CharacterRace.class, null);
        characterClass = readEnum(tag, "class", CharacterClass.class, null);
        alignment = readEnum(tag, "alignment", CharacterAlignment.class, null);
        level = tag.contains("level") ? Math.max(1, tag.getInt("level")) : 1;

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

        if (tag.contains("abilities"))
        {
            CompoundTag a = tag.getCompound("abilities");
            strength = a.getInt("strength");
            dexterity = a.getInt("dexterity");
            constitution = a.getInt("constitution");
            intelligence = a.getInt("intelligence");
            wisdom = a.getInt("wisdom");
            charisma = a.getInt("charisma");
            exceptionalStrength = a.getInt("exceptionalStrength");
        }

        if (tag.contains("skills"))
        {
            CompoundTag s = tag.getCompound("skills");
            openLocks = s.getInt("openLocks");
            findTraps = s.getInt("findTraps");
            pickPockets = s.getInt("pickPockets");
            moveSilently = s.getInt("moveSilently");
            hideInShadows = s.getInt("hideInShadows");
            detectIllusion = s.getInt("detectIllusion");
            setTraps = s.getInt("setTraps");
        }

        if (tag.contains("weaponProficiencies"))
        {
            CompoundTag p = tag.getCompound("weaponProficiencies");
            for (WeaponProficiency proficiency : WeaponProficiency.values())
            {
                weaponRanks.put(proficiency, p.getInt(proficiency.name()));
            }
        }

        if (tag.contains("fightingStyles"))
        {
            CompoundTag s = tag.getCompound("fightingStyles");
            for (FightingStyle style : FightingStyle.values())
            {
                styleRanks.put(style, s.getInt(style.name()));
            }
        }

        if (tag.contains("spells"))
        {
            CompoundTag s = tag.getCompound("spells");
            if (s.contains("knownMage"))
            {
                CompoundTag known = s.getCompound("knownMage");
                for (MageSpell spell : MageSpell.values())
                {
                    if (known.getBoolean(spell.name())) knownMageSpells.add(spell);
                }
            }
            memorizedMageSpell = readEnum(s, "memorizedMage", MageSpell.class, null);

            if (s.contains("memorizedDivine"))
            {
                CompoundTag divine = s.getCompound("memorizedDivine");
                for (DivineSpell spell : DivineSpell.values())
                {
                    int count = divine.getInt(spell.name());
                    if (count > 0) memorizedDivineSpells.put(spell, count);
                }
            }
        }
    }

    private void resetCharacterData()
    {
        characterCreated = false;
        name = "";
        gender = null;
        race = null;
        characterClass = null;
        alignment = null;
        level = 1;
        appearance.reset();

        strength = dexterity = constitution = intelligence = wisdom = charisma = 0;
        exceptionalStrength = 0;
        openLocks = findTraps = pickPockets = moveSilently = hideInShadows = detectIllusion = setTraps = 0;

        clearRanks();
        knownMageSpells.clear();
        memorizedMageSpell = null;
        memorizedDivineSpells.clear();
    }

    private void clearRanks()
    {
        weaponRanks.clear();
        styleRanks.clear();
        for (WeaponProficiency proficiency : WeaponProficiency.values()) weaponRanks.put(proficiency, 0);
        for (FightingStyle style : FightingStyle.values()) styleRanks.put(style, 0);
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
