package dev.cobbledeep.combat;

import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * One real-time-with-pause encounter between a giant rat and its current player target.
 * Both combatants can move throughout each six-second round, but can each make only
 * one melee attack. A d10 initiative roll determines their earliest attack windows.
 * All decisions and spent actions live on the server, not in the client attack loop.
 */
public final class GiantRatCombatRounds {
    public static final int ROUND_TICKS = 120;
    private static final int FIRST_ATTACK_TICK = 16;
    private static final int SECOND_ATTACK_TICK = 52;

    private UUID opponent;
    private long roundStart;
    private int playerInitiative;
    private int ratInitiative;
    private int playerOffset;
    private int ratOffset;
    private boolean playerActed;
    private boolean ratActed;

    /** Start a new encounter only when the opponent actually changes. */
    public void begin(GiantRatEntity rat, Player player) {
        if (rat.level().isClientSide || rat.isCorpse()) return;
        if (opponent != null && opponent.equals(player.getUUID())) return;
        opponent = player.getUUID();
        roundStart = rat.level().getGameTime();
        rollRound(rat, player);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(Component.literal(
                    "Initiative: You d10 " + playerInitiative + ", Giant Rat d10 "
                            + ratInitiative + " — "
                            + (playerInitiative < ratInitiative ? "you act first" : "rat acts first"))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    /** Retiring a target also retires any unspent attacks from that encounter. */
    public void clear(GiantRatEntity rat) {
        opponent = null;
        playerActed = false;
        ratActed = false;
        rat.setCombatPlayerWindow(0, 0);
    }

    /** Called only from the rat's server tick; the tactical pause freezes this clock. */
    public void tick(GiantRatEntity rat) {
        if (opponent == null || rat.isCorpse()) return;
        if (!(rat.getTarget() instanceof Player player) || !opponent.equals(player.getUUID())
                || !player.isAlive()) {
            clear(rat);
            return;
        }
        long now = rat.level().getGameTime();
        if (now >= roundStart + ROUND_TICKS) {
            // New round, new initiative. Movement and orders never consume an action.
            roundStart += ((now - roundStart) / ROUND_TICKS) * ROUND_TICKS;
            rollRound(rat, player);
        }
    }

    /** A rejected/early swing is not an action; a miss after initiative is. */
    public boolean tryPlayerAttack(GiantRatEntity rat, ServerPlayer player) {
        if (rat.level().isClientSide || rat.isCorpse() || !player.isAlive()) return false;
        if (opponent == null || !opponent.equals(player.getUUID())) {
            rat.setTarget(player); // Also starts the encounter and its detection animation.
            begin(rat, player);
        }
        tick(rat);
        if (opponent == null || !opponent.equals(player.getUUID()) || playerActed
                || rat.level().getGameTime() < roundStart + playerOffset) return false;
        playerActed = true;
        rat.setCombatPlayerWindow(player.getId(), Integer.MAX_VALUE);
        return true;
    }

    /** Rat melee uses the same round and consumes its one opportunity on contact. */
    public boolean tryRatAttack(GiantRatEntity rat, Entity target) {
        if (!(target instanceof Player player)) return true; // Non-player targets use their existing rules.
        if (rat.level().isClientSide || rat.isCorpse() || !player.isAlive()) return false;
        if (opponent == null || !opponent.equals(player.getUUID())) begin(rat, player);
        tick(rat);
        if (opponent == null || !opponent.equals(player.getUUID()) || ratActed
                || rat.level().getGameTime() < roundStart + ratOffset) return false;
        ratActed = true;
        return true;
    }

    private void rollRound(GiantRatEntity rat, Player player) {
        playerInitiative = rat.getRandom().nextInt(10) + 1;
        ratInitiative = rat.getRandom().nextInt(10) + 1;
        // Ties are resolved with another opposed roll so the first window is unambiguous.
        while (playerInitiative == ratInitiative) {
            playerInitiative = rat.getRandom().nextInt(10) + 1;
            ratInitiative = rat.getRandom().nextInt(10) + 1;
        }
        playerOffset = playerInitiative < ratInitiative ? FIRST_ATTACK_TICK : SECOND_ATTACK_TICK;
        ratOffset = ratInitiative < playerInitiative ? FIRST_ATTACK_TICK : SECOND_ATTACK_TICK;
        playerActed = false;
        ratActed = false;
        // The client uses this replicated window to avoid rapid, futile swing animations;
        // the server still validates every actual hit independently.
        rat.setCombatPlayerWindow(player.getId(), (int) (roundStart + playerOffset));
    }
}
