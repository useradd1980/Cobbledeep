package dev.cobbledeep.network;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

public class RPGNetwork
{
    private static int packetId = 0;

    private static int nextId()
    {
        return packetId++;
    }

    public static final SimpleChannel CHANNEL =
            ChannelBuilder.named(
                    ResourceLocation.fromNamespaceAndPath(
                            Cobbledeep.MODID,
                            "main"
                    )
            )
            .clientAcceptedVersions((status, version) -> true)
            .serverAcceptedVersions((status, version) -> true)
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void register()
    {
        CHANNEL.messageBuilder(
                OpenCharacterCreationPacket.class,
                nextId(),
                NetworkDirection.PLAY_TO_CLIENT
        )
        .decoder(OpenCharacterCreationPacket::new)
        .encoder(OpenCharacterCreationPacket::encode)
        .consumerMainThread(OpenCharacterCreationPacket::handle)
        .add();
    }
}
