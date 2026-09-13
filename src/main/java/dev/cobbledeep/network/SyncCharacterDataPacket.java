package dev.cobbledeep.network;

import dev.cobbledeep.character.CharacterAlignment;
import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterClass;
import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.PendingCharacter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Synchronizes persisted Cobbledeep character identity/appearance to the client. */
public class SyncCharacterDataPacket
{
    private final boolean created;
    private final String name;
    private final PendingCharacter.Gender gender;
    private final CharacterRace race;
    private final CharacterClass characterClass;
    private final CharacterAlignment alignment;
    private final CharacterAppearance.SkinTone skinTone;
    private final CharacterAppearance.HairStyle hairStyle;
    private final CharacterAppearance.HairColor hairColor;
    private final CharacterAppearance.EyeColor eyeColor;
    private final CharacterAppearance.FacialHair facialHair;
    private final CharacterAppearance.ClothingColor shirtColor;
    private final CharacterAppearance.ClothingColor trouserColor;

    public SyncCharacterDataPacket(CharacterData data)
    {
        CharacterAppearance a = data.getAppearance();
        this.created = data.isCharacterCreated();
        this.name = data.getName();
        this.gender = data.getGender();
        this.race = data.getRace();
        this.characterClass = data.getCharacterClass();
        this.alignment = data.getAlignment();
        this.skinTone = a.getSkinTone();
        this.hairStyle = a.getHairStyle();
        this.hairColor = a.getHairColor();
        this.eyeColor = a.getEyeColor();
        this.facialHair = a.getFacialHair();
        this.shirtColor = a.getShirtColor();
        this.trouserColor = a.getTrouserColor();
    }

    public SyncCharacterDataPacket(FriendlyByteBuf buffer)
    {
        this.created = buffer.readBoolean();
        this.name = buffer.readUtf(32);
        if (created)
        {
            this.gender = buffer.readEnum(PendingCharacter.Gender.class);
            this.race = buffer.readEnum(CharacterRace.class);
            this.characterClass = buffer.readEnum(CharacterClass.class);
            this.alignment = buffer.readEnum(CharacterAlignment.class);
            this.skinTone = buffer.readEnum(CharacterAppearance.SkinTone.class);
            this.hairStyle = buffer.readEnum(CharacterAppearance.HairStyle.class);
            this.hairColor = buffer.readEnum(CharacterAppearance.HairColor.class);
            this.eyeColor = buffer.readEnum(CharacterAppearance.EyeColor.class);
            this.facialHair = buffer.readEnum(CharacterAppearance.FacialHair.class);
            this.shirtColor = buffer.readEnum(CharacterAppearance.ClothingColor.class);
            this.trouserColor = buffer.readEnum(CharacterAppearance.ClothingColor.class);
        }
        else
        {
            CharacterAppearance defaults = new CharacterAppearance();
            this.gender = null;
            this.race = null;
            this.characterClass = null;
            this.alignment = null;
            this.skinTone = defaults.getSkinTone();
            this.hairStyle = defaults.getHairStyle();
            this.hairColor = defaults.getHairColor();
            this.eyeColor = defaults.getEyeColor();
            this.facialHair = defaults.getFacialHair();
            this.shirtColor = defaults.getShirtColor();
            this.trouserColor = defaults.getTrouserColor();
        }
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeBoolean(created);
        buffer.writeUtf(name == null ? "" : name, 32);
        if (!created) return;
        buffer.writeEnum(gender);
        buffer.writeEnum(race);
        buffer.writeEnum(characterClass);
        buffer.writeEnum(alignment);
        buffer.writeEnum(skinTone);
        buffer.writeEnum(hairStyle);
        buffer.writeEnum(hairColor);
        buffer.writeEnum(eyeColor);
        buffer.writeEnum(facialHair);
        buffer.writeEnum(shirtColor);
        buffer.writeEnum(trouserColor);
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        minecraft.player.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            if (!created)
            {
                data.setCharacterCreated(false);
                return;
            }

            CharacterAppearance appearance = new CharacterAppearance();
            appearance.setSkinTone(skinTone);
            appearance.setHairStyle(hairStyle);
            appearance.setHairColor(hairColor);
            appearance.setEyeColor(eyeColor);
            appearance.setFacialHair(facialHair);
            appearance.setShirtColor(shirtColor);
            appearance.setTrouserColor(trouserColor);
            data.setIdentity(name, gender, race, characterClass, alignment, appearance);
        });
    }
}
