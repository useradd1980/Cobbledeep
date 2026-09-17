package dev.cobbledeep.registry;

import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.equipment.AdndInventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus
{
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, Cobbledeep.MODID);

    public static final RegistryObject<MenuType<AdndInventoryMenu>> ADND_INVENTORY =
            MENUS.register("adnd_inventory",
                    () -> IForgeMenuType.create(
                            (containerId, inventory, buffer) -> new AdndInventoryMenu(containerId, inventory)));

    private ModMenus() {}
}
