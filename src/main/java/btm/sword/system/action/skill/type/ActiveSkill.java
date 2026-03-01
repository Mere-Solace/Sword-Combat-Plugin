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

public abstract class ActiveSkill implements Skill {

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

    public abstract void execute(Combatant combatant);
    public abstract int calculateCooldown(Combatant combatant);
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
