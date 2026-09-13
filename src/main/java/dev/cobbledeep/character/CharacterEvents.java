package dev.cobbledeep.character;

import net.minecraft.server.level.ServerPlayer;
import dev.cobbledeep.Cobbledeep;
import net.minecraft.resources.ResourceLocation;
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
            ResourceLocation.fromNamespaceAndPath(
                    Cobbledeep.MODID,
                    "character_data"
            );

    @SubscribeEvent
    public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getObject() instanceof Player)
        {
            CharacterDataProvider provider = new CharacterDataProvider();

            event.addCapability(
                    CHARACTER_DATA_ID,
                    provider
            );

            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer))
            return;

        serverPlayer
                .getCapability(CharacterCapabilities.CHARACTER_DATA)
                .ifPresent(data ->
                {
                    Cobbledeep.LOGGER.info(
                            "Character created: {}",
                            data.isCharacterCreated()
                    );
                });
    }
}