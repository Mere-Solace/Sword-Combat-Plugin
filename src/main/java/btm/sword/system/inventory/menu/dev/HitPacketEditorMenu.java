package btm.sword.system.inventory.menu.dev;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import btm.sword.Sword;
import btm.sword.system.attack.Blockability;
import btm.sword.system.attack.HitPacketPreset;
import btm.sword.system.attack.HitPacketRegistry;
import btm.sword.system.entity.impl.SwordPlayer;
import btm.sword.system.inventory.menu.Menu;
import btm.sword.system.item.ItemStackBuilder;
import btm.sword.utility.ChatInputCapture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import xyz.xenondevs.invui.gui.Gui;
import xyz.xenondevs.invui.item.Click;
import xyz.xenondevs.invui.item.impl.SimpleItem;
import xyz.xenondevs.invui.window.AnvilWindow;
import xyz.xenondevs.invui.window.Window;

/**
 * Dev-only editor for a single {@link HitPacketPreset}.
 *
 * <p>Each of the seven numeric/enum fields is exposed with a dec/display/inc triplet
 * (display opens an anvil dialog for direct numeric entry). Save writes the preset
 * into {@link HitPacketRegistry} and persists the file to disk.</p>
 *
 * <h2>Layout (6 rows × 9)</h2>
 * <pre>
 * Row 0: Back  .  Rename  .  Title  .  Delete  .  Save
 * Row 1: .  SD-  SD  SD+  .  TD-  TD  TD+  .
 * Row 2: .  SL-  SL  SL+  .  RS-  RS  RS+  .
 * Row 3: .  IT-  IT  IT+  .  BP-  BP  BP+  .
 * Row 4: .   .   .   .   B   .   .   .   .
 * Row 5: #   #   #   #   #   #   #   #   #
 * </pre>
 */
public class HitPacketEditorMenu extends Menu {

    private HitPacketPreset working;
    private final String originalId;

    /**
     * Opens the editor for the given preset.
     *
     * @param player  the player opening the menu
     * @param preset  the preset to edit (mutated via withers as the user clicks)
     */
    public HitPacketEditorMenu(SwordPlayer player, HitPacketPreset preset) {
        super(player);
        this.working = preset;
        this.originalId = preset.id();
    }

