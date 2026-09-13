package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.screen.CharacterCreationScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = Cobbledeep.MODID,
        value = Dist.CLIENT
)
public class TitleScreenEvents
{
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event)
    {
        if (!(event.getScreen() instanceof TitleScreen))
            return;

        int centerX = event.getScreen().width / 2;
        int centerY = event.getScreen().height / 2;

        Button newGameButton = Button.builder(
                Component.literal("RPG Craft: New Game"),
                button ->
                {
                    Minecraft.getInstance().setScreen(
                            new CharacterCreationScreen()
                    );
                }
        )
        .bounds(
                centerX - 100,
                centerY + 44,
                200,
                20
        )
        .build();

        event.addListener(newGameButton);
    }
}