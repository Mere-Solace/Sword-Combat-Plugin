package btm.sword.system.attack.dev;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.joml.Vector3f;

import btm.sword.system.attack.def.AttackDef;
import lombok.Getter;

/**
 * Tracks per-player state during an attack creation or editing session.
 *
 * <p>Sessions are stored in a static {@link ConcurrentHashMap} keyed by player UUID.
 * Use {@link #getOrCreate(Player)} to obtain or lazily create a session, and
 * {@link #remove(UUID)} to clean up on logout or session cancellation.</p>
 *
 * <p>A session transitions between {@link DevMode} states via the
 * {@link #startRecording(String)}, {@link #stopRecording()},
 * {@link #startEditing(String, AttackDef.Builder)}, and {@link #stopEditing()} methods.
 * While in any non-{@link DevMode#IDLE} state, {@link #isActive()} returns {@code true}.</p>
 */
@Getter
public final class AttackDevSession {

    private static final ConcurrentHashMap<UUID, AttackDevSession> SESSIONS = new ConcurrentHashMap<>();

    private final Player player;
    private DevMode mode = DevMode.IDLE;
    private String currentAttackName = null;
    private final List<Vector3f> recordingBuffer = new ArrayList<>();
    private AttackDef.Builder editBuilder = null;
    private int currentKeyframeIndex = 0;

    private AttackDevSession(Player player) {
        this.player = player;
    }

    // ── Static API ────────────────────────────────────────────────────────────

    /**
     * Returns the existing session for the given player, creating a new one if absent.
     *
     * @param player the player whose session to retrieve or create
     * @return the existing or newly created session
     */
    public static AttackDevSession getOrCreate(Player player) {
        return SESSIONS.computeIfAbsent(player.getUniqueId(), id -> new AttackDevSession(player));
    }

    /**
     * Removes and discards the session for the given player UUID, if one exists.
     * Should be called on logout or when the player explicitly cancels a session.
     *
     * @param playerId the UUID of the player whose session to remove
     */
    public static void remove(UUID playerId) {
        SESSIONS.remove(playerId);
    }

    // ── Instance API ──────────────────────────────────────────────────────────

    /**
     * Transitions to {@link DevMode#RECORDING} and clears the recording buffer.
     * Any previous recording data is discarded.
     *
     * @param name the name of the attack being recorded
     */
    public void startRecording(String name) {
        this.currentAttackName = name;
        this.recordingBuffer.clear();
        this.mode = DevMode.RECORDING;
    }

    /**
     * Transitions back to {@link DevMode#IDLE} and returns the captured buffer.
     * The returned list is a snapshot — the internal buffer is not cleared.
     *
     * @return the list of recorded local-space tip positions
     */
    public List<Vector3f> stopRecording() {
        this.mode = DevMode.IDLE;
        return List.copyOf(recordingBuffer);
    }

    /**
     * Transitions to {@link DevMode#EDITING} with the given attack name and partial builder.
     *
     * @param name    the name of the attack being edited
     * @param builder the in-progress {@link AttackDef.Builder} to continue editing
     */
    public void startEditing(String name, AttackDef.Builder builder) {
        this.currentAttackName = name;
        this.editBuilder = builder;
        this.currentKeyframeIndex = 0;
        this.mode = DevMode.EDITING;
    }

    /**
     * Transitions back to {@link DevMode#IDLE} and returns the current builder.
     *
     * @return the {@link AttackDef.Builder} as it stood when editing stopped
     */
    public AttackDef.Builder stopEditing() {
        this.mode = DevMode.IDLE;
        return editBuilder;
    }

    /**
     * Returns {@code true} if this session is in any state other than {@link DevMode#IDLE}.
     *
     * @return {@code true} when a recording or editing session is in progress
     */
    public boolean isActive() {
        return mode != DevMode.IDLE;
    }
}
