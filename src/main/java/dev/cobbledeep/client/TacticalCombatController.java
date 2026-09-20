package dev.cobbledeep.client;

import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Maintains a tactical attack order until the target dies or the order is replaced. */
public final class TacticalCombatController
{
    private static final double ATTACK_RANGE_SQUARED = 3.0 * 3.0;
    private static final double RETARGET_DISTANCE_SQUARED = 1.0 * 1.0;
    private static final int RETARGET_INTERVAL_TICKS = 8;

    private static LivingEntity target;
    private static int weaponSlot;
    private static Vec3 lastPursuitTarget;
    private static int retargetCooldown;

    private TacticalCombatController() { }

    public static void startAttack(Minecraft minecraft, LivingEntity newTarget, int selectedWeaponSlot)
    {
        cancel(minecraft);
        if (minecraft.player == null || newTarget == null || !newTarget.isAlive()) return;
        target = newTarget;
        weaponSlot = Math.max(0, Math.min(3, selectedWeaponSlot));
        selectWeapon(minecraft);
    }

    public static void cancel(Minecraft minecraft)
    {
        target = null;
        lastPursuitTarget = null;
        retargetCooldown = 0;
        TacticalPathMovement.stop(minecraft);
    }

    static void tick(Minecraft minecraft)
    {
        if (target == null) return;
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null
                || target.level() != minecraft.level || !minecraft.player.isAlive()
                || !target.isAlive() || target.isRemoved())
        {
            cancel(minecraft);
            return;
        }
        if (minecraft.screen != null || !minecraft.isWindowActive())
        {
            TacticalPathMovement.suspendInputs(minecraft);
            return;
        }

        selectWeapon(minecraft);
        boolean inRange = minecraft.player.distanceToSqr(target) <= ATTACK_RANGE_SQUARED;
        boolean visible = minecraft.player.hasLineOfSight(target);
        if (!inRange || !visible)
        {
            pursue(minecraft);
            return;
        }

        TacticalPathMovement.stop(minecraft);
        faceTarget(minecraft);
        // Waiting for initiative or the next six-second round is not an attack.
        // The rat publishes this window so the client does not animate futile
        // swings every time Minecraft's much shorter weapon cooldown expires.
        // Server-side combat rules independently enforce the same action budget.
        if (target instanceof GiantRatEntity rat
                && !rat.playerAttackWindowOpen(minecraft.player)) return;

        if (minecraft.player.getAttackStrengthScale(0.0F) >= 0.99F)
        {
            minecraft.gameMode.attack(minecraft.player, target);
            minecraft.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    private static void pursue(Minecraft minecraft)
    {
        Vec3 destination = target.position();
        if (retargetCooldown > 0) retargetCooldown--;
        boolean needsRoute = TacticalPathMovement.target() == null;
        boolean targetMoved = lastPursuitTarget == null
                || lastPursuitTarget.distanceToSqr(destination) >= RETARGET_DISTANCE_SQUARED;
        if (!needsRoute && (!targetMoved || retargetCooldown > 0)) return;

        if (needsRoute)
            TacticalPathMovement.start(minecraft, destination);
        else
            TacticalPathMovement.retarget(minecraft, destination);
        lastPursuitTarget = destination;
        retargetCooldown = RETARGET_INTERVAL_TICKS;
    }

    private static void selectWeapon(Minecraft minecraft)
    {
        if (minecraft.player == null) return;
        minecraft.player.getInventory().selected = weaponSlot;
    }

    private static void faceTarget(Minecraft minecraft)
    {
        double dx = target.getX() - minecraft.player.getX();
        double dz = target.getZ() - minecraft.player.getZ();
        minecraft.player.setYRot((float)Math.toDegrees(Math.atan2(-dx, dz)));
    }
}
