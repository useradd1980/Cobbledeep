package dev.cobbledeep.network;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterAlignment;
import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterClass;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.PendingCharacter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Transfers the completed character identity and appearance from the title-screen
 * creation wizard to the server once the player actually joins the new world.
 */
public class SubmitCharacterPacket
{
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

    public SubmitCharacterPacket(PendingCharacter pending)
    {
        this(
                pending.getName().trim(),
                pending.getGender(),
                pending.getRace(),
                pending.getCharacterClass(),
                pending.getAlignment(),
                pending.getAppearance().getSkinTone(),
                pending.getAppearance().getHairStyle(),
                pending.getAppearance().getHairColor(),
                pending.getAppearance().getEyeColor(),
                pending.getAppearance().getFacialHair(),
                pending.getAppearance().getShirtColor(),
                pending.getAppearance().getTrouserColor());
    }

    public SubmitCharacterPacket(FriendlyByteBuf buffer)
    {
        this(
                buffer.readUtf(32),
                buffer.readEnum(PendingCharacter.Gender.class),
                buffer.readEnum(CharacterRace.class),
                buffer.readEnum(CharacterClass.class),
                buffer.readEnum(CharacterAlignment.class),
                buffer.readEnum(CharacterAppearance.SkinTone.class),
                buffer.readEnum(CharacterAppearance.HairStyle.class),
                buffer.readEnum(CharacterAppearance.HairColor.class),
                buffer.readEnum(CharacterAppearance.EyeColor.class),
                buffer.readEnum(CharacterAppearance.FacialHair.class),
                buffer.readEnum(CharacterAppearance.ClothingColor.class),
                buffer.readEnum(CharacterAppearance.ClothingColor.class));
    }

    private SubmitCharacterPacket(
            String name,
            PendingCharacter.Gender gender,
            CharacterRace race,
            CharacterClass characterClass,
            CharacterAlignment alignment,
            CharacterAppearance.SkinTone skinTone,
            CharacterAppearance.HairStyle hairStyle,
            CharacterAppearance.HairColor hairColor,
            CharacterAppearance.EyeColor eyeColor,
            CharacterAppearance.FacialHair facialHair,
            CharacterAppearance.ClothingColor shirtColor,
            CharacterAppearance.ClothingColor trouserColor)
    {
        this.name = name;
        this.gender = gender;
        this.race = race;
        this.characterClass = characterClass;
        this.alignment = alignment;
        this.skinTone = skinTone;
        this.hairStyle = hairStyle;
        this.hairColor = hairColor;
        this.eyeColor = eyeColor;
        this.facialHair = facialHair;
        this.shirtColor = shirtColor;
        this.trouserColor = trouserColor;
    }

    public void encode(FriendlyByteBuf buffer)
    {
        buffer.writeUtf(name, 32);
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
        ServerPlayer sender = context.getSender();
        if (sender == null)
        {
            Cobbledeep.LOGGER.warn("Received character submission without a server-side player");
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

        sender.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresentOrElse(data ->
        {
            data.setIdentity(name, gender, race, characterClass, alignment, appearance);
            Cobbledeep.LOGGER.info(
                    "Stored submitted Cobbledeep character: created={}, name={}, race={}, class={}",
                    data.isCharacterCreated(),
                    data.getName(),
                    data.getRace(),
                    data.getCharacterClass());
            RPGNetwork.sendCharacterData(sender, data);
        }, () -> Cobbledeep.LOGGER.error("Player is missing Cobbledeep character capability"));
    }
}
