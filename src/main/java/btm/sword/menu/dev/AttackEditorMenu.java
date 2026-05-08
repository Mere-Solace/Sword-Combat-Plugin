package btm.sword.menu.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.combat.def.AttackDefSerializer;
import btm.sword.combat.def.AttackInstance;
import btm.sword.combat.def.AttackRegistry;
import btm.sword.combat.dev.AttackDevSession;
import btm.sword.combat.hit.HitValuePacket;
import btm.sword.combat.simulation.KeyframeType;
import btm.sword.combat.simulation.VolumeKeyframe;
import btm.sword.combat.simulation.VolumeShape;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.menu.button.ForwardItem;
import btm.sword.menu.button.PreviousItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.PagedGui;
import xyz.xenondevs.invui.gui.structure.Markers;
import xyz.xenondevs.invui.item.Item;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * In-game keyframe editor for VOLUME attacks.
 *
 * <p>Opens against the current {@link AttackDevSession} for the player, displaying all
 * keyframes as a paged list. Click a keyframe to select it; the bottom rows expose
 * manipulation buttons for the selected keyframe.</p>
 *
 * <h2>Layout (6 rows × 9)</h2>
 * <pre>
 * Row 0: Back | SelBefore | Dur- | Info | Dur+ | SelAfter | LoadWand | Save | PitchToggle
 * Row 1: keyframe list (paged content) ×9
 * Row 2: keyframe list (paged content) ×9
 * Row 3: &lt; prev | next &gt; | Effects | Add | Delete | Shape | ShiftY- | ShiftY+ | LockToggle
 * Row 4: Pos -X | +X | -Y | +Y | -Z | +Z | ShiftX- | ShiftX+ | —
 * Row 5: HalfExt -X | +X | -Y | +Y | -Z | +Z | ShiftZ- | ShiftZ+ | —
 * </pre>
 *
 * <p>Left-click adjustment buttons move by ±0.1 blocks/degrees;
 * right-click moves by ±0.5.</p>
 *
 * <p>{@link #save(AttackDevSession)} writes the current state to
 * {@code plugins/sword/attacks/<id>.yml} and updates {@link AttackRegistry}.</p>
 */
public class AttackEditorMenu extends Menu {

    private static final float STEP_SMALL = 0.1f;
    private static final float STEP_LARGE = 0.5f;

    /**
     * Creates an editor for the given player's current editing session.
     *
     * @param player the player owning the session
     */
    public AttackEditorMenu(SwordPlayer player) {
        super(player);
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        List<VolumeKeyframe> keyframes = session.getEditKeyframes();

        // ── Row 0 controls ────────────────────────────────────────────────────

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .build(),
            click -> {
                session.stopEditing();
                new AttackBrowserMenu(swordPlayer).open();
            }
        );

        boolean hasSelectionForDur = !session.getSelectedKeyframeIndices().isEmpty();
        SimpleItem durDecrease = new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("Duration −", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(hasSelectionForDur
                    ? List.of(
                        Component.text("Left: −100ms  Right: −500ms", NamedTextColor.DARK_GRAY),
                        Component.text("With selection: compresses selected frame timing", NamedTextColor.YELLOW))
                    : List.of(
                        Component.text("Left: −100ms  Right: −500ms", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                int delta = click.getClickType().isRightClick() ? 500 : 100;
                if (!session.getSelectedKeyframeIndices().isEmpty()) {
                    scaleSelectionTimes(session, -delta);
                } else {
                    session.setEditDurationMs(Math.max(50, session.getEditDurationMs() - delta));
                }
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        int durMs = session.getEditDurationMs();
        Set<Integer> multiSel = session.getSelectedKeyframeIndices();
        List<Component> infoLore = new ArrayList<>();
        infoLore.add(Component.text("Duration: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(durMs + "ms", NamedTextColor.YELLOW)));
        infoLore.add(Component.text("Frames: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(String.valueOf(keyframes.size()), NamedTextColor.YELLOW)));
        infoLore.add(Component.text("Cursor: ", NamedTextColor.DARK_GRAY)
            .append(Component.text(
                keyframes.isEmpty() ? "—" : String.valueOf(session.getCurrentKeyframeIndex()),
                NamedTextColor.AQUA)));
        if (!multiSel.isEmpty()) {
            infoLore.add(Component.text("Selection: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(multiSel.size() + " frames", NamedTextColor.YELLOW)));
        }
        SimpleItem durInfo = new SimpleItem(
            new ItemStackBuilder(Material.CLOCK)
                .name(Component.text(session.getCurrentAttackName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(infoLore)
                .build()
        );

        SimpleItem durIncrease = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("Duration +", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(hasSelectionForDur
                    ? List.of(
                        Component.text("Left: +100ms  Right: +500ms", NamedTextColor.DARK_GRAY),
                        Component.text("With selection: stretches selected frame timing", NamedTextColor.YELLOW))
                    : List.of(
                        Component.text("Left: +100ms  Right: +500ms", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                int delta = click.getClickType().isRightClick() ? 500 : 100;
                if (!session.getSelectedKeyframeIndices().isEmpty()) {
                    scaleSelectionTimes(session, delta);
                } else {
                    session.setEditDurationMs(session.getEditDurationMs() + delta);
                }
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem selectBefore = new SimpleItem(
            new ItemStackBuilder(Material.SPECTRAL_ARROW)
                .name(Component.text("Select Before Cursor", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Selects all frames before the cursor.", NamedTextColor.DARK_GRAY),
                    Component.text("Nudge buttons operate on selection.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                int cursor = session.getCurrentKeyframeIndex();
                LinkedHashSet<Integer> sel = new LinkedHashSet<>();
                for (int i = 0; i < cursor; i++) sel.add(i);
                session.setSelectedKeyframeIndices(sel);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem selectAfter = new SimpleItem(
            new ItemStackBuilder(Material.SPECTRAL_ARROW)
                .name(Component.text("Select After Cursor", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Selects all frames after the cursor.", NamedTextColor.DARK_GRAY),
                    Component.text("Nudge buttons operate on selection.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                int cursor = session.getCurrentKeyframeIndex();
                LinkedHashSet<Integer> sel = new LinkedHashSet<>();
                for (int i = cursor + 1; i < keyframes.size(); i++) sel.add(i);
                session.setSelectedKeyframeIndices(sel);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem loadWand = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_ROD)
                .name(Component.text("Load Into Wand", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left-click with the volume wand", NamedTextColor.DARK_GRAY),
                    Component.text("will fire this attack.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                if (keyframes.isEmpty()) {
                    swordPlayer.message(Component.text("No keyframes to load.", NamedTextColor.RED));
                    return;
                }
                try {
                    session.loadIntoWand();
                    swordPlayer.message(Component.text(
                        "[Dev] Attack loaded into wand.", NamedTextColor.GREEN));
                } catch (IllegalStateException e) {
                    swordPlayer.message(Component.text("[Dev] " + e.getMessage(), NamedTextColor.RED));
                }
            }
        );

        SimpleItem saveButton = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .name(Component.text("Save", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(Component.text("Save to attacks/<id>.yml", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                if (keyframes.isEmpty()) {
                    swordPlayer.message(Component.text("No keyframes to save.", NamedTextColor.RED));
                    return;
                }
                save(session);
            }
        );

        boolean pitchOn = session.isEditOrientWithPitch();
        SimpleItem pitchToggle = new SimpleItem(
            new ItemStackBuilder(pitchOn ? Material.CYAN_DYE : Material.GRAY_DYE)
                .name(Component.text("Orient With Pitch", pitchOn ? NamedTextColor.AQUA : NamedTextColor.GRAY,
                    TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("OBBs tilt with the player's view angle.", NamedTextColor.DARK_GRAY),
                    Component.text("Currently: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(pitchOn ? "ON" : "OFF",
                            pitchOn ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                ))
                .build(),
            click -> {
                session.setEditOrientWithPitch(!session.isEditOrientWithPitch());
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        HitValuePacket curHit = session.getEditHitValue();
        List<Component> hitLore = new ArrayList<>();
        if (curHit == null) {
            hitLore.add(Component.text("No hit packet selected.", NamedTextColor.GRAY));
        } else {
            hitLore.add(Component.text("shard: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.valueOf(curHit.shardDamage()), NamedTextColor.WHITE)));
            hitLore.add(Component.text("tough: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.format("%.2f", curHit.toughnessDamage()), NamedTextColor.WHITE)));
            hitLore.add(Component.text("soul loss: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.format("%.2f", curHit.soulfireLoss()), NamedTextColor.WHITE)));
            hitLore.add(Component.text("reaped: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.format("%.2f", curHit.reapedSoulfire()), NamedTextColor.WHITE)));
            hitLore.add(Component.text("invul: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(curHit.invulnerableTicks() + "t", NamedTextColor.WHITE)));
            hitLore.add(Component.text("block: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(curHit.blockability().name(), NamedTextColor.WHITE)));
            hitLore.add(Component.text("bypass: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(String.format("%.2f", curHit.bypassPower()), NamedTextColor.WHITE)));
        }
        hitLore.add(Component.empty());
        hitLore.add(Component.text("Click to open the hit-packet library.", NamedTextColor.YELLOW));
        SimpleItem hitPacketButton = new SimpleItem(
            new ItemStackBuilder(Material.NETHERITE_SWORD)
                .name(Component.text("Hit Packet", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(hitLore)
                .build(),
            click -> new HitPacketLibraryMenu(swordPlayer).open()
        );

        boolean lockOn = session.isEditLockOriginOnFire();
        SimpleItem lockToggle = new SimpleItem(
            new ItemStackBuilder(lockOn ? Material.LODESTONE : Material.COMPASS)
                .name(Component.text("Lock Origin On Fire", lockOn ? NamedTextColor.GOLD : NamedTextColor.GRAY,
                    TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Freeze the attack origin at launch.", NamedTextColor.DARK_GRAY),
                    Component.text("Currently: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(lockOn ? "ON" : "OFF",
                            lockOn ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                ))
                .build(),
            click -> {
                session.setEditLockOriginOnFire(!session.isEditLockOriginOnFire());
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        // ── Keyframe items (paged content) ────────────────────────────────────

        List<Item> kfItems = buildKeyframeItems(session);

        // ── Row 3 controls ────────────────────────────────────────────────────

        SimpleItem effectsButton = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_POWDER)
                .name(Component.text("Effects...", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Edit particle and sound effects", NamedTextColor.DARK_GRAY),
                    Component.text("for the selected keyframe.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                if (keyframes.isEmpty()) {
                    swordPlayer.message(Component.text("No keyframe selected.", NamedTextColor.RED));
                    return;
                }
                new KeyframeVisualsMenu(swordPlayer).open();
            }
        );

        SimpleItem addFrame = new SimpleItem(
            new ItemStackBuilder(Material.LIME_TERRACOTTA)
                .name(Component.text("Add Keyframe", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Inserts a frame midway between", NamedTextColor.DARK_GRAY),
                    Component.text("the selected and next frame.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                insertFrameAfterSelected(session);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem deleteFrame = new SimpleItem(
            new ItemStackBuilder(Material.RED_TERRACOTTA)
                .name(Component.text("Delete Frame", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("Removes the selected keyframe.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.size() <= 1) {
                    swordPlayer.message(Component.text("Cannot delete the last keyframe.", NamedTextColor.RED));
                    return;
                }
                kfs.remove(session.getCurrentKeyframeIndex());
                int newIdx = Math.min(session.getCurrentKeyframeIndex(), kfs.size() - 1);
                session.setCurrentKeyframeIndex(newIdx);
                session.clearSelectedKeyframeIndices();
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        VolumeShape currentShape = keyframes.isEmpty()
            ? VolumeShape.SPHERE
            : keyframes.get(session.getCurrentKeyframeIndex()).shape();
        SimpleItem shiftAllYDec = new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("Shift All Y −", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: −0.1  Right: −0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions down.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? -0.5f : -0.1f;
                shiftAllY(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem shiftAllYInc = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("Shift All Y +", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: +0.1  Right: +0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions up.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? 0.5f : 0.1f;
                shiftAllY(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem shiftAllXDec = new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("Shift All X −", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: −0.1  Right: −0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions back on X.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? -0.5f : -0.1f;
                shiftAllX(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem shiftAllXInc = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("Shift All X +", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: +0.1  Right: +0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions forward on X.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? 0.5f : 0.1f;
                shiftAllX(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem shiftAllZDec = new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("Shift All Z −", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: −0.1  Right: −0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions back on Z.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? -0.5f : -0.1f;
                shiftAllZ(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem shiftAllZInc = new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("Shift All Z +", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Left: +0.1  Right: +0.5", NamedTextColor.DARK_GRAY),
                    Component.text("Moves all keyframe positions forward on Z.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> {
                float delta = click.getClickType().isRightClick() ? 0.5f : 0.1f;
                shiftAllZ(session, delta);
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        SimpleItem setShape = new SimpleItem(
            new ItemStackBuilder(Material.ENDER_EYE)
                .name(Component.text("Set Shape", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Current: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(currentShape.name(), NamedTextColor.AQUA)),
                    Component.text("Click to cycle shape.", NamedTextColor.YELLOW)
                ))
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.isEmpty()) return;
                int idx = session.getCurrentKeyframeIndex();
                VolumeKeyframe kf = kfs.get(idx);
                VolumeShape[] shapes = VolumeShape.values();
                VolumeShape next = shapes[(kf.shape().ordinal() + 1) % shapes.length];
                kfs.set(idx, new VolumeKeyframe(kf.t(), kf.localPosition(), kf.halfExtents(), kf.rotation(), next, kf.effect(), kf.jump(), kf.linearToNext(), kf.keyframeType()));
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        // ── Jump toggle ───────────────────────────────────────────────────────

        boolean jumpOn = !keyframes.isEmpty() && keyframes.get(session.getCurrentKeyframeIndex()).jump();
        SimpleItem jumpToggle = new SimpleItem(
            new ItemStackBuilder(jumpOn ? Material.LIGHTNING_ROD : Material.TRIPWIRE_HOOK)
                .name(Component.text("Jump", jumpOn ? NamedTextColor.GOLD : NamedTextColor.GRAY,
                    TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Toggle instant cut for this keyframe.", NamedTextColor.DARK_GRAY),
                    Component.text("When ON, no interpolation occurs entering", NamedTextColor.DARK_GRAY),
                    Component.text("this frame — the animation snaps.", NamedTextColor.DARK_GRAY),
                    Component.text("Currently: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(jumpOn ? "ON" : "OFF",
                            jumpOn ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                ))
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.isEmpty()) return;
                int idx = session.getCurrentKeyframeIndex();
                kfs.set(idx, kfs.get(idx).withJump(!kfs.get(idx).jump()));
                new AttackEditorMenu(swordPlayer).open();
            }
        );

        // ── Row 4 — position nudge ─────────────────────────────────────────────

        SimpleItem posXDec = posButton(session, "Pos X −", NamedTextColor.RED, false, Axis.POS_X, true);
        SimpleItem posXInc = posButton(session, "Pos X +", NamedTextColor.GREEN, false, Axis.POS_X, false);
        SimpleItem posYDec = posButton(session, "Pos Y −", NamedTextColor.RED, false, Axis.POS_Y, true);
        SimpleItem posYInc = posButton(session, "Pos Y +", NamedTextColor.GREEN, false, Axis.POS_Y, false);
        SimpleItem posZDec = posButton(session, "Pos Z −", NamedTextColor.RED, false, Axis.POS_Z, true);
        SimpleItem posZInc = posButton(session, "Pos Z +", NamedTextColor.GREEN, false, Axis.POS_Z, false);

        // ── Row 5 — half-extents nudge ────────────────────────────────────────

        SimpleItem heXDec = posButton(session, "Half-X −", NamedTextColor.RED, true, Axis.POS_X, true);
        SimpleItem heXInc = posButton(session, "Half-X +", NamedTextColor.GREEN, true, Axis.POS_X, false);
        SimpleItem heYDec = posButton(session, "Half-Y −", NamedTextColor.RED, true, Axis.POS_Y, true);
        SimpleItem heYInc = posButton(session, "Half-Y +", NamedTextColor.GREEN, true, Axis.POS_Y, false);
        SimpleItem heZDec = posButton(session, "Half-Z −", NamedTextColor.RED, true, Axis.POS_Z, true);
        SimpleItem heZInc = posButton(session, "Half-Z +", NamedTextColor.GREEN, true, Axis.POS_Z, false);

        // ── Build GUI ─────────────────────────────────────────────────────────

        PagedGui<Item> gui = PagedGui.items()
            .setStructure(
                "B c D I U d L S P",
                "x x x x x x x x x",
                "x x x x x x x x x",
                "< > E A F V J K T",
                "q w e r t y m n j",
                "u i o p g h a b H")
            .addIngredient('#', BORDER)
            .addIngredient('>', new ForwardItem())
            .addIngredient('<', new PreviousItem())
            .addIngredient('B', back)
            .addIngredient('c', selectBefore)
            .addIngredient('D', durDecrease)
            .addIngredient('I', durInfo)
            .addIngredient('U', durIncrease)
            .addIngredient('d', selectAfter)
            .addIngredient('L', loadWand)
            .addIngredient('S', saveButton)
            .addIngredient('P', pitchToggle)
            .addIngredient('x', Markers.CONTENT_LIST_SLOT_HORIZONTAL)
            .addIngredient('E', effectsButton)
            .addIngredient('A', addFrame)
            .addIngredient('F', deleteFrame)
            .addIngredient('V', setShape)
            .addIngredient('J', shiftAllYDec)
            .addIngredient('K', shiftAllYInc)
            .addIngredient('T', lockToggle)
            // pos row
            .addIngredient('q', posXDec)
            .addIngredient('w', posXInc)
            .addIngredient('e', posYDec)
            .addIngredient('r', posYInc)
            .addIngredient('t', posZDec)
            .addIngredient('y', posZInc)
            .addIngredient('m', shiftAllXDec)
            .addIngredient('n', shiftAllXInc)
            .addIngredient('j', jumpToggle)
            // half-extents row
            .addIngredient('u', heXDec)
            .addIngredient('i', heXInc)
            .addIngredient('o', heYDec)
            .addIngredient('p', heYInc)
            .addIngredient('g', heZDec)
            .addIngredient('h', heZInc)
            .addIngredient('a', shiftAllZDec)
            .addIngredient('b', shiftAllZInc)
            .addIngredient('H', hitPacketButton)
            .setContent(kfItems)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Attack Editor — " + session.getCurrentAttackName())
            .setGui(gui)
            .build()
            .open();
    }

    // ── Keyframe list items ───────────────────────────────────────────────────

    private List<Item> buildKeyframeItems(AttackDevSession session) {
        List<VolumeKeyframe> keyframes = session.getEditKeyframes();
        int cursor = session.getCurrentKeyframeIndex();
        Set<Integer> multiSel = session.getSelectedKeyframeIndices();
        List<Item> items = new ArrayList<>(keyframes.size());

        for (int i = 0; i < keyframes.size(); i++) {
            final int idx = i;
            VolumeKeyframe kf = keyframes.get(i);
            boolean isPrimary = (idx == cursor);
            boolean inRange = !multiSel.isEmpty() && multiSel.contains(idx);

            Vector3f pos = kf.localPosition();
            Vector3f he = kf.halfExtents();

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(String.format("t = %.3f", kf.t()), NamedTextColor.AQUA));
            lore.add(Component.text("shape: ", NamedTextColor.DARK_GRAY)
                .append(Component.text(kf.shape().name(), NamedTextColor.LIGHT_PURPLE)));
            lore.add(Component.text(
                String.format("pos:  x=%.2f  y=%.2f  z=%.2f", pos.x, pos.y, pos.z),
                NamedTextColor.GRAY));
            if (kf.shape() == VolumeShape.SPHERE) {
                lore.add(Component.text(String.format("radius: %.2f", he.x), NamedTextColor.GRAY));
            } else {
                lore.add(Component.text(
                    String.format("half: x=%.2f  y=%.2f  z=%.2f", he.x, he.y, he.z),
                    NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            if (isPrimary) {
                lore.add(Component.text("▶ Cursor", NamedTextColor.GOLD, TextDecoration.BOLD));
            } else if (inRange) {
                lore.add(Component.text("◆ In selection", NamedTextColor.YELLOW, TextDecoration.BOLD));
            } else {
                lore.add(Component.text("Click to select  Shift+Click to range-select", NamedTextColor.YELLOW));
            }

            Material mat;
            NamedTextColor nameColor;
            if (isPrimary) {
                mat = Material.LIME_STAINED_GLASS_PANE;
                nameColor = NamedTextColor.GOLD;
            } else if (inRange) {
                mat = Material.YELLOW_STAINED_GLASS_PANE;
                nameColor = NamedTextColor.YELLOW;
            } else {
                mat = paneForType(kf.keyframeType());
                nameColor = nameColorForType(kf.keyframeType());
            }

            String typeTag = kf.keyframeType() != KeyframeType.STANDARD
                ? " [" + kf.keyframeType().name() + "]" : "";
            int effectCount = kf.effect() != null && kf.effect().displays() != null
                ? kf.effect().displays().size() : 0;
            ItemStack kfItem = new ItemStackBuilder(mat)
                .name(Component.text("Frame " + idx + typeTag, nameColor, TextDecoration.BOLD))
                .lore(lore)
                .build();
            if (effectCount > 0) kfItem.setAmount(Math.min(effectCount, 64));
            items.add(new SimpleItem(
                kfItem,
                click -> {
                    ClickType type = click.getClickType();
                    if (type == ClickType.SWAP_OFFHAND) {
                        session.clearSelectedKeyframeIndices();
                        session.setCurrentKeyframeIndex(idx);
                        new KeyframeVisualsMenu(swordPlayer).open();
                        return;
                    }
                    if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT) {
                        int lo = Math.min(cursor, idx);
                        int hi = Math.max(cursor, idx);
                        LinkedHashSet<Integer> range = new LinkedHashSet<>();
                        for (int j = lo; j <= hi; j++) range.add(j);
                        session.setSelectedKeyframeIndices(range);
                    } else {
                        session.clearSelectedKeyframeIndices();
                        session.setCurrentKeyframeIndex(idx);
                    }
                    new AttackEditorMenu(swordPlayer).open();
                }
            ));
        }
        return items;
    }

    // ── Adjustment button factory ─────────────────────────────────────────────

    /**
     * Creates a nudge button for position or half-extents on a given axis.
     *
     * @param session    the active editing session
     * @param label      display label
     * @param color      name colour
     * @param halfExtent {@code true} to adjust half-extents, {@code false} for position
     * @param axis       the axis to adjust
     * @param decrement  {@code true} to subtract, {@code false} to add
     */
    private SimpleItem posButton(AttackDevSession session, String label, NamedTextColor color,
            boolean halfExtent, Axis axis, boolean decrement) {
        boolean hasSelection = !session.getSelectedKeyframeIndices().isEmpty();
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("L: ±0.1  R: ±0.5", NamedTextColor.DARK_GRAY));
        if (hasSelection) {
            lore.add(Component.text("Applies to " + session.getSelectedKeyframeIndices().size()
                + " selected frame(s) + cursor", NamedTextColor.YELLOW));
        }
        return new SimpleItem(
            new ItemStackBuilder(halfExtent ? Material.ORANGE_DYE : Material.LIGHT_BLUE_DYE)
                .name(Component.text(label, color, TextDecoration.BOLD))
                .lore(lore)
                .build(),
            click -> {
                List<VolumeKeyframe> kfs = session.getEditKeyframes();
                if (kfs.isEmpty()) return;
                float step = click.getClickType().isRightClick() ? STEP_LARGE : STEP_SMALL;
                float delta = decrement ? -step : step;
                Set<Integer> sel = session.getSelectedKeyframeIndices();
                if (sel.isEmpty()) {
                    int idx = session.getCurrentKeyframeIndex();
                    kfs.set(idx, applyDelta(kfs.get(idx), halfExtent, axis, delta));
                } else {
                    applyDeltaToIndices(kfs, sel, halfExtent, axis, delta);
                    applyDeltaToIndex(kfs, session.getCurrentKeyframeIndex(), halfExtent, axis, delta);
                }
                new AttackEditorMenu(swordPlayer).open();
            }
        );
    }

    private static void applyDeltaToIndices(List<VolumeKeyframe> kfs, Set<Integer> indices,
            boolean halfExtent, Axis axis, float delta) {
        for (int idx : indices) {
            if (idx >= 0 && idx < kfs.size()) {
                applyDeltaToIndex(kfs, idx, halfExtent, axis, delta);
            }
        }
    }

    private static void applyDeltaToIndex(List<VolumeKeyframe> kfs, int idx,
            boolean halfExtent, Axis axis, float delta) {
        if (idx < 0 || idx >= kfs.size()) return;
        kfs.set(idx, applyDelta(kfs.get(idx), halfExtent, axis, delta));
    }

    private enum Axis { POS_X, POS_Y, POS_Z }

    /**
     * Returns a new {@link VolumeKeyframe} with the given delta applied to the specified field.
     *
     * @param kf         the source keyframe
     * @param halfExtent {@code true} to modify half-extents, {@code false} for position
     * @param axis       the axis to modify
     * @param delta      amount to add
     * @return a new keyframe record with the updated value
     */
    private static VolumeKeyframe applyDelta(VolumeKeyframe kf, boolean halfExtent, Axis axis, float delta) {
        if (halfExtent) {
            Vector3f he = new Vector3f(kf.halfExtents());
            switch (axis) {
                case POS_X -> he.x = Math.max(0.05f, he.x + delta);
                case POS_Y -> he.y = Math.max(0.05f, he.y + delta);
                case POS_Z -> he.z = Math.max(0.05f, he.z + delta);
            }
            return kf.withHalfExtents(he);
        } else {
            Vector3f pos = new Vector3f(kf.localPosition());
            switch (axis) {
                case POS_X -> pos.x += delta;
                case POS_Y -> pos.y += delta;
                case POS_Z -> pos.z += delta;
            }
            return kf.withLocalPosition(pos);
        }
    }

    // ── Add frame ─────────────────────────────────────────────────────────────

    /**
     * Inserts a new keyframe after the currently selected one.
     * If the selected frame is the last, duplicates it with {@code t} advanced by 0.1
     * (capped at {@code 1.0}). Otherwise interpolates position, half-extents, and rotation
     * from the selected frame and its successor.
     *
     * @param session the active editing session
     */
    private static void insertFrameAfterSelected(AttackDevSession session) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        if (kfs.isEmpty()) {
            kfs.add(new VolumeKeyframe(0f, new Vector3f(0f, 1f, 1f), new Vector3f(0.5f, 0.5f, 0.5f), new Quaternionf(), VolumeShape.SPHERE, null, false, false, KeyframeType.STANDARD));
            session.setCurrentKeyframeIndex(0);
            return;
        }

        int idx = session.getCurrentKeyframeIndex();
        VolumeKeyframe cur = kfs.get(idx);

        VolumeKeyframe newFrame;
        if (idx < kfs.size() - 1) {
            VolumeKeyframe next = kfs.get(idx + 1);
            float t = (cur.t() + next.t()) / 2f;
            Vector3f pos = new Vector3f(cur.localPosition()).lerp(next.localPosition(), 0.5f);
            Vector3f he = new Vector3f(cur.halfExtents()).lerp(next.halfExtents(), 0.5f);
            Quaternionf rot = new Quaternionf(cur.rotation()).slerp(next.rotation(), 0.5f);
            newFrame = new VolumeKeyframe(t, pos, he, rot, cur.shape(), null, false, false, KeyframeType.STANDARD);
        } else {
            float t = Math.min(1.0f, cur.t() + 0.1f);
            newFrame = new VolumeKeyframe(t,
                new Vector3f(cur.localPosition()),
                new Vector3f(cur.halfExtents()),
                new Quaternionf(cur.rotation()),
                cur.shape(), null, false, false, KeyframeType.STANDARD);
        }

        kfs.add(idx + 1, newFrame);
        session.setCurrentKeyframeIndex(idx + 1);
    }

    // ── Shift All X / Y / Z ──────────────────────────────────────────────────

    /**
     * Applies an X-axis delta to every keyframe's local position.
     *
     * @param session the active editing session
     * @param delta   amount to add to each keyframe's {@code localPosition.x}
     */
    private static void shiftAllX(AttackDevSession session, float delta) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        for (int i = 0; i < kfs.size(); i++) {
            VolumeKeyframe kf = kfs.get(i);
            Vector3f pos = new Vector3f(kf.localPosition());
            pos.x += delta;
            kfs.set(i, kf.withLocalPosition(pos));
        }
    }

    /**
     * Applies a Y-axis delta to every keyframe's local position.
     *
     * @param session the active editing session
     * @param delta   amount to add to each keyframe's {@code localPosition.y}
     */
    private static void shiftAllY(AttackDevSession session, float delta) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        for (int i = 0; i < kfs.size(); i++) {
            VolumeKeyframe kf = kfs.get(i);
            Vector3f pos = new Vector3f(kf.localPosition());
            pos.y += delta;
            kfs.set(i, kf.withLocalPosition(pos));
        }
    }

    /**
     * Applies a Z-axis delta to every keyframe's local position.
     *
     * @param session the active editing session
     * @param delta   amount to add to each keyframe's {@code localPosition.z}
     */
    private static void shiftAllZ(AttackDevSession session, float delta) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        for (int i = 0; i < kfs.size(); i++) {
            VolumeKeyframe kf = kfs.get(i);
            Vector3f pos = new Vector3f(kf.localPosition());
            pos.z += delta;
            kfs.set(i, kf.withLocalPosition(pos));
        }
    }

    // ── Scale selection times ─────────────────────────────────────────────────

    /**
     * Stretches or compresses the normalized {@code t} values of the selected keyframes
     * around their collective midpoint by {@code deltaMs} milliseconds, leaving the total
     * attack duration and all unselected keyframes untouched.
     *
     * @param session the active editing session
     * @param deltaMs positive to stretch, negative to compress (in milliseconds)
     */
    private static void scaleSelectionTimes(AttackDevSession session, int deltaMs) {
        List<VolumeKeyframe> kfs = session.getEditKeyframes();
        Set<Integer> sel = session.getSelectedKeyframeIndices();
        float durationMs = session.getEditDurationMs();
        float minT = (float) sel.stream().mapToDouble(i -> kfs.get(i).t()).min().orElse(0);
        float maxT = (float) sel.stream().mapToDouble(i -> kfs.get(i).t()).max().orElse(1);
        float centerT = (minT + maxT) / 2f;
        float spanMs = (maxT - minT) * durationMs;
        float newSpanMs = Math.max(50f, spanMs + deltaMs);
        float scale = spanMs > 1e-4f ? newSpanMs / spanMs : 1f;
        for (int idx : sel) {
            VolumeKeyframe kf = kfs.get(idx);
            float newT = Math.max(0f, Math.min(1f, centerT + (kf.t() - centerT) * scale));
            kfs.set(idx, kf.withT(newT));
        }
    }

    // ── Keyframe type styling ─────────────────────────────────────────────────

    private static Material paneForType(KeyframeType type) {
        return switch (type) {
            case BEZIER_START -> Material.GREEN_STAINED_GLASS_PANE;
            case BEZIER_C1 -> Material.CYAN_STAINED_GLASS_PANE;
            case BEZIER_C2 -> Material.ORANGE_STAINED_GLASS_PANE;
            case BEZIER_END -> Material.RED_STAINED_GLASS_PANE;
            case LINE -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            default -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }

    private static NamedTextColor nameColorForType(KeyframeType type) {
        return switch (type) {
            case BEZIER_START -> NamedTextColor.GREEN;
            case BEZIER_C1 -> NamedTextColor.DARK_AQUA;
            case BEZIER_C2 -> NamedTextColor.GOLD;
            case BEZIER_END -> NamedTextColor.RED;
            case LINE -> NamedTextColor.AQUA;
            default -> NamedTextColor.WHITE;
        };
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Builds the current attack, registers it in {@link AttackRegistry}, and writes it to
     * {@code plugins/sword/attacks/<id>.yml}.
     *
     * @param session the active editing session
     */
    private void save(AttackDevSession session) {
        saveAttack(session, swordPlayer);
    }

    /**
     * Builds the current attack, registers it in {@link AttackRegistry}, and writes it to
     * {@code plugins/sword/attacks/<id>.yml}. Callable from any sub-menu in this package.
     *
     * @param session the active editing session
     * @param player  the player to send feedback messages to
     */
    static void saveAttack(AttackDevSession session, SwordPlayer player) {
        try {
            AttackInstance def = session.buildCurrentAttack();
            AttackRegistry.register(def);

            File attacksDir = new File(Sword.getInstance().getDataFolder(), "attacks");
            attacksDir.mkdirs();
            File file = new File(attacksDir, def.getId() + ".yml");
            AttackDefSerializer.save(file, def);

            player.message(Component.text(
                "[Dev] Saved '" + def.getId() + "' → attacks/" + def.getId() + ".yml",
                NamedTextColor.AQUA));
        } catch (Exception e) {
            player.message(Component.text("[Dev] Save failed: " + e.getMessage(), NamedTextColor.RED));
        }
    }
}
