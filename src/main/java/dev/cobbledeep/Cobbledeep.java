package dev.cobbledeep;

import com.mojang.logging.LogUtils;

import dev.cobbledeep.monster.GiantRatRegistration;
import dev.cobbledeep.network.RPGNetwork;
import dev.cobbledeep.registry.ModMenus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(Cobbledeep.MODID)
public class Cobbledeep
{
    public static final String MODID = "cobbledeep";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Cobbledeep(FMLJavaModLoadingContext context)
    {
        ModMenus.MENUS.register(context.getModEventBus());
        GiantRatRegistration.register(context.getModEventBus());
        RPGNetwork.register();

        LOGGER.info("Cobbledeep loaded.");
    }
}
