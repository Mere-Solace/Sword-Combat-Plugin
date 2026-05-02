package btm.sword.entity.team;

import org.bukkit.entity.LivingEntity;

/**
 * Faction tags used to suppress friendly fire between allied entities.
 * <p>
 * Each constant corresponds to a Bukkit scoreboard tag applied to every {@link btm.sword.entity.base.SwordEntity}
 * via {@link btm.sword.entity.base.SwordEntity#joinTeam(SwordTeam)} on spawn.
 * The raw {@code hit()} path in {@code SwordEntity} skips damage when the attacker and defender
 * share the same tag.
 * </p>
 *
 * <ul>
 *   <li>{@link #RED}    — hostile mobs ({@link btm.sword.entity.mob.Hostile})</li>
 *   <li>{@link #BLUE}   — players ({@link btm.sword.entity.player.SwordPlayer})</li>
 *   <li>{@link #GREEN}  — passive entities ({@link btm.sword.entity.mob.Passive})</li>
 *   <li>{@link #YELLOW} — reserved for future use</li>
 * </ul>
 */
public enum SwordTeam {

    /** Hostile mobs — assigned in {@link btm.sword.entity.mob.Hostile#onSpawn()}. */
    RED("sword_team_red"),

    /** Players — assigned in {@link btm.sword.entity.player.SwordPlayer#onSpawn()}. */
    BLUE("sword_team_blue"),

    /** Passive entities — assigned in {@link btm.sword.entity.mob.Passive#onSpawn()}. */
    GREEN("sword_team_green"),

    /** Reserved for future faction use. */
    YELLOW("sword_team_yellow");

    private static final SwordTeam[] VALUES = values();

    private final String tag;

    SwordTeam(String tag) {
        this.tag = tag;
    }

    /** Returns the scoreboard tag string applied to Bukkit entities on this team. */
    public String tag() {
        return tag;
    }

    /**
     * Returns the {@link SwordTeam} for the given entity by inspecting its scoreboard tags,
     * or {@code null} if the entity carries no sword team tag.
     *
     * @param entity the entity to inspect
     * @return the matching team, or {@code null}
     */
    public static SwordTeam fromEntity(LivingEntity entity) {
        for (SwordTeam team : VALUES) {
            if (entity.getScoreboardTags().contains(team.tag)) {
                return team;
            }
        }
        return null;
    }
}
