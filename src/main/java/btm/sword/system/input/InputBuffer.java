package btm.sword.system.input;

import java.util.EnumMap;

import org.bukkit.Bukkit;

/**
 * Per-player input buffer that prevents duplicate inputs from firing within the same server tick.
 * <p>
 * Minecraft often fires multiple Bukkit events for a single physical input — for example,
 * left-clicking an entity fires both {@code PrePlayerAttackEntityEvent} and
 * {@code PlayerInteractEvent}. Without deduplication, the same input is processed twice,
 * causing actions like attacks to double-fire.
 * </p>
 * <p>
 * Each {@link InputType} is tracked by the server tick it was last accepted on.
 * If the same input type is submitted again in the same tick, it is rejected.
 * </p>
 */
public class InputBuffer {
    private final EnumMap<InputType, Integer> lastAcceptedTick = new EnumMap<>(InputType.class);

    /**
     * Attempts to accept an input for processing. Returns {@code true} if the input
     * is fresh (not yet seen this tick) and should be processed, or {@code false}
     * if it is a duplicate within the current server tick.
     *
     * @param input the input type to accept
     * @return {@code true} if the input should be processed, {@code false} if it is a duplicate
     */
    public boolean accept(InputType input) {
        int currentTick = Bukkit.getCurrentTick();
        Integer lastTick = lastAcceptedTick.get(input);
        if (lastTick != null && lastTick == currentTick) {
            return false;
        }
        lastAcceptedTick.put(input, currentTick);
        return true;
    }

    /**
     * Called once per server tick. Reserved for future input forgiveness and retry logic.
     */
    public void tick() {
        // Future: drain retry queue for input forgiveness
    }
}
