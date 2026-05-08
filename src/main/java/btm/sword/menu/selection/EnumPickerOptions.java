package btm.sword.menu.selection;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Optional presentation hooks for {@link EnumSelectionMenu#forEnum}.
 *
 * <p>Every field is nullable; unset fields fall back to sensible defaults
 * (flat value list, enum name as label, built-in material mapping, no
 * preview action, no filter). Use {@link #builder()} for a readable
 * constructor.</p>
 *
 * @param <E> the enum type being picked
 */
public final class EnumPickerOptions<E extends Enum<E>> {

    @Nullable private final Predicate<E> filter;
    @Nullable private final Function<E, String> label;
    @Nullable private final Consumer<E> onPreview;
    @Nullable private final Function<E, String> groupKey;
    @Nullable private final Function<E, Material> material;

    private EnumPickerOptions(@Nullable Predicate<E> filter,
                              @Nullable Function<E, String> label,
                              @Nullable Consumer<E> onPreview,
                              @Nullable Function<E, String> groupKey,
                              @Nullable Function<E, Material> material) {
        this.filter = filter;
        this.label = label;
        this.onPreview = onPreview;
        this.groupKey = groupKey;
        this.material = material;
    }

    /** Creates a builder; all fields default to null. */
    public static <E extends Enum<E>> Builder<E> builder() {
        return new Builder<>();
    }

    /** Returns a no-op options instance — all defaults. */
    public static <E extends Enum<E>> EnumPickerOptions<E> none() {
        return new EnumPickerOptions<>(null, null, null, null, null);
    }

    @Nullable public Predicate<E> filter() { return filter; }
    @Nullable public Function<E, String> label() { return label; }
    @Nullable public Consumer<E> onPreview() { return onPreview; }
    @Nullable public Function<E, String> groupKey() { return groupKey; }
    @Nullable public Function<E, Material> material() { return material; }

    /** Builder for {@link EnumPickerOptions}. */
    public static final class Builder<E extends Enum<E>> {
        private Predicate<E> filter;
        private Function<E, String> label;
        private Consumer<E> onPreview;
        private Function<E, String> groupKey;
        private Function<E, Material> material;

        /** Excludes constants for which the predicate returns {@code false}. */
        public Builder<E> filter(Predicate<E> f) { this.filter = f; return this; }

        /** Overrides the displayed label for each constant. */
        public Builder<E> label(Function<E, String> l) { this.label = l; return this; }

        /** Right-click handler (e.g. play a sound preview). */
        public Builder<E> onPreview(Consumer<E> p) { this.onPreview = p; return this; }

        /**
         * Enables a two-level browser — constants are grouped by the returned
         * string and the top-level browser shows one entry per group.
         */
        public Builder<E> groupKey(Function<E, String> g) { this.groupKey = g; return this; }

        /** Overrides the display material for each constant. */
        public Builder<E> material(Function<E, Material> m) { this.material = m; return this; }

        /** Builds the {@link EnumPickerOptions} from the configured fields. */
        public EnumPickerOptions<E> build() {
            return new EnumPickerOptions<>(filter, label, onPreview, groupKey, material);
        }
    }
}
