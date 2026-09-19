package dev.cobbledeep.client;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Suppresses Minecraft's vanilla survival tutorial while a Cobbledeep world is
 * active. The previous setting is restored on disconnect so Cobbledeep does not
 * permanently change the player's preference for other installations.
 */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class VanillaTutorialController
{
    private static TutorialSteps previousTutorialStep;

    private VanillaTutorialController()
    {
    }

    @SubscribeEvent
    public static void onPlayerLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (previousTutorialStep == null)
        {
            previousTutorialStep = minecraft.options.tutorialStep;
        }

        minecraft.getTutorial().stop();
        minecraft.options.tutorialStep = TutorialSteps.NONE;
    }

    @SubscribeEvent
    public static void onPlayerLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        if (previousTutorialStep == null)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getTutorial().stop();
        minecraft.options.tutorialStep = previousTutorialStep;
        previousTutorialStep = null;
    }
}
