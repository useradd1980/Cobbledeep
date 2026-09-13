package dev.cobbledeep;

import com.mojang.logging.LogUtils;

import dev.cobbledeep.network.RPGNetwork;
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
        RPGNetwork.register();

        LOGGER.info("Cobbledeep loaded.");
    }
}