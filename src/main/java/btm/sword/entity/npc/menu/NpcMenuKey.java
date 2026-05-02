package btm.sword.entity.npc.menu;

import java.util.Objects;

/**
 * Stable string-keyed identifier for an NPC menu surface.
 * <p>
 * Used by {@link NpcMenuRouter} to look up menu factories without forcing the
 * dialogue layer to depend on concrete menu implementations. Instances are
 * created once, typically as {@code public static final} constants on the
 * package or feature that owns the menu.
 * </p>
 *
 * @param name unique key string; conventionally lower-case dotted (e.g. {@code "npc.shop"})
 */
public record NpcMenuKey(String name) {

    /** Compact constructor enforces a non-blank, lower-case-friendly key. */
    public NpcMenuKey {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("NpcMenuKey name must not be blank");
        }
    }
}
