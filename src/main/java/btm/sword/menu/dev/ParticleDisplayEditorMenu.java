package btm.sword.menu.dev;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.joml.Vector3f;

import btm.sword.Sword;
import btm.sword.combat.def.ParticleDisplayLibrary;
import btm.sword.combat.dev.AttackDevSession;
import btm.sword.combat.visuals.CircleDisplay;
import btm.sword.combat.visuals.LineDisplay;
import btm.sword.combat.visuals.OriginAnchor;
import btm.sword.combat.visuals.ParticleDisplay;
import btm.sword.combat.visuals.PointDisplay;
import btm.sword.combat.visuals.SphereDisplay;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.item.core.ItemStackBuilder;
import btm.sword.menu.Menu;
import btm.sword.util.misc.ChatInputCapture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.Window;

/**
 * Editor for a single {@link ParticleDisplay} at a specific keyframe and display index.
 *
 * <p>Shared controls live in rows 0-3: anchor picker, origin offset (XYZ), random offset
 * range (XYZ), repeat count and period. Rows 4-5 carry shape-specific controls based on
 * the concrete {@link ParticleDisplay} subtype. Swap-click on the anchor cell also opens
 * the picker as a shortcut alongside the normal left-click.</p>
 */
public class ParticleDisplayEditorMenu extends Menu {

    private final int kfIndex;
    private final int displayIndex;

    /**
     * @param player       the player opening the editor
     * @param kfIndex      keyframe index within the edit session
     * @param displayIndex display index within the keyframe's display list
     */
    public ParticleDisplayEditorMenu(SwordPlayer player, int kfIndex, int displayIndex) {
        super(player);
        this.kfIndex = kfIndex;
        this.displayIndex = displayIndex;
    }

