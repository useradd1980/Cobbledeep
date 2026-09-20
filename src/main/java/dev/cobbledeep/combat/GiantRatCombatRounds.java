package dev.cobbledeep.combat;

import dev.cobbledeep.monster.GiantRatEntity;
import dev.cobbledeep.network.ConsoleMessagePacket;
import dev.cobbledeep.network.RPGNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** A six-second, real-time-with-pause initiative round for the giant rat and player. */
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

    public void begin(GiantRatEntity rat, Player player) {
        if (rat.level().isClientSide || rat.isCorpse()) return;
        if (opponent != null && opponent.equals(player.getUUID())) return;
        opponent = player.getUUID();
        roundStart = rat.level().getGameTime();
        rollRound(rat, player);
        if (player instanceof ServerPlayer serverPlayer) {
            RPGNetwork.CHANNEL.send(new ConsoleMessagePacket(
                    "Initiative: You d10 " + playerInitiative + ", Giant Rat d10 "
                            + ratInitiative + " — "
                            + (playerInitiative < ratInitiative ? "you act first" : "rat acts first"),
                    ConsoleMessagePacket.COMBAT, 0xFFFFAA00),
                    PacketDistributor.PLAYER.with(serverPlayer));
        }
    }

    public void clear(GiantRatEntity rat) {
        opponent = null;
        playerActed = false;
        ratActed = false;
        rat.setCombatPlayerWindow(0, 0);
    }

    public void tick(GiantRatEntity rat) {
        if (opponent == null || rat.isCorpse()) return;
        if (!(rat.getTarget() instanceof Player player) || !opponent.equals(player.getUUID())
                || !player.isAlive()) {
            clear(rat);
            return;
        }
        long now = rat.level().getGameTime();
        if (now >= roundStart + ROUND_TICKS) {
            roundStart += ((now - roundStart) / ROUND_TICKS) * ROUND_TICKS;
            rollRound(rat, player);
        }
    }

    public boolean tryPlayerAttack(GiantRatEntity rat, ServerPlayer player) {
        if (rat.level().isClientSide || rat.isCorpse() || !player.isAlive()) return false;
        if (opponent == null || !opponent.equals(player.getUUID())) {
            rat.setTarget(player);
            begin(rat, player);
        }
        tick(rat);
        if (opponent == null || !opponent.equals(player.getUUID()) || playerActed
                || rat.level().getGameTime() < roundStart + playerOffset) return false;
        playerActed = true;
        rat.setCombatPlayerWindow(player.getId(), Integer.MAX_VALUE);
        return true;
    }

    public boolean tryRatAttack(GiantRatEntity rat, Entity target) {
        if (!(target instanceof Player player)) return true;
        if (rat.level().isClientSide || rat.isCorpse() || !player.isAlive()) return false;
        if (opponent == null || !opponent.equals(player.getUUID())) begin(rat, player);
        tick(rat);
        if (opponent == null || !opponent.equals(player.getUUID()) || ratActed
                || rat.level().getGameTime() < roundStart + ratOffset) return false;
        ratActed = true;
        return true;
    }

    private void rollRound(GiantRatEntity rat, Player player) {
        playerInitiative = ThreadLocalRandom.current().nextInt(1, 11);
        ratInitiative = ThreadLocalRandom.current().nextInt(1, 11);
        while (playerInitiative == ratInitiative) {
            playerInitiative = ThreadLocalRandom.current().nextInt(1, 11);
            ratInitiative = ThreadLocalRandom.current().nextInt(1, 11);
        }
        playerOffset = playerInitiative < ratInitiative ? FIRST_ATTACK_TICK : SECOND_ATTACK_TICK;
        ratOffset = ratInitiative < playerInitiative ? FIRST_ATTACK_TICK : SECOND_ATTACK_TICK;
        playerActed = false;
        ratActed = false;
        rat.setCombatPlayerWindow(player.getId(), (int) (roundStart + playerOffset));
    }
}
