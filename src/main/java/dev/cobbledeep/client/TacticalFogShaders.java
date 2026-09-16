package dev.cobbledeep.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.io.IOException;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TacticalFogShaders
{
    static ShaderInstance shader;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException
    {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(Cobbledeep.MODID, "tactical_fog"),
                DefaultVertexFormat.POSITION_TEX), loaded -> shader = loaded);
    }
}
