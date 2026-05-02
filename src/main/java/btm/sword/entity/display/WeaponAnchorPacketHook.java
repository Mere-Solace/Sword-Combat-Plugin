package btm.sword.entity.display;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.utility.MinecraftReflection;
import com.comphenix.protocol.wrappers.Vector3F;
import com.comphenix.protocol.wrappers.WrappedDataValue;

import btm.sword.Sword;
import btm.sword.util.misc.Debug;

/**
 * ProtocolLib-based final-authority override for weapon-slot anchor {@link ItemDisplay} entities.
 *
 * <p>Registers a {@code ENTITY_METADATA} packet listener that intercepts outgoing metadata
 * for any tracked anchor and forces the desired item, scale, translation, and rotation before
 * the packet reaches any client. This hook owns five indices:</p>
 * <ul>
 *   <li>Index 11 — translation ({@code Vector3f}): DEU's animated value is preserved and the
 *       per-material translation offset is added on top.</li>
 *   <li>Index 12 — scale ({@code Vector3f}): forced to the configured weapon scale.</li>
 *   <li>Index 13 — left rotation ({@code Quaternionf}): composed with a per-material rotation
 *       offset on top of whatever DEU provides.</li>
 *   <li>Index 14 — right rotation ({@code Quaternionf}): forced to identity.</li>
 *   <li>Index 23 — item ({@code ItemStack}): forced to the weapon item (or AIR when hidden).</li>
 * </ul>
 *
 * <p>This means DEU can reset the item, change the scale, or do anything else server-side —
 * none of it reaches clients while an override is active.</p>
 *
 * <p>Call {@link #register()} once at plugin startup.
 * Call {@link #override} when a weapon is equipped and {@link #clear} when it is removed.</p>
 */
public final class WeaponAnchorPacketHook {

    // Display entity metadata indices — stable since 1.19.4 through 1.21.x.
    /** Index 8: interpolation start delta ticks (int) on {@link org.bukkit.entity.Display}. */
    private static final int INDEX_INTERPOLATION_DELAY = 8;
    /** Index 9: transformation interpolation duration (int) on {@link org.bukkit.entity.Display}. */
    private static final int INDEX_INTERPOLATION_DURATION = 9;

    private static final int INDEX_TRANSLATION = 11;
    /** Index 12: uniform scale {@code Vector3f} on {@link org.bukkit.entity.Display}. */
    private static final int INDEX_SCALE = 12;
    /** Index 13: left rotation {@code Quaternionf} on {@link org.bukkit.entity.Display}. */
    private static final int INDEX_LEFT_ROTATION = 13;
    /** Index 14: right rotation {@code Quaternionf} on {@link org.bukkit.entity.Display}. */
    private static final int INDEX_RIGHT_ROTATION = 14;
    /** Index 23: displayed item on {@link ItemDisplay}. */
    private static final int INDEX_ITEM = 23;

    /**
     * Active overrides keyed by entity ID (int, not UUID) for O(1) lookup on the hot packet path.
     * {@link ConcurrentHashMap} because {@code onPacketSending} runs on ProtocolLib's async I/O thread.
     */
    private static final Map<Integer, OverrideState> OVERRIDES = new ConcurrentHashMap<>();

    /**
     * Immutable snapshot of what a weapon-slot anchor should display.
     *
     * @param nmsItem          pre-converted NMS {@code ItemStack} (avoids per-packet Bukkit→NMS conversion)
     * @param scale            uniform weapon scale
     * @param rotOffset        additional rotation right-multiplied onto DEU's animated left rotation
     * @param translationOffset per-material offset added on top of DEU's animated bone translation
     */
    record OverrideState(Object nmsItem, float scale, Quaternionf rotOffset, Vector3f translationOffset) {}