    @Override
    public void open() {
        AttackDevSession session = AttackDevSession.getOrCreate(swordPlayer.player());
        ParticleDisplay display = fetch(session);
        if (display == null) {
            new KeyframeVisualsMenu(swordPlayer).open();
            return;
        }

        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY)).build(),
            click -> new KeyframeVisualsMenu(swordPlayer).open()
        );

        SimpleItem save = new SimpleItem(
            new ItemStackBuilder(Material.EMERALD)
                .name(Component.text("Save", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("Save to attacks/<id>.yml", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> AttackEditorMenu.saveAttack(session, swordPlayer)
        );

        SimpleItem info = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text(display.shapeTypeLabel() + " Display", NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Keyframe #" + kfIndex + "  •  Slot " + displayIndex, NamedTextColor.DARK_GRAY),
                    Component.text("Particles: " + display.getParticles().size(), NamedTextColor.GRAY)))
                .build()
        );

        SimpleItem anchor = anchorButton(display);
        SimpleItem editParticles = new SimpleItem(
            new ItemStackBuilder(Material.BLAZE_POWDER)
                .name(Component.text("Edit Particles", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text(display.getParticles().size() + " entries", NamedTextColor.GRAY),
                    Component.text("Open particle list.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new ParticleListMenu(swordPlayer, kfIndex, displayIndex).open()
        );
        SimpleItem deleteDisplay = new SimpleItem(
            new ItemStackBuilder(Material.TNT)
                .name(Component.text("Delete Display", NamedTextColor.DARK_RED, TextDecoration.BOLD)).build(),
            click -> {
                session.removeKeyframeDisplay(kfIndex, displayIndex);
                new KeyframeVisualsMenu(swordPlayer).open();
            }
        );

        SimpleItem saveToLibrary = new SimpleItem(
            new ItemStackBuilder(Material.BOOKSHELF)
                .name(Component.text("Save to Library", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Saves a copy of this display to particles.yml.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> ChatInputCapture.prompt(
                swordPlayer.player(),
                Component.text("Enter a name for this preset:", NamedTextColor.AQUA),
                name -> {
                    if (name.equalsIgnoreCase("cancel")) return;
                    String key = name.trim().toLowerCase().replace(' ', '_');
                    if (key.isEmpty()) return;
                    ParticleDisplayLibrary.register(key, display,
                        new File(Sword.getInstance().getDataFolder(), "particles.yml"));
                    swordPlayer.message(Component.text(
                        "[Dev] Saved preset '" + key + "' to particles.yml", NamedTextColor.GREEN));
                })
        );

        Vector3f off = display.getOriginOffset();
        SimpleItem offXDec = dec(click -> { off.x -= stepFloat(click); display.setOriginOffset(off); open(); });
        SimpleItem offXDisp = displayFloat("Off X", off.x, Material.RED_STAINED_GLASS, v -> { off.x = v; display.setOriginOffset(off); open(); });
        SimpleItem offXInc = inc(click -> { off.x += stepFloat(click); display.setOriginOffset(off); open(); });
        SimpleItem offYDec = dec(click -> { off.y -= stepFloat(click); display.setOriginOffset(off); open(); });
        SimpleItem offYDisp = displayFloat("Off Y", off.y, Material.LIME_STAINED_GLASS, v -> { off.y = v; display.setOriginOffset(off); open(); });
        SimpleItem offYInc = inc(click -> { off.y += stepFloat(click); display.setOriginOffset(off); open(); });
        SimpleItem offZDec = dec(click -> { off.z -= stepFloat(click); display.setOriginOffset(off); open(); });
        SimpleItem offZDisp = displayFloat("Off Z", off.z, Material.LIGHT_BLUE_STAINED_GLASS, v -> { off.z = v; display.setOriginOffset(off); open(); });
        SimpleItem offZInc = inc(click -> { off.z += stepFloat(click); display.setOriginOffset(off); open(); });

        Vector3f rnd = display.getRandomOffsetRange();
        SimpleItem rndXDec = dec(click -> { rnd.x = Math.max(0f, rnd.x - stepFloat(click)); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndXDisp = displayFloat("Rand X", rnd.x, Material.PINK_STAINED_GLASS, v -> { rnd.x = Math.max(0f, v); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndXInc = inc(click -> { rnd.x += stepFloat(click); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndYDec = dec(click -> { rnd.y = Math.max(0f, rnd.y - stepFloat(click)); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndYDisp = displayFloat("Rand Y", rnd.y, Material.GREEN_STAINED_GLASS, v -> { rnd.y = Math.max(0f, v); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndYInc = inc(click -> { rnd.y += stepFloat(click); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndZDec = dec(click -> { rnd.z = Math.max(0f, rnd.z - stepFloat(click)); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndZDisp = displayFloat("Rand Z", rnd.z, Material.CYAN_STAINED_GLASS, v -> { rnd.z = Math.max(0f, v); display.setRandomOffsetRange(rnd); open(); });
        SimpleItem rndZInc = inc(click -> { rnd.z += stepFloat(click); display.setRandomOffsetRange(rnd); open(); });

        SimpleItem rcDec = dec(click -> { display.setRepeatCount(Math.max(1, display.getRepeatCount() - stepInt(click))); open(); });
        SimpleItem rcDisp = displayInt("Repeat", display.getRepeatCount(), Material.REPEATER, v -> { display.setRepeatCount(Math.max(1, v)); open(); });
        SimpleItem rcInc = inc(click -> { display.setRepeatCount(display.getRepeatCount() + stepInt(click)); open(); });
        SimpleItem pdDec = dec(click -> { display.setRepeatPeriodTicks(Math.max(0, display.getRepeatPeriodTicks() - stepInt(click))); open(); });
        SimpleItem pdDisp = displayInt("Period", display.getRepeatPeriodTicks(), Material.CLOCK, v -> { display.setRepeatPeriodTicks(Math.max(0, v)); open(); });
        SimpleItem pdInc = inc(click -> { display.setRepeatPeriodTicks(display.getRepeatPeriodTicks() + stepInt(click)); open(); });
        SimpleItem bkrItem = displayInt("BtwKf", display.getBetweenKfRepeat(), Material.HOPPER,
            v -> { display.setBetweenKfRepeat(Math.max(0, v)); open(); });

        Gui.Builder<?, ?> builder = Gui.normal()
            .setStructure(
                "B V I . . a c . D",
                "1 2 3 4 5 6 7 8 9",
                "q w e r t y u i o",
                "R + M P p m N . K",
                "z x c v b n , . .",
                "Z X C V B N < > .")
            .addIngredient('.', BORDER)
            .addIngredient('B', back)
            .addIngredient('V', save)
            .addIngredient('I', info)
            .addIngredient('a', editParticles)
            .addIngredient('c', saveToLibrary)
            .addIngredient('D', deleteDisplay)
            .addIngredient('1', offXDec).addIngredient('2', offXDisp).addIngredient('3', offXInc)
            .addIngredient('4', offYDec).addIngredient('5', offYDisp).addIngredient('6', offYInc)
            .addIngredient('7', offZDec).addIngredient('8', offZDisp).addIngredient('9', offZInc)
            .addIngredient('q', rndXDec).addIngredient('w', rndXDisp).addIngredient('e', rndXInc)
            .addIngredient('r', rndYDec).addIngredient('t', rndYDisp).addIngredient('y', rndYInc)
            .addIngredient('u', rndZDec).addIngredient('i', rndZDisp).addIngredient('o', rndZInc)
            .addIngredient('R', rcDec).addIngredient('+', rcDisp).addIngredient('M', rcInc)
            .addIngredient('P', pdDec).addIngredient('p', pdDisp).addIngredient('m', pdInc)
            .addIngredient('N', anchor).addIngredient('K', bkrItem);

        addShapeRows(builder, display, back, save);

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle(display.shapeTypeLabel() + " Display — kf" + kfIndex + " slot" + displayIndex)
            .setGui(builder.build())
            .build()
            .open();
    }

    private void addShapeRows(Gui.Builder<?, ?> builder, ParticleDisplay display,
                              SimpleItem back, SimpleItem save) {
        switch (display) {
            case LineDisplay ld -> {
                builder.addIngredient('z', anchorEndButton(ld));
                builder.addIngredient('x', scalarAnvil("End Off X", ld.getEndOffset().x,
                    Material.RED_DYE, v -> {
                        ld.getEndOffset().x = v;
                        open();
                    }));
                builder.addIngredient('c', scalarAnvil("End Off Y", ld.getEndOffset().y,
                    Material.LIME_DYE, v -> {
                        ld.getEndOffset().y = v;
                        open();
                    }));
                builder.addIngredient('v', scalarAnvil("End Off Z", ld.getEndOffset().z,
                    Material.LIGHT_BLUE_DYE, v -> {
                        ld.getEndOffset().z = v;
                        open();
                    }));
                builder.addIngredient('b', scalarAnvil("End Rnd X", ld.getEndRandomRange().x,
                    Material.PINK_DYE, v -> {
                        ld.getEndRandomRange().x = Math.max(0f, v);
                        open();
                    }));
                builder.addIngredient('n', scalarAnvil("End Rnd Y", ld.getEndRandomRange().y,
                    Material.GREEN_DYE, v -> {
                        ld.getEndRandomRange().y = Math.max(0f, v);
                        open();
                    }));
                builder.addIngredient(',', scalarAnvil("End Rnd Z", ld.getEndRandomRange().z,
                    Material.CYAN_DYE, v -> {
                        ld.getEndRandomRange().z = Math.max(0f, v);
                        open();
                    }));
                builder.addIngredient('Z', dec(click -> {
                    ld.setSpacing(Math.max(0.05, ld.getSpacing() - stepFloat(click)));
                    open();
                }));
                builder.addIngredient('X', displayFloatD("Spacing", ld.getSpacing(), Material.STRING, v -> {
                    ld.setSpacing(Math.max(0.05, v));
                    open();
                }));
                builder.addIngredient('C', inc(click -> {
                    ld.setSpacing(ld.getSpacing() + stepFloat(click));
                    open();
                }));
                builder.addIngredient('V', BORDER);
                builder.addIngredient('B', back);
                builder.addIngredient('N', save);
                builder.addIngredient('<', back);
                builder.addIngredient('>', save);
            }
            case SphereDisplay sd -> {
                builder.addIngredient('z', dec(click -> {
                    sd.setRadius(Math.max(0.0, sd.getRadius() - stepFloat(click)));
                    open();
                }));
                builder.addIngredient('x', displayFloatD("Radius", sd.getRadius(), Material.SLIME_BALL, v -> {
                    sd.setRadius(Math.max(0.0, v));
                    open();
                }));
                builder.addIngredient('c', inc(click -> {
                    sd.setRadius(sd.getRadius() + stepFloat(click));
                    open();
                }));
                builder.addIngredient('v', dec(click -> {
                    sd.setDensity(Math.max(1, sd.getDensity() - stepInt(click)));
                    open();
                }));
                builder.addIngredient('b', displayInt("Density", sd.getDensity(), Material.GUNPOWDER, v -> {
                    sd.setDensity(Math.max(1, v));
                    open();
                }));
                builder.addIngredient('n', inc(click -> {
                    sd.setDensity(sd.getDensity() + stepInt(click));
                    open();
                }));
                builder.addIngredient(',', toggleItem("Filled", sd.isFilled(), click -> {
                    sd.setFilled(!sd.isFilled());
                    open();
                }));
                builder.addIngredient('Z', BORDER).addIngredient('X', BORDER).addIngredient('C', BORDER);
                builder.addIngredient('V', back).addIngredient('B', save).addIngredient('N', BORDER);
                builder.addIngredient('<', back).addIngredient('>', save);
            }
            case CircleDisplay cd -> {
                builder.addIngredient('z', dec(click -> {
                    cd.setOuterRadius(Math.max(0.0, cd.getOuterRadius() - stepFloat(click)));
                    open();
                }));
                builder.addIngredient('x', displayFloatD("Outer", cd.getOuterRadius(), Material.GOLDEN_HOE, v -> {
                    cd.setOuterRadius(Math.max(0.0, v));
                    open();
                }));
                builder.addIngredient('c', inc(click -> {
                    cd.setOuterRadius(cd.getOuterRadius() + stepFloat(click));
                    open();
                }));
                builder.addIngredient('v', dec(click -> {
                    cd.setInnerRadius(Math.max(0.0, cd.getInnerRadius() - stepFloat(click)));
                    open();
                }));
                builder.addIngredient('b', displayFloatD("Inner", cd.getInnerRadius(), Material.IRON_HOE, v -> {
                    cd.setInnerRadius(Math.max(0.0, Math.min(v, cd.getOuterRadius())));
                    open();
                }));
                builder.addIngredient('n', inc(click -> {
                    cd.setInnerRadius(Math.min(cd.getOuterRadius(), cd.getInnerRadius() + stepFloat(click)));
                    open();
                }));
                builder.addIngredient(',', scalarAnvilD("Space Rad", cd.getSpacingRadial(), Material.COMPASS, v -> {
                    cd.setSpacingRadial(Math.max(0.01, v));
                    open();
                }));
                builder.addIngredient('Z', scalarAnvilD("Space Arc", cd.getSpacingArc(), Material.RECOVERY_COMPASS, v -> {
                    cd.setSpacingArc(Math.max(0.01, v));
                    open();
                }));
                builder.addIngredient('X', cycleNormal(cd));
                builder.addIngredient('C', BORDER).addIngredient('V', save).addIngredient('B', back);
                builder.addIngredient('N', BORDER).addIngredient('<', back).addIngredient('>', save);
            }
            case null, default -> {
                builder.addIngredient('z', BORDER).addIngredient('x', BORDER).addIngredient('c', BORDER);
                builder.addIngredient('v', BORDER).addIngredient('b', BORDER).addIngredient('n', BORDER);
                builder.addIngredient(',', BORDER);
                builder.addIngredient('Z', BORDER).addIngredient('X', BORDER).addIngredient('C', BORDER);
                builder.addIngredient('V', save).addIngredient('B', back).addIngredient('N', BORDER);
                builder.addIngredient('<', back).addIngredient('>', save);
            }
        }
    }

    private SimpleItem anchorButton(ParticleDisplay display) {
        return new SimpleItem(
            new ItemStackBuilder(Material.COMPASS)
                .name(Component.text("Anchor: ", NamedTextColor.GRAY)
                    .append(Component.text(describeAnchor(display.getAnchor()), NamedTextColor.AQUA, TextDecoration.BOLD)))
                .lore(List.of(
                    Component.text("Click or swap-click to pick a new anchor.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openAnchorPicker(a -> {
                display.setAnchor(a);
                open();
            })
        );
    }

    private SimpleItem anchorEndButton(LineDisplay ld) {
        return new SimpleItem(
            new ItemStackBuilder(Material.SPYGLASS)
                .name(Component.text("End Anchor: ", NamedTextColor.GRAY)
                    .append(Component.text(describeAnchor(ld.getEndAnchor()), NamedTextColor.AQUA, TextDecoration.BOLD)))
                .lore(List.of(
                    Component.text("Click or swap-click to pick the line end anchor.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openAnchorPicker(a -> {
                ld.setEndAnchor(a);
                open();
            })
        );
    }

    private void openAnchorPicker(Consumer<OriginAnchor> onPick) {
        new OriginAnchorPickerMenu(swordPlayer, onPick, this::open).open();
    }

    private SimpleItem cycleNormal(CircleDisplay cd) {
        return new SimpleItem(
            new ItemStackBuilder(Material.ENDER_EYE)
                .name(Component.text("Normal: ", NamedTextColor.GRAY)
                    .append(Component.text(cd.getNormal().name(), NamedTextColor.AQUA, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to cycle.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> {
                CircleDisplay.Normal[] values = CircleDisplay.Normal.values();
                cd.setNormal(values[(cd.getNormal().ordinal() + 1) % values.length]);
                open();
            }
        );
    }

    private SimpleItem toggleItem(String label, boolean value, Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(value ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(value ? "ON" : "OFF",
                        value ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)))
                .build(),
            handler
        );
    }

    private ParticleDisplay fetch(AttackDevSession session) {
        if (session.getEditKeyframes() == null || kfIndex < 0 || kfIndex >= session.getEditKeyframes().size()) return null;
        var effect = session.getEditKeyframes().get(kfIndex).effect();
        if (effect == null || effect.displays() == null) return null;
        if (displayIndex < 0 || displayIndex >= effect.displays().size()) return null;
        return effect.displays().get(displayIndex);
    }

    private static String describeAnchor(OriginAnchor a) {
        return switch (a) {
            case OriginAnchor.OwningKeyframe ignored -> "Owning";
            case OriginAnchor.KeyframeIndex ki -> "KF #" + ki.index();
            case OriginAnchor.EntityBodyPoint ebp -> "Body " + ebp.point().name();
            case OriginAnchor.FireLockedOrigin ignored -> "Locked";
            case OriginAnchor.RaycastOrigin ignored -> "Ray Origin";
            case OriginAnchor.NextKeyframe nk -> "Next KF +" + nk.offset();
        };
    }

    // ── Shared item factories ────────────────────────────────────────────────

    private static SimpleItem dec(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("−", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("L small R medium Shift+L large", NamedTextColor.DARK_GRAY)))
                .build(),
            handler);
    }

    private static SimpleItem inc(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("L small R medium Shift+L large", NamedTextColor.DARK_GRAY)))
                .build(),
            handler);
    }

    private SimpleItem displayFloat(String label, float value, Material mat, Consumer<Float> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.2f", value), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openFloatAnvil(label, value, onInput, this::open));
    }

    private SimpleItem displayFloatD(String label, double value, Material mat, Consumer<Double> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%.3f", value), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openDoubleAnvil(label, value, onInput, this::open));
    }

    private SimpleItem displayInt(String label, int value, Material mat, Consumer<Integer> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(value), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openIntAnvil(label, value, onInput, this::open));
    }

    private SimpleItem scalarAnvil(String label, float value, Material mat, Consumer<Float> onInput) {
        return displayFloat(label, value, mat, onInput);
    }

    private SimpleItem scalarAnvilD(String label, double value, Material mat, Consumer<Double> onInput) {
        return displayFloatD(label, value, mat, onInput);
    }

    private static int stepInt(Click click) {
        return switch (click.getClickType()) {
            case RIGHT -> 5;
            case SHIFT_LEFT -> 10;
            default -> 1;
        };
    }

    private static float stepFloat(Click click) {
        return switch (click.getClickType()) {
            case RIGHT -> 0.25f;
            case SHIFT_LEFT -> 1.0f;
            default -> 0.05f;
        };
    }

    /** Factory for building a fresh display of the requested shape. */
    public static ParticleDisplay createDefault(String shape) {
        return switch (shape.toUpperCase()) {
            case "LINE" -> LineDisplay.defaults();
            case "SPHERE" -> SphereDisplay.defaults();
            case "CIRCLE" -> CircleDisplay.defaults();
            default -> PointDisplay.defaults();
        };
    }
}
