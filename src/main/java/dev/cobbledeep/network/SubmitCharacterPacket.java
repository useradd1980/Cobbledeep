package dev.cobbledeep.network;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import dev.cobbledeep.character.CharacterData;
import dev.cobbledeep.character.PendingCharacter;
import dev.cobbledeep.combat.DndPlayerMechanics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Transfers the complete generated character to the server after world join. */
public class SubmitCharacterPacket
{
    private final CharacterData submittedData = new CharacterData();

    public SubmitCharacterPacket(PendingCharacter pending)
    {
        submittedData.setFromPending(pending);
    }

    public SubmitCharacterPacket(FriendlyByteBuf buffer)
    {
        submittedData.readNetwork(buffer);
    }

    public void encode(FriendlyByteBuf buffer)
    {
        submittedData.writeNetwork(buffer);
    }

    public void handle(CustomPayloadEvent.Context context)
    {
        ServerPlayer sender = context.getSender();
        if (sender == null)
        {
            Cobbledeep.LOGGER.warn("Received character submission without a server-side player");
            return;
        }

        sender.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            data.copyFrom(submittedData);
            DndPlayerMechanics.applyCharacter(sender, data, true);
            Cobbledeep.LOGGER.info(
                    "Stored submitted Cobbledeep character: created={}, name={}, race={}, class={}, STR={}, DEX={}, proficiencies={}, mageSpells={}",
                    data.isCharacterCreated(),
                    data.getName(),
                    data.getRace(),
                    data.getCharacterClass(),
                    data.getStrength(),
                    data.getDexterity(),
                    countWeaponRanks(data),
                    data.getKnownMageSpells().size());
            RPGNetwork.sendCharacterData(sender, data);
        });
    }

    private static int countWeaponRanks(CharacterData data)
    {
        int total = 0;
        for (var proficiency : dev.cobbledeep.character.WeaponProficiency.values())
        {
            total += data.getWeaponRank(proficiency);
        }
        return total;
    }
}
