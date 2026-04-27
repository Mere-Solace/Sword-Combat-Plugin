package btm.sword.system.entity.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import btm.sword.system.attack.dev.AnimationModeInputHandler;
import btm.sword.system.attack.dev.AttackDevSession;
import btm.sword.system.attack.dev.SaveConfirmDialog;
import btm.sword.system.input.InputType;
import btm.sword.system.inventory.menu.dev.SweepGeneratorMenu;
import btm.sword.system.item.KeyRegistry;
import btm.sword.system.playerdata.PlayerData;
import lombok.Getter;
import lombok.Setter;

/**
 * A developer-only subclass of {@link SwordPlayer} constructed at login for players
 * whose usernames are listed in the bundled {@code devnames.txt} resource.
 *
 * <p>Unlike a normal {@link SwordPlayer}, this wrapper is constructed once per login
 * and stays resident — no runtime swap is needed. AnimationMode is an internal state
 * flag ({@link #inAnimationMode}), mirroring the {@code inCreativeDevMode} pattern
 * already present on {@link SwordPlayer}.</p>
 *
 * <h2>AnimationMode behaviour</h2>
 * <ul>
 *   <li>The UmbralBlade FSM ticks identically to a normal {@link SwordPlayer} at all times.
 *       When AnimationMode is entered, {@link Combatant#setUmbralBladeActive(boolean)} suppresses
 *       the blade; setting it back to {@code true} on exit causes the lifecycle owner to respawn it.</li>
 *   <li>{@link #act(InputType)} routes to {@link AnimationModeInputHandler} when
 *       {@link #isInAnimationMode()} is {@code true}; otherwise falls through to the
 *       normal input execution tree so non-animation dev interactions still work.</li>
 * </ul>
 *
 * <h2>Dev-name loading</h2>
 * Call {@link #loadDevNames(Plugin)} once in {@code Sword.onEnable()} before any players
 * join. The names are read from the {@code devnames.txt} JAR resource (one username per
 * line, {@code #} lines are comments). If the resource is absent all players are treated
 * as normal.
 */
@Setter
@Getter
public final class DevSwordPlayer extends SwordPlayer {

    // ── Static dev-name registry ──────────────────────────────────────────────

    private static final Set<String> DEV_NAMES = new HashSet<>();

    /**
     * Reads {@code devnames.txt} from the plugin's JAR resources and populates the
     * static dev-name set. Must be called once from {@code Sword.onEnable()} before
     * any players connect. Lines starting with {@code #} and blank lines are ignored.
     *
     * @param plugin the loaded plugin instance used for resource access and logging
     */
    public static void loadDevNames(Plugin plugin) {
        DEV_NAMES.clear();
        try (InputStream is = plugin.getResource("devnames.txt")) {
            if (is == null) {
                plugin.getLogger().info("[DevSwordPlayer] devnames.txt not found in JAR — no dev players registered.");
                return;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        DEV_NAMES.add(line);
                    }
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "[DevSwordPlayer] Failed to read devnames.txt", e);
        }
        plugin.getLogger().info("[DevSwordPlayer] Registered " + DEV_NAMES.size() + " dev player(s).");
    }

    /**
     * Returns {@code true} if the given username appears in the loaded dev-name list.
     *
     * @param username the Minecraft username to check (case-sensitive)
     * @return whether this player should be wrapped as a {@link DevSwordPlayer}
     */
    public static boolean isDevPlayer(String username) {
        return DEV_NAMES.contains(username);
    }

    // ── Instance state ────────────────────────────────────────────────────────

    /**
     * -- GETTER --
     *  Returns
     *  when the player is actively in AnimationMode —
     *  hotbar populated with editing tools and combat suppressed.
     * -- SETTER --
     *  Sets the AnimationMode flag. Called by
     *  on entry and exit.
     *
     */
    private boolean inAnimationMode = false;

    /**
     * Constructs a DevSwordPlayer wrapping the given entity with the provided data.
     * The UmbralBlade is never ticked for this wrapper.
     *
     * @param associatedEntity the Bukkit living entity (player) to wrap
     * @param data             the player data for this player
     */
    public DevSwordPlayer(LivingEntity associatedEntity, PlayerData data) {
        super(associatedEntity, data);
    }

    /**
     * Routes input to {@link AnimationModeInputHandler} when in AnimationMode.
     * Falls through to the normal input execution tree otherwise, so non-animation
     * dev inputs still work.
     *
     * @param input the input type received from the player
     */
    @Override
    public void act(InputType input) {
        if (inAnimationMode) {
            AnimationModeInputHandler.handle(this, input);
            return;
        }
        if (handleUmbralBladeTesterInput(input)) {
            return;
        }
        super.act(input);
    }

    /**
     * Routes lifecycle commands while the player holds the UmbralBlade tester item
     * (BREEZE_ROD tagged with {@link KeyRegistry#UMBRAL_BLADE_TESTER_KEY}).
     *
     * <ul>
     *   <li>{@link InputType#LEFT}: deactivates the blade — destroys the instance and
     *       blocks recreation until activate is called.</li>
     *   <li>{@link InputType#DROP}: activates the blade — the lifecycle owner respawns
     *       it on the next tick.</li>
     * </ul>
     *
     * @param input the input received from the player
     * @return {@code true} if the input was handled by the tester and should not fall
     *         through to the normal input tree
     */
    private boolean handleUmbralBladeTesterInput(InputType input) {
        ItemStack mainHand = player().getInventory().getItemInMainHand();
        if (mainHand == null || mainHand.isEmpty()
            || !KeyRegistry.hasKey(mainHand, KeyRegistry.UMBRAL_BLADE_TESTER_KEY)) {
            return false;
        }
        switch (input) {
            case LEFT -> deactivateUmbralBlade();
            case DROP -> activateUmbralBlade();
            default -> {
                return false;
            }
        }
        return true;
    }

    /**
     * Extends the base inventory click handler with AnimationMode slot locks.
     *
     * <p>When in AnimationMode:</p>
     * <ul>
     *   <li>Slots 0–8 (animation tool hotbar) are fully locked.</li>
     *   <li>Slot 35 (BARRIER exit button) cancels movement; a left-click opens
     *       {@link SaveConfirmDialog}.</li>
     *   <li>All other slots fall through to the normal handler.</li>
     * </ul>
     *
     * @param event the inventory click event to handle
     * @return {@code true} if the event was handled and should be cancelled
     */
    @Override
    public boolean handleInventoryInput(InventoryClickEvent event) {
        if (!inAnimationMode) {
            // Volume-attack wand click → open Sweep Generator
            if (event.getClickedInventory() == player().getInventory()) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && KeyRegistry.hasKey(clicked, KeyRegistry.TEST_VOLUME_ATTACK_KEY)) {
                    new SweepGeneratorMenu(this).open();
                    return true;
                }
            }
            return super.handleInventoryInput(event);
        }

        if (event.getClickedInventory() != player().getInventory()) {
            return super.handleInventoryInput(event);
        }

        int slot = event.getSlot();

        if (slot == 35) {
            if (event.getClick() == ClickType.LEFT) {
                AttackDevSession session = AttackDevSession.get(player().getUniqueId());
                SaveConfirmDialog.open(this, session);
            }
            return true;
        }

        if (slot >= 0 && slot <= 8) {
            return true;
        }

        return super.handleInventoryInput(event);
    }
}
