package btm.sword.system.attack.dev;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.joml.Vector3f;

import btm.sword.config.Config;
import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.attack.def.AttackDef;
import btm.sword.system.attack.def.VolumeType;
import btm.sword.system.attack.simulation.ControlMode;
import btm.sword.system.attack.simulation.ControlPoint;
import btm.sword.system.attack.simulation.ControlPointTrajectory;
import btm.sword.system.attack.simulation.KeyframeEffect;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Tracks per-player state during an attack creation or editing session.
 *
 * <p>Sessions are stored in a static {@link ConcurrentHashMap} keyed by player UUID.
 * Use {@link #getOrCreate(Player)} to obtain or lazily create a session,
 * {@link #get(UUID)} for a non-creating lookup, and {@link #remove(UUID)} to clean
 * up on logout or session cancellation.</p>
 *
 * <p>A session transitions between {@link DevMode} states via the
 * {@link #startRecording(String)}, {@link #stopRecording()},
 * {@link #startEditing(String, List, int, HitValuePacket)}, and {@link #stopEditing()}
 * methods. While in any non-{@link DevMode#IDLE} state, {@link #isActive()} returns
 * {@code true}.</p>
 *
 * <p>During RECORDING, right-click places world-space tip points into {@link #pendingPoints}.
 * DROP+SWAP cycles the active {@link PlacementMode}. On stop, the pending points are
 * converted to keyframes by {@code SweepRecordingAction.saveDraft}.</p>
 *
 * <p>During EDITING, the mutable {@link #editKeyframes} list and {@link #editDurationMs}
 * field are the primary working state. Call {@link #buildCurrentAttack()} to materialise
 * the current state into an {@link AttackDef}, and {@link #loadIntoWand()} to cache that
 * definition so the test wand fires it on left-click.</p>
 */
@Getter
public final class AttackDevSession {

    private static final ConcurrentHashMap<UUID, AttackDevSession> SESSIONS = new ConcurrentHashMap<>();

    private final Player player;
    private DevMode mode = DevMode.IDLE;
    private String currentAttackName = null;

    // ── AnimationMode state ───────────────────────────────────────────────────
    /**
     * Snapshot of the player's inventory taken on {@link AnimationMode#enter}.
     * Restored on {@link AnimationMode#exit} to return to the creative-dev state.
     */
    @Setter private ItemStack[] savedAnimationInventory = null;

    /**
     * Number of hotbar-tool mutations made since the last save in AnimationMode.
     * Displayed in the {@link SaveConfirmDialog} title. Reset to 0 after each save.
     */
    @Setter private int editCount = 0;

    /**
     * Whether the animated keyframe preview is currently playing in AnimationMode.
     * Toggled by {@link AnimationModeInputHandler} slot 7. The preview loop task
     * self-cancels when this becomes {@code false}.
     */
    @Setter private boolean playingPreview = false;

    // ── Recording state ───────────────────────────────────────────────────────
    /** Current placement mode — determines how right-click deposits points. */
    private PlacementMode placementMode = PlacementMode.SINGLE_KEYFRAME;

    /**
     * Points deposited by right-click during the current recording, each tagged with the
     * {@link PlacementMode} active at placement time. Mode switching no longer clears this
     * list, allowing mixed-mode trajectories to accumulate freely.
     */
    private final List<PlacedPoint> pendingPoints = new ArrayList<>();

    /**
     * Half-extents applied to each placed keyframe when the recording is saved.
     * Defaults to {@code 0.4 × 0.4 × 0.4} blocks.
     * -- SETTER --
     *  Replaces the half-extents used for newly placed keyframe volumes.
     */
    @Setter
    private Vector3f currentPlacementSize = new Vector3f(0.4f, 0.4f, 0.4f);

    /**
     * Distance from the player's eye at which a non-raycast point is placed, in blocks.
     * Adjusted via the Recording Settings menu.
     */
    @Getter @Setter private float tipDistance = 1.5f;

    /**
     * Maximum raycast distance for {@link PlacementMode#RAYCAST} point placement, in blocks.
     * Adjusted via the Recording Settings menu.
     */
    @Getter @Setter private float raycastMaxDistance = 8.0f;

    /**
     * Offset along the look direction applied to the raycast origin before firing, in blocks.
     * {@code 0} starts from the eye. Positive values push the origin forward; negative values
     * pull it behind the eye (useful for modeling attacks that originate behind the player).
     * Adjusted via the Recording Settings menu.
     */
    @Getter @Setter private float raycastOriginOffset = 0.0f;

    /**
     * Vertical offset applied to the eye position when computing an {@link PlacementMode#ORIGIN_RAY}
     * tip. Positive shifts the ray start upward; negative shifts it downward.
     * Adjusted via the Recording Settings menu.
     */
    @Getter @Setter private float originRayHeightOffset = 0.0f;

    /** Boss bar shown to the player while in {@link DevMode#RECORDING}. Hidden on stop. */
    @Getter(AccessLevel.NONE)
    private BossBar recordingBossBar = null;

    /**
     * Player {@link Location} (feet) captured at the start of recording.
     * Used to lock the player in place via {@code PlayerMoveEvent} and as the
     * world-space origin for the north-arrow visualization.
     */
    private Location lockedOrigin = null;
    /**
     * Bounding-box centre of the player at the moment {@link #startRecording} was called.
     * Used as the local-space origin when converting pending points to keyframes.
     */
    private Vector3f recordingRefOrigin = null;
    /**
     * Player yaw (degrees) at the moment {@link #startRecording} was called.
     * Used as the reference yaw for the world→local conversion in {@code saveDraft}.
     */
    private float recordingRefYaw = 0f;

    // ── Editing state ─────────────────────────────────────────────────────────
    /** Mutable list of OBB keyframes being edited. Directly mutated by the editor menu. */
    private List<VolumeKeyframe> editKeyframes = new ArrayList<>();
    /** Attack duration in milliseconds for the current edit session.
     * -- SETTER --
     *  Updates the attack duration for the current edit session.
     *
     */
    @Setter
    private int editDurationMs = 600;
    /** Hit-value packet for the current edit session. Carried over from the loaded definition. */
    private HitValuePacket editHitValue = null;
    /** Index of the currently selected keyframe in {@link #editKeyframes}.
     * -- SETTER --
     *  Sets the currently selected keyframe index. Clamped to valid range by callers.
     *
     */
    @Setter
    private int currentKeyframeIndex = 0;
    /**
     * Secondary selection set used for multi-keyframe operations.
     * When non-empty, nudge and adjustment operations apply to every index in this
     * set as well as to {@link #currentKeyframeIndex}. Populated via shift-click
     * range select or the "Select Before / After" buttons. Cleared on single-click
     * or when starting a new editing session.
     */
    @Getter
    @Setter
    private LinkedHashSet<Integer> selectedKeyframeIndices = new LinkedHashSet<>();
    /**
     * The most recently built {@link AttackDef} loaded into the test wand.
     * {@code null} means the wand fires the default {@code test_volume_attack}.
     */
    private AttackDef loadedAttackDef = null;

    /**
     * Control points for the current CTRL_POINT edit session, or {@code null} when
     * the session is keyframe-based. Set by {@link #startEditingCtrlPoint}.
     */
    @Getter @Setter private List<ControlPoint> editControlPoints = null;

    /**
     * Interpolation mode for the current CTRL_POINT edit session, or {@code null}
     * when the session is keyframe-based.
     */
    @Getter @Setter private ControlMode editControlMode = null;

    // ── Orientation flags for the current edit session ────────────────────────
    /**
     * Whether OBBs are tilted by the player's pitch angle. Defaults to the global config
     * value; overridable per-attack via the editor toggle.
     */
    @Getter @Setter private boolean editOrientWithPitch = Config.Combat.ATTACKS_ORIENT_WITH_PITCH;

    /**
     * Whether the attack origin is locked at fire time. Defaults to the global config value;
     * overridable per-attack via the editor toggle.
     */
    @Getter @Setter private boolean editLockOriginOnFire = Config.Combat.ATTACKS_LOCK_ORIGIN_ON_FIRE;

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
     * Returns the existing session for the given UUID, or {@code null} if none exists.
     *
     * @param playerId the UUID of the player to look up
     * @return the session, or {@code null}
     */
    public static AttackDevSession get(UUID playerId) {
        return SESSIONS.get(playerId);
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

    /**
     * Returns a snapshot of all sessions currently in {@link DevMode#EDITING} mode.
     * Used by {@link VolumeEditorMode} to render keyframe visualizations each tick.
     *
     * @return immutable list of active editing sessions
     */
    public static List<AttackDevSession> getEditingSessions() {
        return SESSIONS.values().stream()
            .filter(s -> s.mode == DevMode.EDITING)
            .toList();
    }

    /** Returns all active sessions. */
    public static Collection<AttackDevSession> allSessions() {
        return SESSIONS.values();
    }

    // ── Instance API — Recording ──────────────────────────────────────────────

    /**
     * Transitions to {@link DevMode#RECORDING} and resets all recording state.
     * Any previous pending points are discarded. A red boss bar is shown to the player
     * indicating the current placement mode and point count.
     *
     * @param name the name of the attack being recorded
     */
    public void startRecording(String name) {
        this.currentAttackName = name;
        this.pendingPoints.clear();
        this.mode = DevMode.RECORDING;

        // Capture position lock and reference frame at recording start
        this.lockedOrigin = player.getLocation().clone().setDirection(Config.Direction.north());
        BoundingBox bb = player.getBoundingBox();
        this.recordingRefOrigin = new Vector3f(
            (float) bb.getCenterX(), (float) bb.getCenterY(), (float) bb.getCenterZ());
        this.recordingRefYaw = player.getLocation().getYaw();

        this.recordingBossBar = BossBar.bossBar(
            recordingBossBarTitle(), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        player.showBossBar(recordingBossBar);
    }

    /**
     * Transitions back to {@link DevMode#IDLE} and hides the recording boss bar.
     * The pending points list remains accessible via {@link #getPendingPoints()} until
     * the next {@link #startRecording} call.
     */
    public void stopRecording() {
        this.mode = DevMode.IDLE;
        if (recordingBossBar != null) {
            player.hideBossBar(recordingBossBar);
            recordingBossBar = null;
        }
    }

    /**
     * Advances to the next {@link PlacementMode} and refreshes the recording boss bar.
     * Previously placed points are preserved — mode switching no longer resets the session,
     * allowing mixed-mode trajectories to accumulate across multiple mode changes.
     */
    public void cyclePlacementMode() {
        this.placementMode = placementMode.next();
        updateRecordingBossBar();
    }

    /**
     * Appends a world-space point to the pending points buffer, tagged with the current
     * placement mode, and updates the boss bar.
     *
     * @param point the world-space position to record
     */
    public void addPendingPoint(Vector3f point) {
        pendingPoints.add(new PlacedPoint(point, placementMode));
        updateRecordingBossBar();
    }

    /** Clears all pending points and refreshes the boss bar count. */
    public void clearPendingPoints() {
        pendingPoints.clear();
        updateRecordingBossBar();
    }

    /** Updates the recording boss bar title to reflect the current mode and point count. */
    public void updateRecordingBossBar() {
        if (recordingBossBar != null) {
            recordingBossBar.name(recordingBossBarTitle());
        }
    }

    private Component recordingBossBarTitle() {
        int placed = pendingPoints.size();
        String ptsLabel = placementMode == PlacementMode.SINGLE_KEYFRAME
            ? "(" + placed + " pts)"
            : "(" + placed + "/" + placementMode.requiredPoints() + " pts)";
        return Component.text("[Dev] Recording — ", NamedTextColor.RED)
            .append(Component.text(placementMode.label(), NamedTextColor.WHITE))
            .append(Component.text("  " + ptsLabel, NamedTextColor.GRAY));
    }

    // ── Instance API — Editing ────────────────────────────────────────────────

    /**
     * Transitions to {@link DevMode#EDITING} with the given attack parameters.
     * Replaces any previous editing state.
     *
     * @param name        the attack id being edited
     * @param keyframes   the initial keyframe list (copied into a mutable list)
     * @param durationMs  the attack duration in milliseconds
     * @param hitValue    the hit-value packet to preserve when saving
     */
    public void startEditing(String name, List<VolumeKeyframe> keyframes, int durationMs, HitValuePacket hitValue) {
        this.currentAttackName = name;
        this.editKeyframes = new ArrayList<>(keyframes);
        this.editControlPoints = null;
        this.editControlMode = null;
        this.editDurationMs = durationMs;
        this.editHitValue = hitValue;
        this.currentKeyframeIndex = 0;
        this.selectedKeyframeIndices.clear();
        this.mode = DevMode.EDITING;
        VolumeEditorMode.startForSession(this);
    }

    /**
     * Transitions to {@link DevMode#EDITING} with control-point trajectory data.
     * Clears any previous keyframe editing state.
     *
     * @param name       the attack id being edited
     * @param points     the control points (2 for LINEAR, 4 for BEZIER)
     * @param mode       the interpolation mode
     * @param durationMs the attack duration in milliseconds
     * @param hitValue   the hit-value packet to preserve when saving
     */
    public void startEditingCtrlPoint(String name, List<ControlPoint> points, ControlMode mode,
                                      int durationMs, HitValuePacket hitValue) {
        this.currentAttackName = name;
        this.editKeyframes = new ArrayList<>();
        this.editControlPoints = new ArrayList<>(points);
        this.editControlMode = mode;
        this.editDurationMs = durationMs;
        this.editHitValue = hitValue;
        this.currentKeyframeIndex = 0;
        this.selectedKeyframeIndices.clear();
        this.mode = DevMode.EDITING;
        VolumeEditorMode.startCtrlPointForSession(this);
    }

    /**
     * Transitions back to {@link DevMode#IDLE}. The editing state remains accessible
     * (e.g. for a final save) until the next {@link #startEditing} call or session removal.
     */
    public void stopEditing() {
        this.mode = DevMode.IDLE;
    }

    /**
     * Transitions to {@link DevMode#VIEWING} with the given attack's keyframes.
     * Only visualizes — no editing is possible. Use {@link #stopViewing()} to return to IDLE.
     *
     * @param name      the attack id being viewed
     * @param keyframes the keyframes to display (copied into a mutable list)
     */
    public void startViewing(String name, List<VolumeKeyframe> keyframes) {
        this.currentAttackName = name;
        this.editKeyframes = new ArrayList<>(keyframes);
        this.currentKeyframeIndex = -1;
        this.mode = DevMode.VIEWING;
        VolumeEditorMode.startViewingForSession(this);
    }

    /**
     * Transitions back to {@link DevMode#IDLE} from {@link DevMode#VIEWING}.
     */
    public void stopViewing() {
        this.mode = DevMode.IDLE;
    }

    /**
     * Clears the multi-selection set, reverting to single-keyframe selection mode.
     * Call this on a normal (non-shift) click or after any operation that invalidates
     * the current range.
     */
    public void clearSelectedKeyframeIndices() {
        this.selectedKeyframeIndices.clear();
    }

    /**
     * Replaces the keyframe list for the current edit session.
     * Intended for in-place regeneration (e.g. the sweep generator replacing all frames
     * without restarting the session). Clears the multi-selection set.
     *
     * @param keyframes the new keyframe list; copied into a mutable list
     */
    public void setEditKeyframes(List<VolumeKeyframe> keyframes) {
        this.editKeyframes = new ArrayList<>(keyframes);
        this.selectedKeyframeIndices.clear();
        this.currentKeyframeIndex = 0;
    }

    /**
     * Replaces the {@link KeyframeEffect} on the keyframe at {@code index}.
     * Pass {@code null} to clear effects from that keyframe.
     *
     * @param index  keyframe index; must be within {@code [0, editKeyframes.size())}
     * @param effect the new effect bundle, or {@code null} to clear
     */
    public void setKeyframeEffect(int index, KeyframeEffect effect) {
        editKeyframes.set(index, editKeyframes.get(index).withEffect(effect));
    }

    /**
     * Constructs an immutable {@link AttackDef} from the current edit state.
     * Delegates to a control-point build when the session holds CTRL_POINT data;
     * otherwise builds a keyframe-based VOLUME attack.
     *
     * @return the built attack definition
     * @throws IllegalStateException if there is neither keyframe nor control-point data
     */
    public AttackDef buildCurrentAttack() {
        if (editControlPoints != null && !editControlPoints.isEmpty()) {
            return new AttackDef.Builder(currentAttackName)
                .controlPoints(editControlPoints, editControlMode)
                .duration(editDurationMs)
                .onHit(editHitValue != null ? editHitValue
                    : new HitValuePacket(() -> 0f, () -> 10, () -> 2, () -> 0f, () -> 0f))
                .orientWithPitch(editOrientWithPitch)
                .lockOriginOnFire(editLockOriginOnFire)
                .build();
        }
        if (editKeyframes.isEmpty()) {
            throw new IllegalStateException("Cannot build AttackDef: no keyframes defined");
        }
        return new AttackDef.Builder(currentAttackName)
            .type(VolumeType.VOLUME)
            .keyframes(editKeyframes)
            .duration(editDurationMs)
            .onHit(editHitValue != null ? editHitValue
                : new HitValuePacket(() -> 0f, () -> 10, () -> 2, () -> 0f, () -> 0f))
            .orientWithPitch(editOrientWithPitch)
            .lockOriginOnFire(editLockOriginOnFire)
            .build();
    }

    /**
     * Builds the current attack definition from edit state and stores it as the
     * {@linkplain #loadedAttackDef wand-loaded attack}. The next left-click with the
     * test wand will fire this definition instead of the default {@code test_volume_attack}.
     */
    public void loadIntoWand() {
        this.loadedAttackDef = buildCurrentAttack();
    }

    /**
     * Starts an editing session from an existing {@link AttackDef}.
     * Supports {@link KeyframedTrajectory} and {@link ControlPointTrajectory}.
     *
     * @param def the attack definition to edit
     * @throws IllegalArgumentException if the trajectory type is not editable
     */
    public void startEditingFromDef(AttackDef def) {
        if (def.getTrajectory() instanceof ControlPointTrajectory cpt) {
            startEditingCtrlPoint(def.getId(), cpt.getPoints(), cpt.getMode(),
                def.getDurationMs(), def.getHitValue());
            this.editOrientWithPitch = def.isOrientWithPitch();
            this.editLockOriginOnFire = def.isLockOriginOnFire();
            return;
        }
        if (!(def.getTrajectory() instanceof KeyframedTrajectory kt)) {
            throw new IllegalArgumentException("Cannot edit attack with unsupported trajectory: " + def.getId());
        }
        startEditing(def.getId(), kt.getKeyframes(), def.getDurationMs(), def.getHitValue());
        this.editOrientWithPitch = def.isOrientWithPitch();
        this.editLockOriginOnFire = def.isLockOriginOnFire();
    }

    /**
     * Stops whatever active session is running and returns to {@link DevMode#IDLE}.
     * No-op if already {@link DevMode#IDLE}.
     */
    public void stopCurrentSession() {
        switch (mode) {
            case EDITING  -> stopEditing();
            case VIEWING  -> stopViewing();
            case RECORDING -> { this.mode = DevMode.IDLE; }
            default -> { }
        }
    }

    // ── Convenience ───────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this session is in any state other than {@link DevMode#IDLE}.
     *
     * @return {@code true} when a recording or editing session is in progress
     */
    public boolean isActive() {
        return mode != DevMode.IDLE;
    }
}
