package btm.sword.system.scene.animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import btm.sword.Sword;

/**
 * Registry for all {@link AnimationDef} entries loaded from {@code animations.yml}.
 * <p>
 * Call {@link #initialize(JavaPlugin)} once in {@code Sword.onEnable()} after
 * {@code ConfigManager} is ready. Hot-reloads can call {@link #reload()} at any time.
 * The loop flag for each animation can be toggled at runtime via {@link #setLoop(String, boolean)},
 * which persists the change back to {@code animations.yml} immediately.
 * </p>
 *
 * <h2>YAML format</h2>
 * Animations are grouped under their DEU group section. Each group can contain multiple
 * animation entries. The section key is used as the group tag by default; the animation
 * subsection key is used as both the logical key and anim tag by default. Explicit
 * {@code group:} and {@code anim:} overrides are only needed when the tags differ from
 * their section keys.
 * <pre>
 * animations:
 *   slash_test:             # section key = group tag (override with group: foo)
 *     anims:
 *       slash_default:      # anim key = anim tag (override with anim: foo)
 *         loop: false
 *       slash_heavy:
 *         loop: false
 *   main_menu:
 *     group: main_menu_group   # explicit group tag when different from section key
 *     anims:
 *       intro:
 *         anim: mm_intro_anim  # explicit anim tag when different from anim key
 *         loop: false
 * </pre>
 *
 * <h2>Startup scan</h2>
 * After loading {@code animations.yml}, the registry scans
 * {@code plugins/DisplayEntityUtils/bdenginedatapacks/} for any {@code .zip} files
 * that have no matching {@code .deanim} file in {@code plugins/DisplayEntityUtils/animations/}.
 * For each unconverted pack it logs:
 * <pre>
 *   [Sword] Unconverted BDE pack: foo.zip — run:
 *     /deu bdengine convertdp foo &lt;group-tag&gt; &lt;anim-prefix&gt;
 * </pre>
 *
 * <h2>Dual-save</h2>
 * {@link #saveToDisk()} writes to the plugin data folder and, if the source resource file
 * {@code ../src/main/resources/animations.yml} exists (dev environment only), mirrors
 * the save there so changes survive a rebuild.
 */
public class AnimationRegistry {

    private static final String ANIMATIONS_FILE = "animations.yml";
    private static final File DEU_BASE = new File("plugins/DisplayEntityUtils");
    private static Map<String, AnimationDef> registry = Collections.emptyMap();
    private static JavaPlugin plugin;

    private AnimationRegistry() {}

    /**
     * Initialises the registry from {@code animations.yml}, copying the bundled default
     * if the file does not yet exist in the plugin data folder.
     *
     * @param javaPlugin the owning plugin instance
     */
    public static void initialize(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
        saveDefaultIfAbsent();
        reload();
    }

