package btm.sword.system.action.skill;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

/**
 * Unique identifier for a {@link Skill}, backed by a Bukkit {@link NamespacedKey}.
 *
 * <p>Use the {@link #of(String, String)} factory for most cases (namespace = plugin id,
 * value = skill name). {@link #parse(String)} accepts the {@code "namespace:value"} serialised
 * form used in player-data persistence.</p>
 */
public record SkillId(NamespacedKey key) {

    /* =========================
       Factory methods
       ========================= */

    /**
     * Creates a {@code SkillId} from an existing {@link NamespacedKey}.
     *
     * @param key the namespaced key
     * @return the corresponding {@code SkillId}
     */
    public static SkillId of(NamespacedKey key) {
        return new SkillId(key);
    }

    /**
     * Creates a {@code SkillId} from a namespace and value string.
     *
     * @param namespace the namespace (usually the plugin id)
     * @param value     the skill name within the namespace
     * @return the corresponding {@code SkillId}
     */
    public static SkillId of(String namespace, String value) {
        return new SkillId(new NamespacedKey(namespace, value));
    }

    /**
     * Parses a {@code "namespace:value"} string into a {@code SkillId}.
     *
     * @param serialized the serialised form
     * @return the parsed {@code SkillId}
     * @throws IllegalArgumentException if the string is not a valid namespaced key
     */
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

    /**
     * Returns the string form of this ID ({@code "namespace:value"}).
     *
     * @return serialised key string
     */
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
