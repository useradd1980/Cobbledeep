package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.screen.CharacterSheetScreen;
import dev.cobbledeep.network.OpenAdndInventoryPacket;
import dev.cobbledeep.network.RPGNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

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
                minecraft.setScreen(new CharacterSheetScreen());
            }
        }
    }

    @SubscribeEvent
    public static void replaceVanillaInventory(ScreenEvent.Opening event)
    {
        if (!(event.getNewScreen() instanceof InventoryScreen)) return;
        event.setNewScreen(null);
        RPGNetwork.CHANNEL.send(new OpenAdndInventoryPacket(), PacketDistributor.SERVER.noArg());
    }

}
