package btm.sword.system.hud;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

/**
 * Central registry for systems that want to suppress or replace vanilla HUD elements.
 */
public final class HudOverrideManager {

    private record RegisteredOverride(String key, int priority, HudOverride override) {}

    public static final String CUSTOM_INTERACTION_KEY = "custom_interaction";

    private static final Map<UUID, Map<String, RegisteredOverride>> OVERRIDES = new ConcurrentHashMap<>();

    private static final HudOverride CUSTOM_INTERACTION_OVERRIDE = (player, state) -> {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) != null
            ? player.getAttribute(Attribute.MAX_HEALTH).getValue() // TODO: Make sure you're null-checking instances like this!
            : state.health();
        return new HudRenderState(maxHealth, 20, 5.0f, player.getMaximumAir());
    };

    private HudOverrideManager() {}

    public static void register(Player player, String key, int priority, HudOverride override) {
        OVERRIDES.computeIfAbsent(player.getUniqueId(), ignored -> new ConcurrentHashMap<>())
            .put(key, new RegisteredOverride(key, priority, override));
    }

    public static void clear(Player player, String key) {
        Map<String, RegisteredOverride> playerOverrides = OVERRIDES.get(player.getUniqueId());
        if (playerOverrides == null) {
            return;
        }
        playerOverrides.remove(key);
        if (playerOverrides.isEmpty()) {
            OVERRIDES.remove(player.getUniqueId());
        }
    }

    public static void clearAll(Player player) {
        OVERRIDES.remove(player.getUniqueId());
    }

    public static void enableCustomInteractionOverride(Player player) {
        register(player, CUSTOM_INTERACTION_KEY, 100, CUSTOM_INTERACTION_OVERRIDE);
    }

    public static void disableCustomInteractionOverride(Player player) {
        clear(player, CUSTOM_INTERACTION_KEY);
    }

    public static HudRenderState resolve(Player player, HudRenderState baseState) {
        Map<String, RegisteredOverride> playerOverrides = OVERRIDES.get(player.getUniqueId());
        if (playerOverrides == null || playerOverrides.isEmpty()) {
            return baseState;
        }

        HudRenderState resolved = baseState;
        for (RegisteredOverride override : playerOverrides.values().stream()
            .sorted(Comparator.comparingInt(RegisteredOverride::priority))
            .toList()) {
            resolved = override.override().apply(player, resolved);
        }
        return resolved;
    }

    public static void apply(Player player, HudRenderState state) {
        player.sendHealthUpdate(state.health(), state.food(), state.saturation());
        player.setRemainingAir(state.air());
    }
}
