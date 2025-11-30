package btm.sword.system.input;

import java.util.List;

import btm.sword.system.item.SwordItemType;

public record InputKey (InputType input, List<SwordItemType> allowedItemTypes) {

    public static InputKey of(InputType input) {
        return new InputKey(input, List.of(SwordItemType.GENERIC));
    }

    public static InputKey of(InputType input, SwordItemType allowedItemType) {
        return new InputKey(input, List.of(allowedItemType));
    }

    public static InputKey of(InputType input, SwordItemType... allowedItemTypes) {
        return new InputKey(input, List.of(allowedItemTypes));
    }

    public boolean allowed(SwordItemType itemType) {
        return allowedItemTypes.contains(itemType) || allowedItemTypes.contains(SwordItemType.GENERIC);
    }
}
