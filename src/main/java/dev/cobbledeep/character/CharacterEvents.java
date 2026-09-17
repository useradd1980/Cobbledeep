package dev.cobbledeep.character;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.network.RPGNetwork;
import dev.cobbledeep.combat.DndPlayerMechanics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public class CharacterEvents
{
    private static final ResourceLocation CHARACTER_DATA_ID =
            ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "character_data");

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            CharacterDataProvider provider = new CharacterDataProvider();
            event.addCapability(CHARACTER_DATA_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event)
    {
        event.getOriginal().reviveCaps();
        event.getOriginal().getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(oldData ->
                event.getEntity().getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(newData ->
                        newData.copyFrom(oldData)));
        event.getOriginal().invalidateCaps();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        serverPlayer.getCapability(CharacterCapabilities.CHARACTER_DATA).ifPresent(data ->
        {
            DndPlayerMechanics.applyCharacter(serverPlayer, data, false);
            Cobbledeep.LOGGER.info(
                    "Cobbledeep character loaded: created={}, name={}, race={}, class={}, STR={}, DEX={}, weaponRanks={}, mageSpells={}, openLocks={}",
                    data.isCharacterCreated(),
                    data.getName(),
                    data.getRace(),
                    data.getCharacterClass(),
                    data.getStrength(),
                    data.getDexterity(),
                    countWeaponRanks(data),
                    data.getKnownMageSpells().size(),
                    data.getOpenLocks());
            RPGNetwork.sendCharacterData(serverPlayer, data);
        });
    }

    private static int countWeaponRanks(CharacterData data)
    {
        int total = 0;
        for (WeaponProficiency proficiency : WeaponProficiency.values())
        {
            total += data.getWeaponRank(proficiency);
        }
        return total;
    }
}
