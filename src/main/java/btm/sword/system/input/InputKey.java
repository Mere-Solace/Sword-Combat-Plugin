package btm.sword.system.input;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.item.SwordItemType;

/**
 * Represents an input key (type + allowed item types + optional predicate).
 * The predicate allows arbitrary runtime checks (e.g., slot equipped, skill unlocked).
 * NOTE: Predicate equality is not relied upon for path uniqueness — builder will ensure
 * predicates are only applied at the leaf node to avoid divergent-key collisions.
 */
public record InputKey(InputType input, List<SwordItemType> allowedItemTypes, Predicate<SwordPlayer> accessibilityPredicate) {

    public InputKey(InputType input, List<SwordItemType> allowedItemTypes) {
        this(input, allowedItemTypes, p -> true);
    }

    public InputKey(InputType input) {
        this(input, List.of(SwordItemType.GENERIC), p -> true);
    }

    public static InputKey of(InputType input) {
        return new InputKey(input);
    }

    public static InputKey of(InputType input, SwordItemType allowedItemType) {
        return new InputKey(input, List.of(Objects.requireNonNull(allowedItemType)));
    }

    public static InputKey of(InputType input, SwordItemType... allowedItemTypes) {
        return new InputKey(input, Arrays.asList(Objects.requireNonNull(allowedItemTypes)));
    }

    public static InputKey of(InputType input, Predicate<SwordPlayer> accessibilityPredicate) {
        return new InputKey(input, List.of(SwordItemType.GENERIC), accessibilityPredicate);
    }

    /**
     * Checks whether this input key is accessible for the given player (based on held item type or predicate).
     */
    public boolean checkAccessibility(SwordPlayer swordPlayer) {
        if (!accessibilityPredicate.test(swordPlayer)) return false;
        SwordItemType held = SwordItemType.fromString(swordPlayer.getItemStackInHand(true));
        return allowedItemTypes.contains(SwordItemType.GENERIC) || allowedItemTypes.contains(held);
    }

    /**
     * Returns true if this key allows the specified item type.
     */
    public boolean allows(SwordItemType itemType) {
        return allowedItemTypes.contains(SwordItemType.GENERIC) || allowedItemTypes.contains(itemType);
    }
}
