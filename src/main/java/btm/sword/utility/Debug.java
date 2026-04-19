package btm.sword.utility;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import btm.sword.Sword;
import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.impl.DevSwordPlayer;
import btm.sword.utility.display.DrawUtil;
import btm.sword.utility.math.Basis;
import btm.sword.utility.math.VectorUtil;

/**
 * Centralized debug utility. Each category maps to a {@link Config.Debug} flag and a DevMenu toggle.
 * <p>
 * All methods log to the server console and, if online, send a chat message to each player
 * listed in {@code devnames.txt} (plugin data folder). Caller class and line number are captured automatically via
 * {@link StackWalker} — no need to pass them manually.
 * </p>
 *
 * <ul>
 *   <li>{@link #debug}    – General/fallback debug        ({@code VERBOSE_DEBUG})</li>
 *   <li>{@link #combat}   – Combat events                  ({@code VERBOSE_COMBAT})</li>
 *   <li>{@link #movement} – Movement / dash                ({@code VERBOSE_MOVEMENT})</li>
 *   <li>{@link #inventory}– Inventory interactions         ({@code VERBOSE_INVENTORY})</li>
 *   <li>{@link #system}   – State machine, scheduling      ({@code VERBOSE_SYSTEM})</li>
 *   <li>{@link #umbral}   – UmbralBlade specific           ({@code VERBOSE_UMBRAL})</li>
 *   <li>{@link #hostile}  – Hostile entity behaviour       ({@code VERBOSE_HOSTILE})</li>
 *   <li>{@link #listener} – Bukkit/Packet listener events  ({@code VERBOSE_LISTENER})</li>
 *   <li>{@link #animation}– DEU animation hook             ({@code VERBOSE_ANIMATION})</li>
 *   <li>{@link #input}    – Input combo trie / dispatch    ({@code VERBOSE_INPUT})</li>
 *   <li>{@link #skill}    – Skill slot resolution          ({@code VERBOSE_SKILL})</li>
 *   <li>{@link #ability}  – Ability lifecycle              ({@code VERBOSE_ABILITY})</li>
 *   <li>{@link #grab}     – Grab action lifecycle          ({@code VERBOSE_GRAB})</li>
 *   <li>{@link #attack}        – Attack sweeps / hitbox         ({@code VERBOSE_ATTACK})</li>
 *   <li>{@link #throwing}      – Thrown-item lifecycle          ({@code VERBOSE_THROWING})</li>
 *   <li>{@link #attackVolume}  – AttackDef volume simulation    ({@code VERBOSE_ATTACK_VOLUME})</li>
 * </ul>
 */
public final class Debug {

    private Debug() {}

    private static final List<String> DEV_NAMES = new ArrayList<>();

    private static final StackWalker WALKER =
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    /**
     * When {@code false}, {@link btm.sword.system.item.special.NonMovableItem#isNonMovable}
     * always returns {@code false}, letting devs freely move special items for testing.
     */
    public static boolean SPECIAL_ITEM_CHECKS_ENABLED = true;

    /**
     * Loads the list of dev player names from {@code devnames.txt} in the plugin data folder.
     * One name per line; lines beginning with {@code #} and blank lines are ignored.
     * Safe to call on reload — clears the previous list first.
     *
     * @param dataFolder the plugin's data folder ({@link org.bukkit.plugin.Plugin#getDataFolder()})
     */
    public static void init(File dataFolder) {
        DEV_NAMES.clear();
        File file = new File(dataFolder, "devnames.txt");
        if (!file.exists()) return;
        try {
            for (String entry : Files.readAllLines(file.toPath())) {
                entry = entry.strip();
                if (!entry.isEmpty() && !entry.startsWith("#")) {
                    DEV_NAMES.add(entry);
                }
            }
        } catch (IOException e) {
            Sword.print("[Debug] Failed to read devnames.txt: " + e.getMessage());
        }
    }

    // =========================================================================
    // Category methods
    // =========================================================================

