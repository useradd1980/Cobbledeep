package dev.cobbledeep.monster;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GiantRatRegistration {
    private static final DeferredRegister<EntityType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Cobbledeep.MODID);

    public static final RegistryObject<EntityType<GiantRatEntity>> GIANT_RAT = TYPES.register(
            "giant_rat", () -> EntityType.Builder.of(GiantRatEntity::new, MobCategory.MONSTER)
                    .sized(0.45f, 0.53f)
                    .clientTrackingRange(8)
                    .build("cobbledeep:giant_rat"));

    private GiantRatRegistration() {}

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(GiantRatRegistration::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GIANT_RAT.get(), GiantRatEntity.createAttributes().build());
    }
}
