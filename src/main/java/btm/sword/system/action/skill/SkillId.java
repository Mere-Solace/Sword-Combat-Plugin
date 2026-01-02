package btm.sword.system.action.skill;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

public record SkillId(NamespacedKey key) {

    /* =========================
       Factory methods
       ========================= */

    public static SkillId of(NamespacedKey key) {
        return new SkillId(key);
    }

    public static SkillId of(String namespace, String value) {
        return new SkillId(new NamespacedKey(namespace, value));
    }

    public static SkillId parse(String serialized) {
        NamespacedKey key = NamespacedKey.fromString(serialized);
        if (key == null) {
            throw new IllegalArgumentException("Invalid SkillId: " + serialized);
        }
        return new SkillId(key);
    }

    /* =========================
       Accessors
       ========================= */

    public String asString() {
        return key.toString(); // namespace:value
    }

    /* =========================
       Object contract
       ========================= */

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SkillId(NamespacedKey key1))) return false;
        return key.equals(key1);
    }

    @Override
    public @NotNull String toString() {
        return asString();
    }
}
