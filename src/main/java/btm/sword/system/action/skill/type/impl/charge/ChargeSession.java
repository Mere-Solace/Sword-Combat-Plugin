package btm.sword.system.action.skill.type.impl.charge;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.ItemDisplay;

import btm.sword.system.control.TimeArbiter;
import lombok.Getter;
import lombok.Setter;

/**
 * Holds the runtime state of an active ability charge.
 *
 * <p>Created by {@link ChargeAction#startCharge} when the player begins charging,
 * and disposed by {@link ChargeAction#releaseCharge} or {@link ChargeAction#cancelCharge}.</p>
 *
 * <p>Abilities store their own per-tick state (current scale, rotation, etc.) in the
 * generic {@link #data} map.</p>
 */
@Getter
@Setter
public class ChargeSession {

    private final ChargeableAbility ability;
    private final ItemDisplay display;
    private final int slotIndex;
    private final long startTimeMs;
    private TimeArbiter.TaskHandle tickTask;

    /**
     * Generic data map for ability-specific state (e.g. current scale, rotation angle).
     * Avoids the need for session subclasses.
     */
    private final Map<String, Object> data = new HashMap<>();

    /**
     * Creates a new charge session.
     *
     * @param ability   the chargeable ability being charged
     * @param display   the display entity
     * @param slotIndex the hotbar slot index (1 or 2)
     */
    public ChargeSession(ChargeableAbility ability, ItemDisplay display, int slotIndex) {
        this.ability = ability;
        this.display = display;
        this.slotIndex = slotIndex;
        this.startTimeMs = System.currentTimeMillis();
    }

    /**
     * Convenience getter for a float value from the data map.
     *
     * @param key          the data key
     * @param defaultValue the value to return if the key is absent
     * @return the stored float or the default
     */
    public float getFloat(String key, float defaultValue) {
        Object v = data.get(key);
        return v instanceof Number n ? n.floatValue() : defaultValue;
    }

    /**
     * Convenience setter for a float value in the data map.
     *
     * @param key   the data key
     * @param value the value to store
     */
    public void setFloat(String key, float value) {
        data.put(key, value);
    }

    /**
     * Cancels the tick task and removes the display entity.
     */
    public void dispose() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }
}
