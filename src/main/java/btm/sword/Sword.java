package btm.sword;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import btm.sword.commands.SwordCommands;
import btm.sword.config.ConfigManager;
import btm.sword.listeners.EntityListener;
import btm.sword.listeners.InputListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.listeners.SystemListener;
import btm.sword.listeners.WorldListener;
import btm.sword.listeners.packet.MovementListener;
import btm.sword.system.action.throwing.InteractiveItemArbiter;
import btm.sword.system.action.throwing.ProjectileManager;
import btm.sword.system.display.BossBarManager;
import btm.sword.system.display.ScoreboardManager;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.inventory.InventoryMenuManager;
import btm.sword.system.join.MenuSlotGrid;
import btm.sword.system.join.ServerJoinArbiter;
import btm.sword.system.playerdata.PlayerDataManager;
import btm.sword.system.scene.FakePlayerManager;
import btm.sword.system.scene.animation.AnimationRegistry;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import xyz.xenondevs.invui.InvUI;

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

        // Initialize configuration system (must be first for other systems to use it)
        ConfigManager.initialize(this);
        // load in values from the config file to override the defaults from the Config class
        ConfigManager.getInstance().loadConfig();

        InvUI.getInstance().setPlugin(this);

        getServer().getPluginManager().registerEvents(new InputListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ServerJoinArbiter(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);
        getServer().getPluginManager().registerEvents(new WorldListener(), this);
        getServer().getPluginManager().registerEvents(new SystemListener(), this);

        protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new MovementListener());

        // Register commands using Paper's Brigadier lifecycle system
        LifecycleEventManager<@NotNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            SwordCommands.register(event.registrar());
        });

        // Catch and register all entities whose data is already cached by the server
        SwordEntityArbiter.registerAllExistingEntities();

        // Start the standalone projectile tick loop (does not affect FSM-driven projectiles)
        ProjectileManager.startTicking();

        InventoryMenuManager.registerAll();

        AnimationRegistry.initialize(this);

        PlayerDataManager.initialize();

        // Place the staging grid platforms (worlds are loaded before onEnable on Paper)
        MenuSlotGrid.placeAllBlocks();

        getLogger().info("~ Sword: Combat Evolved has been enabled ~");
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

        PlayerDataManager.shutdown();

        getLogger().info("~ Sword: Combat Evolved has been disabled ~");
    }



    public static void print(String str) {
        instance.getLogger().info(str);
    }
}
