package btm.sword;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import btm.sword.commands.SwordCommands;
import btm.sword.config.ConfigManager;
import btm.sword.listeners.EntityListener;
import btm.sword.listeners.InputListener;
import btm.sword.listeners.PlayerListener;
import btm.sword.system.action.utility.thrown.InteractiveItemArbiter;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.playerdata.PlayerDataManager;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import xyz.xenondevs.invui.InvUI;

public final class Sword extends JavaPlugin {
    @Getter
    private static Sword instance;
    @Getter
    private static ScheduledExecutorService scheduler;

    @Override
    public void onEnable() {
        instance = this;
        scheduler = Executors.newSingleThreadScheduledExecutor();

        // Initialize configuration system (must be first for other systems to use it)
        ConfigManager.initialize(this);

        InvUI.getInstance().setPlugin(this);

        getServer().getPluginManager().registerEvents(new InputListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new EntityListener(), this);

        // Register commands using Paper's Brigadier lifecycle system
        LifecycleEventManager<@NotNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            SwordCommands.register(event.registrar());
        });

        // Catch and register all entities whose data is already cached by the server
        SwordEntityArbiter.registerAllExistingEntities();

        PlayerDataManager.initialize();

        getLogger().info("~ Sword: Combat Evolved has been enabled ~");
    }

    @Override
    public void onDisable() {
        // Clean up all entity displays (sheathed weapons, status displays)
        SwordEntityArbiter.removeAllDisplays();

        // Clean up all active thrown item displays
        InteractiveItemArbiter.cleanupAll();

        // TODO: Uncomment when persistent data is ready
        // PlayerDataManager.shutdown();

        getLogger().info("~ Sword: Combat Evolved has been disabled ~");
    }



    public static void print(String str) {
        instance.getLogger().info(str);
    }
}
