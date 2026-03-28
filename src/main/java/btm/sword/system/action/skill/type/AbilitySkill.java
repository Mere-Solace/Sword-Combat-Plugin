package btm.sword.system.action.skill.type;

import java.util.Set;

import org.bukkit.inventory.ItemStack;

import btm.sword.system.action.skill.AbilityType;
import btm.sword.system.action.skill.AbilityUseType;
import btm.sword.system.action.skill.Skill;

/**
 * Marker interface for abilities — skills that exist as physical world items.
 *
 * <p>Abilities extend the base {@link Skill} contract with:
 * <ol>
 *   <li>A world item ({@link #buildWorldItem()}) that represents the ability as a droppable,
 *       findable, equippable {@link ItemStack} tagged with the ability's {@link #id()}.</li>
 *   <li>Physical-item consumption rules ({@link #consumesOnUse()} /
 *       {@link #requiresPhysicalItemToUse()}) that govern whether the item is spent
 *       from the player's inventory when the ability fires.</li>
 *   <li>A {@link #useType()} that controls how hotbar-slot uses are tracked
 *       (stack count, durability bar, or cooldown-only).</li>
 * </ol>
 *
 * <p>Implementations should extend one of the concrete base classes:
 * {@link ActivatableAbility}, {@link ThrowableAbility}, {@link ConsumableAbility},
 * {@link PassiveAbilitySkill}, or {@link QuestAbility}.
 */
public interface AbilitySkill extends Skill {

    /**
     * Returns the ability classification governing activation and consumption behavior.
     *
     * @return the {@link AbilityType} for this ability
     */
    AbilityType abilityType();

    /**
     * Builds the physical {@link ItemStack} that represents this ability in the world.
     * The returned item is tagged with the {@code ability_id} PDC key so it can be
     * identified and matched against this ability's {@link #id()}.
     *
     * @return a new, tagged world item for this ability
     */
    ItemStack buildWorldItem();

    /**
     * Whether using this ability removes one physical item from the player's inventory.
     * True for {@link AbilityType#CONSUMABLE} and {@link AbilityType#THROWABLE}.
     *
     * @return {@code true} if the item is consumed on activation
     */
    boolean consumesOnUse();

    /**
     * Whether a physical ability item must be present in the player's inventory
     * before this ability can be activated. Equal to {@link #consumesOnUse()} in
     * the base design — separated for clarity and potential future divergence.
     *
     * @return {@code true} if inventory presence is required to cast
     */
    boolean requiresPhysicalItemToUse();

    /**
     * Returns the set of active resource-tracking mechanisms for this ability's hotbar slot.
     * Multiple types may be combined — they are non-exclusive:
     * <ul>
     *   <li>{@link AbilityUseType#STACK} — each activation decrements the slot's use count;
     *       the slot becomes {@code DEPLETED} when the count reaches zero.</li>
     *   <li>{@link AbilityUseType#DURABILITY} — each activation degrades the item's durability bar;
     *       the slot becomes {@code DEPLETED} when durability reaches zero.</li>
     *   <li>{@link AbilityUseType#COOLDOWN} — applies a Minecraft item-cooldown overlay after each
     *       activation; the slot is never depleted by this mechanism alone.</li>
     * </ul>
     * An empty set means no slot-level resource tracking (the ability can always be used).
     *
     * @return the set of active use types; never {@code null}
     */
    default Set<AbilityUseType> useTypes() {
        return Set.of();
    }

    /**
     * For abilities with {@link AbilityUseType#STACK}, the number of uses in a fresh stack.
     * Ignored if {@code STACK} is not in {@link #useTypes()}. Default is {@code 1}.
     *
     * @return the maximum use count
     */
    default int maxUses() {
        return 1;
    }

    /**
     * For abilities with {@link AbilityUseType#DURABILITY}, the maximum durability value.
     * Ignored if {@code DURABILITY} is not in {@link #useTypes()}. Default is {@code 1}.
     *
     * @return the maximum durability
     */
    default int maxDurability() {
        return 1;
    }

    /**
     * For abilities with {@link AbilityUseType#COOLDOWN}, the cooldown duration in ticks
     * applied as a Minecraft item-cooldown overlay after each activation.
     * Ignored if {@code COOLDOWN} is not in {@link #useTypes()}. Default is {@code 0}.
     *
     * @return cooldown duration in ticks
     */
    default int cooldownTicks() {
        return 0;
    }

    /**
     * Whether this ability doubles as a weak melee weapon when the player
     * presses LEFT while holding the ability item. Default is {@code false}.
     *
     * @return {@code true} if the ability item can deal weak basic attacks
     */
    default boolean isWeapon() {
        return false;
    }
}
