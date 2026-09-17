package dev.cobbledeep.relations;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-side reactions that enforce tactical relationships. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID)
public final class CharacterRelationEvents
{
    private CharacterRelationEvents() { }

    @SubscribeEvent
    public static void onTargetChanged(LivingChangeTargetEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        if (event.getNewTarget() instanceof Player
                && CharacterRelations.disposition(event.getEntity()) == CharacterDisposition.FRIENDLY)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent event)
    {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (CharacterRelations.disposition(event.getEntity()) != CharacterDisposition.FRIENDLY) return;

        CharacterRelations.setDisposition(event.getEntity(), CharacterDisposition.HOSTILE);
        if (event.getEntity() instanceof Mob mob) mob.setTarget(attacker);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob mob)) return;
        if (mob.getTarget() instanceof Player
                && CharacterRelations.disposition(mob) == CharacterDisposition.FRIENDLY)
            mob.setTarget(null);
    }
}