    /** General debug output. Gated by {@link Config.Debug#LOGGING_VERBOSE_DEBUG}. */
    public static void debug(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_DEBUG, "Debug", message);
    }

    /** Combat-system debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_COMBAT}. */
    public static void combat(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_COMBAT, "Combat", message);
    }

    /** Movement/dash debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_MOVEMENT}. */
    public static void movement(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_MOVEMENT, "Movement", message);
    }

    /** Inventory-interaction debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_INVENTORY}. */
    public static void inventory(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_INVENTORY, "Inventory", message);
    }

    /**
     * System-level debug (state machine transitions, scheduler tasks, display lifecycle).
     * Gated by {@link Config.Debug#LOGGING_VERBOSE_SYSTEM}.
     */
    public static void system(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_SYSTEM, "System", message);
    }

    /** UmbralBlade-specific debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_UMBRAL}. */
    public static void umbral(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_UMBRAL, "Umbral", message);
    }

    /** Hostile-entity debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_HOSTILE}. */
    public static void hostile(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_HOSTILE, "Hostile", message);
    }

    /** Bukkit/Packet listener debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_LISTENER}. */
    public static void listener(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_LISTENER, "Listener", message);
    }

    /** DEU animation hook / weapon-slot display debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_ANIMATION}. */
    public static void animation(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_ANIMATION, "Animation", message);
    }

    /** Input combo detection and trie traversal debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_INPUT}. */
    public static void input(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_INPUT, "Input", message);
    }

    /** Skill slot resolution and {@link btm.sword.system.action.skill.container.PlayerSkillContainer} debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_SKILL}. */
    public static void skill(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_SKILL, "Skill", message);
    }

    /** Ability activation, soulfire cost, cooldown, and charge session debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_ABILITY}. */
    public static void ability(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_ABILITY, "Ability", message);
    }

    /** Grab action lifecycle debug (cast, hold, release, target). Gated by {@link Config.Debug#LOGGING_VERBOSE_GRAB}. */
    public static void grab(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_GRAB, "Grab", message);
    }

    /** Attack sweep, hitbox detection, and damage dispatch debug. Gated by {@link Config.Debug#LOGGING_VERBOSE_ATTACK}. */
    public static void attack(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_ATTACK, "Attack", message);
    }

    /** Thrown-item lifecycle debug (spawn, flight, collision, recall). Gated by {@link Config.Debug#LOGGING_VERBOSE_THROWING}. */
    public static void throwing(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_THROWING, "Throwing", message);
    }

    /** AttackDef volume simulation debug (launch, hit confirmation, expiry). Gated by {@link Config.Debug#LOGGING_VERBOSE_ATTACK_VOLUME}. */
    public static void attackVolume(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_ATTACK_VOLUME, "AttackVolume", message);
    }

    /** ParticleEffect → ParticleWrapper conversion debug (type, dustOptions, dispatch path). Gated by {@link Config.Debug#LOGGING_VERBOSE_PARTICLE_DISPLAY}. */
    public static void particleDisplay(String message) {
        emit(Config.Debug.LOGGING_VERBOSE_PARTICLE_DISPLAY, "ParticleDisplay", message);
    }

    // =========================================================================
    // Visual helpers
    // =========================================================================

    /**
     * Renders a particle blob at the given location for visual debugging.
     *
     * @param debugLocation world location to display the blob
     */
    public static void debugBlob(Location debugLocation) {
        Prefab.Particles.DEBUG_BLOB.display(debugLocation);
    }

    /**
     * Renders the three basis axes (right, up, forward) as particle lines originating from
     * {@code location} in the given {@code direction}.
     *
     * @param location  origin of the basis
     * @param direction direction to align the basis to
     * @param length    length of each rendered axis line
     */
    public static void debugBasis(Location location, Vector direction, double length) {
        Basis testBasis = VectorUtil.getBasis(location, direction);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
            location, testBasis.right(), length, 0.25);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
            location, testBasis.up(), length, 0.25);
        DrawUtil.line(List.of(Prefab.Particles.TEST_SWORD_BLUE),
            location, testBasis.forward(), length, 0.25);
    }

    /**
     * Sends a formatted dump of an {@link InventoryClickEvent} to the clicking player.
     * No-ops if {@link Config.Debug#LOGGING_VERBOSE_INVENTORY} is disabled.
     *
     * @param event the inventory click event to describe
     */
    public static void sendInventoryClickDebugMessage(InventoryClickEvent event) {
        if (!Config.Debug.LOGGING_VERBOSE_INVENTORY) return;

        StringBuilder b = new StringBuilder("> Inventory Click <\n");
        b.append("Click: ").append(event.getClick())
            .append(" | Action: ").append(event.getAction()).append('\n');
        b.append("Slot: ").append(event.getSlot())
            .append(" (raw ").append(event.getRawSlot())
            .append(") | Hotbar: ").append(event.getHotbarButton()).append('\n');

        b.append("ClickedInv: ");
        if (event.getClickedInventory() == null) b.append("null");
        else b.append(event.getClickedInventory().getType());
        b.append(" | Top: ").append(event.getView().getTopInventory().getType())
            .append(" | Bot: ").append(event.getView().getBottomInventory().getType()).append('\n');

        b.append("Result: ").append(event.getResult())
            .append(" | Cancelled: ").append(event.isCancelled()).append('\n');

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        b.append("Current: ").append(current == null ? "null" : current.getType() + " x" + current.getAmount())
            .append(" | Cursor: ").append(cursor.getType()).append(" x").append(cursor.getAmount()).append('\n');

        b.append("Who: ").append(event.getWhoClicked().getName());

        event.getWhoClicked().sendMessage(b.toString());
    }

    // =========================================================================
    // Internal
    // =========================================================================

    /**
     * Core emit: checks the flag, resolves the caller location via {@link StackWalker},
     * logs to console, and forwards to the dev player if online.
     *
     * <p>Call depth (skip 3): {@code callerLocation} → {@code emit} → public method → actual caller.</p>
     */
    @SuppressWarnings("all")
    private static void emit(boolean flag, String tag, String message) {
        if (!flag) return;

        String location = WALKER.walk(s -> s.skip(3).findFirst()
            .map(f -> f.getDeclaringClass().getSimpleName() + ":" + f.getLineNumber())
            .orElse("?:?"));

        String line = "[" + tag + "][" + location + "] " + message;
        Sword.print(line);

        for (SwordEntity se : SwordEntityArbiter.getONLINE_SWORD_PLAYERS().values()) {
            if (se instanceof DevSwordPlayer dsp) {
                dsp.message(line);
            }
        }
    }
}