    /**
     * Re-parses {@code animations.yml} without restarting the server.
     * Safe to call at any time after {@link #initialize}.
     */
    public static void reload() {
        File file = new File(plugin.getDataFolder(), ANIMATIONS_FILE);
        if (!file.exists()) {
            Sword.getInstance().getLogger().warning("[AnimationRegistry] animations.yml not found — no animations loaded.");
            registry = Collections.emptyMap();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection animationsSection = yaml.getConfigurationSection("animations");

        Map<String, AnimationDef> loaded = new LinkedHashMap<>();
        if (animationsSection != null) {
            for (String groupSectionKey : animationsSection.getKeys(false)) {
                ConfigurationSection groupEntry = animationsSection.getConfigurationSection(groupSectionKey);
                if (groupEntry == null) continue;
                // "group:" defaults to the section key if omitted.
                String groupTag = groupEntry.getString("group", groupSectionKey);
                ConfigurationSection anims = groupEntry.getConfigurationSection("anims");
                if (anims == null) continue;
                for (String animKey : anims.getKeys(false)) {
                    ConfigurationSection animEntry = anims.getConfigurationSection(animKey);
                    boolean loop = animEntry != null && animEntry.getBoolean("loop", false);
                    // "anim:" defaults to the anim subsection key if omitted.
                    String animTag = animEntry != null ? animEntry.getString("anim", animKey) : animKey;
                    loaded.put(animKey, new AnimationDef(animKey, groupTag, animTag, loop));
                }
            }
        }

        registry = new LinkedHashMap<>(loaded);
        Sword.getInstance().getLogger().info("[AnimationRegistry] Loaded " + loaded.size() + " animation(s).");
        cullStaleEntries();
        scanUnconvertedPacks();
    }

    /**
     * Returns the {@link AnimationDef} for the given key, or empty if not found.
     *
     * @param key the animation key
     * @return an {@link Optional} containing the def, or empty
     */
    public static Optional<AnimationDef> get(String key) {
        return Optional.ofNullable(registry.get(key));
    }

    /**
     * Returns all registered animation definitions.
     *
     * @return unmodifiable collection of all {@link AnimationDef}s
     */
    public static Collection<AnimationDef> all() {
        return Collections.unmodifiableCollection(registry.values());
    }

    /**
     * Toggles the loop flag for the given animation key, updating the in-memory registry
     * and persisting the change to {@code animations.yml} immediately.
     *
     * @param key  the animation key to update
     * @param loop the new loop state
     */
    public static void setLoop(String key, boolean loop) {
        AnimationDef existing = registry.get(key);
        if (existing == null) return;
        registry.put(key, new AnimationDef(existing.key(), existing.groupTag(), existing.animTag(), loop));
        saveToDisk();
    }

    /**
     * Returns whether the given animation key is currently set to loop.
     * Returns {@code false} if the key is not registered.
     *
     * @param key the animation key
     * @return {@code true} if the animation loops
     */
    public static boolean isLooping(String key) {
        AnimationDef def = registry.get(key);
        return def != null && def.defaultLoop();
    }

    /**
     * Scans {@code plugins/DisplayEntityUtils/animations/} for all {@code .deanim} files and
     * registers any that are not already in the registry. The group tag is inferred by finding
     * the longest-matching {@code .deg} basename in {@code savedentities/} that is a prefix of
     * the animation tag; falls back to the animation tag itself when no match exists.
     *
     * <p>Stale entries (whose {@code .deg} or {@code .deanim} files no longer exist) are culled
     * first so the registry stays in sync with what is actually on disk.</p>
     *
     * @return the number of newly registered animations
     */
    public static int syncFromLocalStorage() {
        cullStaleEntries();

        File animDir = new File(DEU_BASE, "animations");
        File savedEntities = new File(DEU_BASE, "savedentities");

        if (!animDir.exists() || !animDir.isDirectory()) return 0;

        File[] deanimFiles = animDir.listFiles((dir, name) -> name.endsWith(".deanim"));
        if (deanimFiles == null || deanimFiles.length == 0) return 0;

        List<String> groupTags = new ArrayList<>();
        if (savedEntities.exists() && savedEntities.isDirectory()) {
            File[] degs = savedEntities.listFiles((dir, name) -> name.endsWith(".deg"));
            if (degs != null) {
                for (File deg : degs) {
                    groupTags.add(deg.getName().replace(".deg", ""));
                }
            }
        }

        int added = 0;
        for (File deanim : deanimFiles) {
            String animTag = deanim.getName().replace(".deanim", "");
            if (registry.containsKey(animTag)) continue;

            String inferredGroup = animTag;
            int best = 0;
            for (String group : groupTags) {
                if (animTag.startsWith(group) && group.length() > best) {
                    best = group.length();
                    inferredGroup = group;
                }
            }

            registry.put(animTag, new AnimationDef(animTag, inferredGroup, animTag, false));
            added++;
        }

        if (added > 0) {
            saveToDisk();
            Sword.getInstance().getLogger().info("[AnimationRegistry] Synced " + added + " new animation(s) from DEU local storage.");
        }
        return added;
    }

    /**
     * Returns the base names (without {@code .zip}) of all BDE datapacks that have not yet
     * been converted — i.e. those in {@code bdenginedatapacks/} with no matching {@code .deanim}
     * file in {@code plugins/DisplayEntityUtils/animations/}.
     *
     * @return list of unconverted base names; empty if none or the directory is missing
     */
    public static List<String> getUnconvertedZipBases() {
        File packDir = new File(DEU_BASE, "bdenginedatapacks");
        File animDir = new File(DEU_BASE, "animations");

        if (!packDir.exists() || !packDir.isDirectory()) return Collections.emptyList();

        File[] zips = packDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (zips == null) return Collections.emptyList();

        List<String> unconverted = new ArrayList<>();
        for (File zip : zips) {
            String base = zip.getName().replace(".zip", "");
            if (!hasDeanimForBase(animDir, base)) {
                unconverted.add(base);
            }
        }
        return unconverted;
    }

    /**
     * Scans {@code plugins/DisplayEntityUtils/animations/} for {@code .deanim} files whose
     * name starts with {@code base}, registers each as a new {@link AnimationDef}
     * (key = anim tag = file basename without extension, groupTag = {@code base}),
     * and persists to {@code animations.yml}. Skips any key that is already registered.
     *
     * @param base the zip base name (e.g. {@code "creep_test"})
     * @return list of newly registered animation keys; empty if none found or all already registered
     */
    public static List<String> registerFromAnimDir(String base) {
        File animDir = new File(DEU_BASE, "animations");
        if (!animDir.exists() || !animDir.isDirectory()) return Collections.emptyList();

        File[] files = animDir.listFiles((dir, name) -> name.startsWith(base) && name.endsWith(".deanim"));
        if (files == null || files.length == 0) return Collections.emptyList();

        List<String> registered = new ArrayList<>();
        for (File file : files) {
            String animTag = file.getName().replace(".deanim", "");
            if (registry.containsKey(animTag)) continue;
            registry.put(animTag, new AnimationDef(animTag, base, animTag, false));
            registered.add(animTag);
        }
        if (!registered.isEmpty()) {
            saveToDisk();
        }
        return registered;
    }

    /**
     * Adds a new animation entry to the in-memory registry and persists it to
     * {@code animations.yml}. If an entry with the same key already exists it is left unchanged.
     *
     * @param key      logical animation key
     * @param groupTag DEU group tag
     * @param animTag  DEU animation tag
     * @param loop     default loop flag
     */
    public static void registerAnimation(String key, String groupTag, String animTag, boolean loop) {
        if (registry.containsKey(key)) return;
        registry.put(key, new AnimationDef(key, groupTag, animTag, loop));
        saveToDisk();
    }

    // ------------------------------------------------------------------

    private static boolean hasDeanimForBase(File animDir, String base) {
        if (!animDir.exists() || !animDir.isDirectory()) return false;
        File[] files = animDir.listFiles((dir, name) -> name.startsWith(base) && name.endsWith(".deanim"));
        return files != null && files.length > 0;
    }

    private static void saveDefaultIfAbsent() {
        File file = new File(plugin.getDataFolder(), ANIMATIONS_FILE);
        if (file.exists()) return;
        try {
            InputStream in = plugin.getResource(ANIMATIONS_FILE);
            if (in == null) {
                Sword.getInstance().getLogger().warning("[AnimationRegistry] No bundled animations.yml found in jar.");
                return;
            }
            file.getParentFile().mkdirs();
            Files.copy(in, file.toPath());
        } catch (IOException e) {
            Sword.getInstance().getLogger().warning("[AnimationRegistry] Failed to save default animations.yml: " + e.getMessage());
        }
    }

    /**
     * Serialises the registry to the nested group/anims YAML format and writes to disk.
     * Groups are determined by {@link AnimationDef#groupTag()}. Within each group section,
     * the anim key is the logical {@link AnimationDef#key()}. The {@code group:} field is
     * omitted when it matches the section key; {@code anim:} is omitted when it matches
     * the anim subsection key.
     */
    private static void saveToDisk() {
        YamlConfiguration yaml = new YamlConfiguration();

        // Bucket defs by groupTag so each group writes a single section.
        Map<String, List<AnimationDef>> byGroup = new LinkedHashMap<>();
        for (AnimationDef def : registry.values()) {
            byGroup.computeIfAbsent(def.groupTag(), k -> new ArrayList<>()).add(def);
        }

        for (Map.Entry<String, List<AnimationDef>> entry : byGroup.entrySet()) {
            String groupSectionKey = entry.getKey(); // section key == groupTag
            // Omit "group:" when it equals the section key (the common case).
            // When section key is a custom alias, set it explicitly.
            for (AnimationDef def : entry.getValue()) {
                String animBase = "animations." + groupSectionKey + ".anims." + def.key();
                // Omit "anim:" when it equals the anim subsection key (the common case).
                if (!def.animTag().equals(def.key())) {
                    yaml.set(animBase + ".anim", def.animTag());
                }
                yaml.set(animBase + ".loop", def.defaultLoop());
            }
        }

        File runtimeFile = new File(plugin.getDataFolder(), ANIMATIONS_FILE);
        try {
            yaml.save(runtimeFile);
        } catch (IOException e) {
            Sword.getInstance().getLogger().warning("[AnimationRegistry] Failed to save animations.yml: " + e.getMessage());
        }

        // Mirror to source resources in the dev environment so changes survive a rebuild.
        File sourceFile = new File("../src/main/resources/" + ANIMATIONS_FILE);
        if (sourceFile.exists()) {
            try {
                yaml.save(sourceFile);
            } catch (IOException e) {
                Sword.getInstance().getLogger().warning("[AnimationRegistry] Failed to mirror animations.yml to source: " + e.getMessage());
            }
        }
    }

    private static void cullStaleEntries() {
        File savedEntities = new File(DEU_BASE, "savedentities");
        File animDir = new File(DEU_BASE, "animations");

        List<String> stale = new ArrayList<>();
        for (AnimationDef def : registry.values()) {
            File deg = new File(savedEntities, def.groupTag() + ".deg");
            File deanim = new File(animDir, def.animTag() + ".deanim");
            if (!deg.exists() || !deanim.exists()) {
                stale.add(def.key());
                Sword.getInstance().getLogger().info(
                    "[AnimationRegistry] Culling stale entry '" + def.key()
                        + "' (group=" + def.groupTag() + ", anim=" + def.animTag() + ")"
                        + (!deg.exists() ? " — missing .deg" : "")
                        + (!deanim.exists() ? " — missing .deanim" : ""));
            }
        }

        if (!stale.isEmpty()) {
            stale.forEach(registry::remove);
            saveToDisk();
        }
    }

    private static void scanUnconvertedPacks() {
        File packDir = new File(DEU_BASE, "bdenginedatapacks");
        File animDir = new File(DEU_BASE, "animations");

        if (!packDir.exists() || !packDir.isDirectory()) return;

        File[] zips = packDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (zips == null) return;

        for (File zip : zips) {
            String base = zip.getName().replace(".zip", "");
            if (!hasDeanimForBase(animDir, base)) {
                Sword.getInstance().getLogger().warning(
                    "[Sword] Unconverted BDE pack: " + zip.getName()
                        + " \u2014 run: /deu bdengine convertdp " + base + " <group-tag> <anim-prefix>"
                );
            }
        }
    }
}
