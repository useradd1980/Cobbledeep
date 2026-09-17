package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.screen.CharacterSheetScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class CharacterSheetController
{
    private CharacterSheetController() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent.Post event)
    {
        Minecraft minecraft = Minecraft.getInstance();
        while (TacticalCameraKeys.CHARACTER_SHEET.consumeClick())
        {
            if (minecraft.player != null && minecraft.screen == null)
            {
                minecraft.setScreen(new CharacterSheetScreen(false));
            }
        }
    }

}
