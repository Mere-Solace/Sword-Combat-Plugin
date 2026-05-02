package btm.sword;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import btm.sword.action.throwing.InteractiveItemArbiter;
import btm.sword.action.throwing.ProjectileManager;
import btm.sword.combat.def.AttackRegistry;
import btm.sword.combat.def.ParticleDisplayLibrary;
import btm.sword.combat.hit.HitPacketRegistry;
import btm.sword.combat.simulation.CollisionEventBridge;
import btm.sword.combat.simulation.VolumeSimulation;
import btm.sword.command.SwordCommands;
import btm.sword.config.ConfigManager;
import btm.sword.entity.arbiter.SwordEntityArbiter;
import btm.sword.entity.display.DEUAnimationHook;
import btm.sword.entity.display.WeaponAnchorPacketHook;
import btm.sword.entity.display.WeaponDisplayRegistry;
import btm.sword.entity.mob.MobTypeRegistry;
import btm.sword.entity.player.DevSwordPlayer;
import btm.sword.event.listener.CustomInteractionListener;
import btm.sword.event.listener.EntityListener;
import btm.sword.event.listener.ErrorListener;
import btm.sword.event.listener.PlayerListener;
import btm.sword.event.listener.SystemListener;
import btm.sword.event.listener.WorldListener;
import btm.sword.event.packet.EntityPacketListener;
import btm.sword.event.packet.MovementListener;
import btm.sword.input.InputListener;
import btm.sword.input.ItemInputDispatchTable;
import btm.sword.input.binding.InputBindingsRegistrar;
import btm.sword.interaction.CustomInteractionManager;
import btm.sword.join.MenuSlotGrid;
import btm.sword.join.ServerJoinArbiter;
import btm.sword.menu.InventoryMenuManager;
import btm.sword.playerdata.PlayerDataManager;
import btm.sword.runtime.scheduler.TimeArbiter;
import btm.sword.scene.animation.AnimationRegistry;
import btm.sword.scene.fake.FakePlayerManager;
import btm.sword.ui.bossbar.BossBarManager;
import btm.sword.ui.scoreboard.ScoreboardManager;
import btm.sword.util.misc.Debug;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import xyz.xenondevs.invui.InvUI;

/**
 * Root plugin class for <b>Sword: Combat Evolved</b>.
 * <p>
 * Bootstraps all subsystems in {@link #onEnable()} (config, entity arbiter, listeners,
 * commands, player-data, inventory menus, display rigs, join sequencing) and tears them
 * down cleanly in {@link #onDisable()}. Exposes a static {@link #getInstance()} accessor
 * and a thin {@link #print(String)} logging helper used throughout the codebase.
 * </p>
 */
public final class Sword extends JavaPlugin {
    @Getter
    private static Sword instance;
    @Getter
    private static ScheduledExecutorService scheduler;
    @Getter
    private static ProtocolManager protocolManager;

    @Override
    public void onEnable() {
        instance = this;
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Load dev player names from the bundled devnames.txt resource (gitignored, baked into JAR)
        DevSwordPlayer.loadDevNames(this);

        // Initialize configuration system (must be first for other systems to use it)
        ConfigManager.initialize(this);
        // load in values from the config file to override the defaults from the Config class
        ConfigManager.getInstance().loadConfig();
        Debug.init(getDataFolder());

        InvUI.getInstance().setPlugin(this);

        getServer().getPluginManager().registerEvents(new ErrorListener(), this);
        getServer().getPluginManager().registerEvents(new InputListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new CustomInteractionListener(), this);
        getServer().getPluginManager().registerEvents(new ServerJoinArbiter(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new DEUAnimationHook(), this);
        WeaponAnchorPacketHook.register();
        getServer().getPluginManager().registerEvents(new WorldListener(), this);
        getServer().getPluginManager().registerEvents(new SystemListener(), this);

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new MovementListener());
        protocolManager.addPacketListener(new EntityPacketListener());

        // Register commands using Paper's Brigadier lifecycle system
        LifecycleEventManager<@NotNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            SwordCommands.register(event.registrar());
        });

        // Catch and register all entities whose data is already cached by the server
        SwordEntityArbiter.registerAllExistingEntities();

        TimeArbiter.beginAll();

        VolumeSimulation.INSTANCE.start();

        // Load any saved attack definitions from plugins/sword/attacks/
        AttackRegistry.loadDirectory(new File(getDataFolder(), "attacks"));

        // Load hit-packet presets from plugins/sword/hit-packets.yaml
        HitPacketRegistry.bootstrap(this);

        // Load particle display library presets from plugins/sword/particles.yml
        ParticleDisplayLibrary.load(new File(getDataFolder(), "particles.yml"));

        // Drain simulation collision events to the main thread every tick
        TimeArbiter.runTimeIndependentBukkitTaskOnTimer(
            null,
            CollisionEventBridge.INSTANCE::drainToMain,
            0, 50,
            Sword.class, "collisionEventDrain"
        );

        // Start the standalone projectile tick loop (does not affect FSM-driven projectiles)
        ProjectileManager.startTicking();

        InventoryMenuManager.registerAll();
        CustomInteractionManager.initialize();

        // Register all per-item input bindings (menu button, storage buttons, ability slots).
        InputBindingsRegistrar.registerAll();

        AnimationRegistry.initialize(this);
        MobTypeRegistry.initialize(this);
        WeaponDisplayRegistry.initialize(this);

        PlayerDataManager.initialize();

        // Place the staging grid platforms (worlds are loaded before onEnable on Paper)
        MenuSlotGrid.placeAllBlocks();

        applyDevSetupIfFirstRun();

        getLogger().info("~ Sword: Combat Evolved has been enabled ~");
    }

    /**
     * On first plugin start, applies developer-friendly gamerules to all loaded worlds:
     * {@code keepInventory=true} and {@code doMobLoot=false}. Writes a marker file so this
     * only runs once per server instance and is not reapplied on every reload.
     */
    private void applyDevSetupIfFirstRun() {
        File marker = new File(getDataFolder(), "dev-setup.lock");
        if (marker.exists()) return;

        for (World world : getServer().getWorlds()) {
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.DO_MOB_LOOT, false);
        }

        getDataFolder().mkdirs();
        try {
            marker.createNewFile();
        } catch (IOException e) {
            getLogger().warning("Could not create dev-setup.lock: " + e.getMessage());
        }

        getLogger().info("Dev setup applied: keepInventory=true, doMobLoot=false");
    }

    @Override
    public void onDisable() {
        // Clean up all active scoreboards and boss bars
        ScoreboardManager.removeAll();
        BossBarManager.removeAll();

        // Clean up all entity displays (sheathed weapons, status displays)
        SwordEntityArbiter.removeAllDisplays();

        // Clean up all active thrown item displays
        InteractiveItemArbiter.cleanupAll();

        // Remove any lingering packet-based fake player NPCs
        FakePlayerManager.despawnAll();

        // Release all staging grid slots
        MenuSlotGrid.releaseAll();

        // Empty the per-item input dispatch table so a /reload re-registers cleanly.
        ItemInputDispatchTable.clear();

        PlayerDataManager.shutdown();
        VolumeSimulation.INSTANCE.shutdown();
        TimeArbiter.shutdown();

        getLogger().info("~ Sword: Combat Evolved has been disabled ~");
    }



    /** Logs an informational message through the plugin logger. */
    public static void print(String str) {
        instance.getLogger().info(str);
    }
}
