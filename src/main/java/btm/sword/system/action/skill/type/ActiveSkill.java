package btm.sword.system.action.skill.type;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.input.InputAction;
import btm.sword.system.item.ItemStackBuilder;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

/**
 * Base class for player-activated skills that occupy {@code ACTIVE} or {@code UMBRAL} slots.
 *
 * <p>Concrete subclasses implement {@link #execute}, {@link #calculateCooldown}, and
 * {@link #canPerform}. The cached {@link InputAction} is stored here so cooldown state
 * survives input-tree rebuilds without resetting the timer.</p>
 */
public abstract class ActiveSkill implements Skill {

    /** Creates an {@code ActiveSkill}. */
    protected ActiveSkill() {}

    /** Cached {@link InputAction} so cooldowns persist across dynamic re-resolves. */
    @Getter @Setter
    private InputAction cachedInputAction;
    @Override
    public ItemStack icon() {
        return ItemStackBuilder.of(Material.SPECTRAL_ARROW)
            .name(Component.text("Yet-to-be-implemented Active Skill"))
            .hideAll()
            .build();
    }

    /**
     * Executes this skill for the given combatant. Called when the player's input matches
     * the registered input sequence and all cast guards pass.
     *
     * @param combatant the player performing the skill
     */
    public abstract void execute(Combatant combatant);

    /**
     * Returns the cooldown for this skill in milliseconds.
     *
     * @param combatant the player performing the skill
     * @return cooldown duration in milliseconds
     */
    public abstract int calculateCooldown(Combatant combatant);

    /**
     * Returns {@code true} if this skill can currently be performed by the given combatant.
     *
     * @param combatant the player to check
     * @return {@code true} if the skill's preconditions are met
     */
    public abstract boolean canPerform(Combatant combatant);

    /**
     * Whether this skill's input sequence requires a held right-click ({@code RIGHT_HOLD})
     * rather than a tap ({@code RIGHT_TAP}). Override to return {@code true} for throwable or
     * chargeable active skills.
     *
     * @return false by default (tap activation)
     */
    public boolean requiresHold() {
        return false;
    }
}