    @Override
    public void open() {
        SimpleItem back = new SimpleItem(
            new ItemStackBuilder(Material.COPPER_TRAPDOOR)
                .name(Component.text("Back", NamedTextColor.GRAY))
                .lore(List.of(Component.text("Discards unsaved changes.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> new HitPacketLibraryMenu(swordPlayer).open()
        );

        SimpleItem rename = new SimpleItem(
            new ItemStackBuilder(Material.NAME_TAG)
                .name(Component.text("Rename", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Current: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(working.displayName(), NamedTextColor.WHITE)),
                    Component.text("Click to change display name.", NamedTextColor.GRAY)
                ))
                .build(),
            click -> ChatInputCapture.prompt(
                swordPlayer.player(),
                Component.text("Type a new display name for '" + working.id() + "':",
                    NamedTextColor.YELLOW),
                text -> {
                    if (text.equalsIgnoreCase("cancel") || text.isEmpty()) {
                        open();
                        return;
                    }
                    String newId = uniqueId(HitPacketPreset.slugify(text), originalId);
                    if (newId.isEmpty()) {
                        swordPlayer.message(Component.text(
                            "[Dev] Name must contain at least one alphanumeric character.",
                            NamedTextColor.RED));
                        open();
                        return;
                    }
                    working = working.withDisplayName(text).withId(newId);
                    open();
                }
            )
        );

        SimpleItem title = new SimpleItem(
            new ItemStackBuilder(Material.PAPER)
                .name(Component.text(working.displayName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("id: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(working.id(), NamedTextColor.AQUA)),
                    Component.empty(),
                    Component.text("shard: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(working.shardDamage()), NamedTextColor.WHITE)),
                    Component.text("tough: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(fmt2(working.toughnessDamage()), NamedTextColor.WHITE)),
                    Component.text("soul loss: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(fmt2(working.soulfireLoss()), NamedTextColor.WHITE)),
                    Component.text("reaped: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(fmt2(working.reapedSoulfire()), NamedTextColor.WHITE)),
                    Component.text("invul ticks: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(String.valueOf(working.invulnerableTicks()), NamedTextColor.WHITE)),
                    Component.text("block: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(working.blockability().name(), NamedTextColor.WHITE)),
                    Component.text("bypass: ", NamedTextColor.DARK_GRAY)
                        .append(Component.text(fmt2(working.bypassPower()), NamedTextColor.WHITE))
                ))
                .build()
        );

        SimpleItem delete = new SimpleItem(
            new ItemStackBuilder(Material.BARRIER)
                .name(Component.text("Delete", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Removes this preset from the registry", NamedTextColor.DARK_GRAY),
                    Component.text("and writes the updated YAML.", NamedTextColor.DARK_GRAY),
                    Component.text("Shift+Left to confirm.", NamedTextColor.YELLOW)
                ))
                .build(),
            click -> {
                if (click.getClickType() != ClickType.SHIFT_LEFT) {
                    swordPlayer.message(Component.text(
                        "Shift+Left-click Delete to confirm.", NamedTextColor.YELLOW));
                    return;
                }
                HitPacketRegistry.remove(working.id());
                HitPacketRegistry.saveAll();
                swordPlayer.message(Component.text(
                    "[Dev] Deleted preset '" + working.id() + "'.", NamedTextColor.RED));
                new HitPacketLibraryMenu(swordPlayer).open();
            }
        );

        SimpleItem save = new SimpleItem(
            new ItemStackBuilder(Material.WRITABLE_BOOK)
                .name(Component.text("Save", NamedTextColor.AQUA, TextDecoration.BOLD))
                .lore(List.of(
                    Component.text("Registers this preset and", NamedTextColor.DARK_GRAY),
                    Component.text("writes hit-packets.yaml.", NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                if (!working.id().equals(originalId)) {
                    HitPacketRegistry.remove(originalId);
                }
                HitPacketRegistry.register(working);
                HitPacketRegistry.saveAll();
                swordPlayer.message(Component.text(
                    "[Dev] Saved preset '" + working.id() + "'.", NamedTextColor.GREEN));
                new HitPacketLibraryMenu(swordPlayer).open();
            }
        );

        // Field rows
        SimpleItem shardDec = dec(click -> { working = working.withShardDamage(
            Math.max(0, working.shardDamage() - stepInt(click, 1, 5, 10))); open(); });
        SimpleItem shardDisplay = displayInt("Shard Damage", working.shardDamage(), Material.IRON_SWORD,
            v -> { working = working.withShardDamage(Math.max(0, v)); open(); });
        SimpleItem shardInc = inc(click -> { working = working.withShardDamage(
            working.shardDamage() + stepInt(click, 1, 5, 10)); open(); });

        SimpleItem toughDec = dec(click -> { working = working.withToughnessDamage(
            Math.max(0f, working.toughnessDamage() - stepFloat(click, 0.25f, 1.0f, 5.0f))); open(); });
        SimpleItem toughDisplay = displayFloat("Toughness Damage", working.toughnessDamage(), Material.IRON_CHESTPLATE,
            v -> { working = working.withToughnessDamage(Math.max(0f, v)); open(); });
        SimpleItem toughInc = inc(click -> { working = working.withToughnessDamage(
            working.toughnessDamage() + stepFloat(click, 0.25f, 1.0f, 5.0f)); open(); });

        SimpleItem soulLossDec = dec(click -> { working = working.withSoulfireLoss(
            Math.max(0f, working.soulfireLoss() - stepFloat(click, 0.1f, 0.5f, 2.0f))); open(); });
        SimpleItem soulLossDisplay = displayFloat("Soulfire Loss", working.soulfireLoss(), Material.SOUL_LANTERN,
            v -> { working = working.withSoulfireLoss(Math.max(0f, v)); open(); });
        SimpleItem soulLossInc = inc(click -> { working = working.withSoulfireLoss(
            working.soulfireLoss() + stepFloat(click, 0.1f, 0.5f, 2.0f)); open(); });

        SimpleItem reapedDec = dec(click -> { working = working.withReapedSoulfire(
            Math.max(0f, working.reapedSoulfire() - stepFloat(click, 0.1f, 0.5f, 2.0f))); open(); });
        SimpleItem reapedDisplay = displayFloat("Reaped Soulfire", working.reapedSoulfire(), Material.SOUL_TORCH,
            v -> { working = working.withReapedSoulfire(Math.max(0f, v)); open(); });
        SimpleItem reapedInc = inc(click -> { working = working.withReapedSoulfire(
            working.reapedSoulfire() + stepFloat(click, 0.1f, 0.5f, 2.0f)); open(); });

        SimpleItem invulDec = dec(click -> { working = working.withInvulnerableTicks(
            Math.max(0, working.invulnerableTicks() - stepInt(click, 1, 5, 10))); open(); });
        SimpleItem invulDisplay = displayInt("Invulnerable Ticks", working.invulnerableTicks(), Material.CLOCK,
            v -> { working = working.withInvulnerableTicks(Math.max(0, v)); open(); });
        SimpleItem invulInc = inc(click -> { working = working.withInvulnerableTicks(
            working.invulnerableTicks() + stepInt(click, 1, 5, 10)); open(); });

        SimpleItem bypassDec = dec(click -> { working = working.withBypassPower(
            clamp01(working.bypassPower() - stepFloat(click, 0.05f, 0.1f, 0.25f))); open(); });
        SimpleItem bypassDisplay = displayFloat("Bypass Power", working.bypassPower(), Material.PIGLIN_BANNER_PATTERN,
            v -> { working = working.withBypassPower(clamp01(v)); open(); });
        SimpleItem bypassInc = inc(click -> { working = working.withBypassPower(
            clamp01(working.bypassPower() + stepFloat(click, 0.05f, 0.1f, 0.25f))); open(); });

        // Blockability cycle
        Blockability block = working.blockability();
        SimpleItem blockButton = new SimpleItem(
            new ItemStackBuilder(block == Blockability.BLOCKABLE ? Material.SHIELD : Material.NETHERITE_SWORD)
                .name(Component.text("Blockability: ", NamedTextColor.GRAY)
                    .append(Component.text(block.name(),
                        block == Blockability.BLOCKABLE ? NamedTextColor.AQUA : NamedTextColor.RED,
                        TextDecoration.BOLD)))
                .lore(List.of(
                    Component.text("Click to cycle.", NamedTextColor.GRAY),
                    Component.text("SHIELD_PASSING applies bypass-power to shielded hits.",
                        NamedTextColor.DARK_GRAY)
                ))
                .build(),
            click -> {
                Blockability[] values = Blockability.values();
                Blockability next = values[(block.ordinal() + 1) % values.length];
                working = working.withBlockability(next);
                open();
            }
        );

        Gui gui = Gui.normal()
            .setStructure(
                "P . R . T . X . S",
                ". a b c . d e f .",
                ". g h i . j k l .",
                ". m n o . p q r .",
                ". . . . V . . . .",
                "# # # # # # # # #")
            .addIngredient('#', BORDER)
            .addIngredient('P', back)
            .addIngredient('R', rename)
            .addIngredient('T', title)
            .addIngredient('X', delete)
            .addIngredient('S', save)
            .addIngredient('a', shardDec).addIngredient('b', shardDisplay).addIngredient('c', shardInc)
            .addIngredient('d', toughDec).addIngredient('e', toughDisplay).addIngredient('f', toughInc)
            .addIngredient('g', soulLossDec).addIngredient('h', soulLossDisplay).addIngredient('i', soulLossInc)
            .addIngredient('j', reapedDec).addIngredient('k', reapedDisplay).addIngredient('l', reapedInc)
            .addIngredient('m', invulDec).addIngredient('n', invulDisplay).addIngredient('o', invulInc)
            .addIngredient('p', bypassDec).addIngredient('q', bypassDisplay).addIngredient('r', bypassInc)
            .addIngredient('V', blockButton)
            .build();

        Window.single()
            .setViewer(swordPlayer.player())
            .setTitle("Hit Packet — " + working.id())
            .setGui(gui)
            .build()
            .open();
    }

    // ── Shared item factories ────────────────────────────────────────────────

    private static SimpleItem dec(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.RED_DYE)
                .name(Component.text("−", NamedTextColor.RED, TextDecoration.BOLD))
                .lore(List.of(Component.text("L: small  R: medium  Shift+L: large", NamedTextColor.DARK_GRAY)))
                .build(),
            handler
        );
    }

    private static SimpleItem inc(Consumer<Click> handler) {
        return new SimpleItem(
            new ItemStackBuilder(Material.LIME_DYE)
                .name(Component.text("+", NamedTextColor.GREEN, TextDecoration.BOLD))
                .lore(List.of(Component.text("L: small  R: medium  Shift+L: large", NamedTextColor.DARK_GRAY)))
                .build(),
            handler
        );
    }

    private SimpleItem displayFloat(String label, float value, Material mat, Consumer<Float> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(fmt2(value), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openFloatAnvil(label, fmt2(value), onInput)
        );
    }

    private SimpleItem displayInt(String label, int value, Material mat, Consumer<Integer> onInput) {
        return new SimpleItem(
            new ItemStackBuilder(mat)
                .name(Component.text(label + ": ", NamedTextColor.GRAY)
                    .append(Component.text(String.valueOf(value), NamedTextColor.GOLD, TextDecoration.BOLD)))
                .lore(List.of(Component.text("Click to type a value.", NamedTextColor.DARK_GRAY)))
                .build(),
            click -> openIntAnvil(label, String.valueOf(value), onInput)
        );
    }

    // ── Anvil input ──────────────────────────────────────────────────────────

    private void openFloatAnvil(String label, String currentVal, Consumer<Float> onInput) {
        openAnvil(label, currentVal, text -> {
            try {
                onInput.accept(Float.parseFloat(text.trim()));
            } catch (NumberFormatException ignored) { }
        });
    }

    private void openIntAnvil(String label, String currentVal, Consumer<Integer> onInput) {
        openAnvil(label, currentVal, text -> {
            try {
                onInput.accept(Integer.parseInt(text.trim()));
            } catch (NumberFormatException ignored) { }
        });
    }

    private void openAnvil(String label, String currentVal, Consumer<String> handler) {
        ItemStack inputItem = new ItemStackBuilder(Material.PAPER)
            .name(Component.text(currentVal, NamedTextColor.WHITE))
            .build();
        Gui gui = Gui.normal()
            .setStructure("X # #")
            .addIngredient('X', new SimpleItem(inputItem))
            .addIngredient('#', BORDER)
            .build();
        AnvilWindow.single()
            .setViewer(swordPlayer.player())
            .setTitle(label)
            .setGui(gui)
            .addRenameHandler(text -> {
                handler.accept(text);
                Bukkit.getScheduler().runTask(Sword.getInstance(), this::open);
            })
            .build()
            .open();
    }

    // ── Step helpers ─────────────────────────────────────────────────────────

    private static int stepInt(Click click, int small, int medium, int large) {
        return switch (click.getClickType()) {
            case RIGHT -> medium;
            case SHIFT_LEFT -> large;
            default -> small;
        };
    }

    private static float stepFloat(Click click, float small, float medium, float large) {
        return switch (click.getClickType()) {
            case RIGHT -> medium;
            case SHIFT_LEFT -> large;
            default -> small;
        };
    }

    /**
     * Returns a unique id based on {@code base}. If the base is already taken by a preset
     * other than {@code allowedId}, appends {@code _2}, {@code _3}, … until a free key is
     * found.
     */
    static String uniqueId(String base, String allowedId) {
        if (base.isEmpty()) return "";
        if (base.equals(allowedId)) return base;
        if (HitPacketRegistry.get(base) == null) return base;
        int n = 2;
        while (true) {
            String candidate = base + "_" + n;
            if (candidate.equals(allowedId) || HitPacketRegistry.get(candidate) == null) {
                return candidate;
            }
            n++;
        }
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, Math.round(v * 100f) / 100f));
    }

    private static String fmt2(float v) {
        return String.format("%.2f", v);
    }
}
