package dev.cobbledeep.relations;

import dev.cobbledeep.monster.GiantRatEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Central relationship registry for tactical characters.
 *
 * <p>Entity-type defaults provide the current test classifications. Explicit
 * changes use reserved scoreboard teams, which are saved with the world and
 * synchronized to clients automatically. Scripts can call
 * {@link #setDisposition(LivingEntity, CharacterDisposition)}.</p>
 */
public final class CharacterRelations
{
    private static final String TEAM_PREFIX = "cdeep_";
    private static final String FRIENDLY_TEAM = TEAM_PREFIX + "friend";
    private static final String HOSTILE_TEAM = TEAM_PREFIX + "enemy";
    private static final String PLAYABLE_TEAM = TEAM_PREFIX + "playable";
    private static final String NEUTRAL_TEAM = TEAM_PREFIX + "neutral";

    private CharacterRelations() { }

    public static CharacterDisposition disposition(LivingEntity entity)
    {
        Team team = entity.getTeam();
        if (team != null)
        {
            CharacterDisposition override = fromTeam(team.getName());
            if (override != null) return override;
        }

        if (entity instanceof Player) return CharacterDisposition.PLAYER;
        // Giant Rats use the existing hostile relationship and red NPC ring.
        // Dead rats are filtered by the ring renderer, so their persistent
        // corpses remain lootable without displaying a hostile ring.
        if (entity instanceof GiantRatEntity) return CharacterDisposition.HOSTILE;
        if (entity instanceof Creeper) return CharacterDisposition.HOSTILE;
        if (entity instanceof Skeleton) return CharacterDisposition.FRIENDLY;
        return CharacterDisposition.NEUTRAL;
    }

    public static void setDisposition(LivingEntity entity, CharacterDisposition disposition)
    {
        if (entity.level().isClientSide) return;
        Scoreboard scoreboard = entity.level().getScoreboard();
        String member = entity.getScoreboardName();
        scoreboard.removePlayerFromTeam(member);

        String teamName = teamName(disposition);
        if (teamName == null) return;
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) team = scoreboard.addPlayerTeam(teamName);
        scoreboard.addPlayerToTeam(member, team);
    }

    public static boolean hasNpcRing(CharacterDisposition disposition)
    {
        return disposition == CharacterDisposition.FRIENDLY
                || disposition == CharacterDisposition.HOSTILE
                || disposition == CharacterDisposition.PLAYABLE;
    }

    private static CharacterDisposition fromTeam(String name)
    {
        return switch (name)
        {
            case FRIENDLY_TEAM -> CharacterDisposition.FRIENDLY;
            case HOSTILE_TEAM -> CharacterDisposition.HOSTILE;
            case PLAYABLE_TEAM -> CharacterDisposition.PLAYABLE;
            case NEUTRAL_TEAM -> CharacterDisposition.NEUTRAL;
            default -> null;
        };
    }

    private static String teamName(CharacterDisposition disposition)
    {
        return switch (disposition)
        {
            case FRIENDLY -> FRIENDLY_TEAM;
            case HOSTILE -> HOSTILE_TEAM;
            case PLAYABLE -> PLAYABLE_TEAM;
            case NEUTRAL -> NEUTRAL_TEAM;
            case PLAYER -> null;
        };
    }
}