    /**
     * Registers the {@code ENTITY_METADATA} packet listener with ProtocolLib.
     * Must be called once during plugin startup, before any mobs are spawned.
     */
    public static void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(
            new PacketAdapter(Sword.getInstance(), ListenerPriority.HIGHEST,
                PacketType.Play.Server.ENTITY_METADATA) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    int entityId = event.getPacket().getIntegers().read(0);
                    OverrideState state = OVERRIDES.get(entityId);
                    if (state == null) return;

                    List<WrappedDataValue> metadata =
                        event.getPacket().getDataValueCollectionModifier().read(0);
                    if (metadata == null) return;

                    boolean touched = false;
                    for (WrappedDataValue entry : metadata) {
                        switch (entry.getIndex()) {
                            case INDEX_INTERPOLATION_DELAY -> {
                                // Force zero so our transformations apply immediately —
                                // prevents DEU's per-frame interpolation setup from causing
                                // the client to blend toward a previous near-zero-scale state.
                                entry.setValue(0);
                                touched = true;
                            }
                            case INDEX_INTERPOLATION_DURATION -> {
                                entry.setValue(0);
                                touched = true;
                            }
                            case INDEX_TRANSLATION -> {
                                Object raw = entry.getValue();

                                if (raw instanceof Vector3F v) {
                                    Vector3f base = new Vector3f(v.getX(), v.getY(), v.getZ());

                                    Vector3f finalPos = base.add(new Vector3f(1, 1, 1)); // <-- YOU control this

                                    entry.setValue(new Vector3F(finalPos.x, finalPos.y, finalPos.z));
                                    touched = true;
                                } else if (raw instanceof Vector3f v) {
                                    Vector3f finalPos = new Vector3f(v).add(new Vector3f(1, 1, 1));

                                    entry.setValue(finalPos);
                                    touched = true;
                                }
                            }
                            case INDEX_SCALE -> {
                                Object cur = entry.getValue();
                                float s = state.scale();
                                if (cur instanceof Vector3F) {
                                    entry.setValue(new Vector3F(s, s, s));
                                } else {
                                    entry.setValue(new Vector3f(s, s, s));
                                }
                                Debug.animation("PacketHook scale: entity=" + entityId
                                    + " type=" + (cur != null ? cur.getClass().getSimpleName() : "null")
                                    + " -> " + s);
                                touched = true;
                            }
                            case INDEX_LEFT_ROTATION -> {
                                Object raw = entry.getValue();
                                if (raw instanceof Quaternionf deuRot) {
                                    entry.setValue(new Quaternionf(deuRot).mul(state.rotOffset()));
                                }
                                Debug.animation("PacketHook rot: entity=" + entityId
                                    + " type=" + (raw != null ? raw.getClass().getSimpleName() : "null"));
                                touched = true;
                            }
                            case INDEX_RIGHT_ROTATION -> {
                                // Force identity — right rotation is unused for weapon display.
                                entry.setValue(new Quaternionf());
                                touched = true;
                            }
                            case INDEX_ITEM -> {
                                entry.setValue(state.nmsItem());
                                touched = true;
                            }
                            default -> {}
                        }
                    }

                    if (touched) {
                        // Log ALL indices in this packet so we can see exactly what DEU sends.
                        StringBuilder indices = new StringBuilder();
                        for (WrappedDataValue e : metadata) {
                            indices.append(e.getIndex()).append(' ');
                        }
                        Debug.animation("PacketHook: overrode entity=" + entityId
                            + " entries=" + metadata.size() + " indices=[" + indices.toString().trim() + "]");
                    }
                }
            }
        );
    }

    /**
     * Begins overriding all outgoing metadata for the given anchor entity.
     *
     * <p>The NMS item is pre-converted at registration time to avoid per-packet conversion cost.
     * Any metadata DEU sends for this entity (transformation resets, item resets, etc.) will be
     * silently overridden before reaching clients.</p>
     *
     * @param anchor            the weapon-slot anchor {@link ItemDisplay}
     * @param item              the item to display (must not be AIR; use {@link #clear} to hide)
     * @param scale             uniform scale for the weapon
     * @param rotOffset         additional rotation composited onto DEU's animated bone rotation
     * @param translationOffset per-material offset added on top of DEU's animated bone translation
     */
    public static void override(ItemDisplay anchor, ItemStack item, float scale,
            Quaternionf rotOffset, Vector3f translationOffset) {
        Object nmsItem = MinecraftReflection.getMinecraftItemStack(item);
        OVERRIDES.put(anchor.getEntityId(), new OverrideState(nmsItem, scale, rotOffset, translationOffset));
        Debug.animation("PacketHook: override registered entity=" + anchor.getEntityId()
            + " item=" + item.getType() + " scale=" + scale);
    }

    /**
     * Stops overriding metadata for the given anchor, returning full control to DEU.
     *
     * <p><strong>Must be called before</strong> any cleanup {@code setItemStack} or
     * {@code setTransformation} calls so those server-side packets reach clients unintercepted.</p>
     *
     * @param anchor the anchor to release
     */
    public static void clear(ItemDisplay anchor) {
        OVERRIDES.remove(anchor.getEntityId());
        Debug.animation("PacketHook: override cleared entity=" + anchor.getEntityId());
    }

    private WeaponAnchorPacketHook() {}
}
