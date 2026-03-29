package btm.sword.gamemode.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;

import btm.sword.config.Config;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.impl.SwordPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * A wave-based roguelike run for one or more players.
 * <p>
 * Players survive {@value #TOTAL_WAVES} escalating waves of enemies. After the final wave
 * is cleared a {@link TextDisplay} reward chest marker appears at the spawn centre.
 * The run ends either when all waves are cleared or when the 5-minute safety cap
 * ({@value #MAX_DURATION_SECONDS}s) expires.
 * </p>
 *
 * <p>Spawn centre and per-wave enemy counts are configured via {@link Config.Roguelike}
 * and are hot-reloadable with {@code /sword reload}.</p>
 */
public class RoguelikeRun extends Gamemode {

    private static final int TOTAL_WAVES = 3;
    private static final int MAX_DURATION_SECONDS = 300; // 5-minute safety cap

    private int currentWave = 0;
    private boolean runComplete = false;
    private final List<LivingEntity> currentWaveEnemies = new ArrayList<>();
    private final Random random = new Random();

    /**
     * Creates a new roguelike run for the given players.
     *
     * @param players players participating in this run
     */
    public RoguelikeRun(List<SwordPlayer> players) {
        super(players);
        this.durationSeconds = new AtomicInteger(MAX_DURATION_SECONDS);
    }

    @Override
    protected int getMaxDuration() {
        return MAX_DURATION_SECONDS;
    }

    @Override
    protected String getTitle() {
        if (runComplete) {
            return "Roguelike - Complete!";
        }
        return "Roguelike - Wave " + currentWave + "/" + TOTAL_WAVES;
    }

    @Override
    protected void onStart() {
        Location spawnLoc = getSpawnCenter();
        for (SwordPlayer sp : players) {
            sp.player().teleport(spawnLoc);
        }
        spawnNextWave();
    }

    @Override
    protected void onTick(int secondsLeft) {
        if (currentWaveEnemies.isEmpty()) {
            return;
        }
        currentWaveEnemies.removeIf(e -> e.isDead() || !e.isValid());
        if (currentWaveEnemies.isEmpty()) {
            if (currentWave >= TOTAL_WAVES) {
                runComplete = true;
                spawnRewardChest();
                stop();
            } else {
                spawnNextWave();
            }
        }
    }

    @Override
    protected void onStop() {
        for (LivingEntity e : currentWaveEnemies) {
            if (e.isValid()) {
                e.remove();
            }
        }
        currentWaveEnemies.clear();

        Component resultMsg = runComplete
            ? Component.text("Run complete! Collect your rewards.", NamedTextColor.GOLD, TextDecoration.BOLD)
            : Component.text("The roguelike run has ended.", NamedTextColor.RED);

        for (SwordPlayer sp : players) {
            sp.message(resultMsg);
        }
    }

    private void spawnNextWave() {
        currentWave++;
        currentWaveEnemies.clear();

        Location center = getSpawnCenter();
        World world = center.getWorld();

        for (EnemySpec spec : getWaveSpec(currentWave)) {
            for (int i = 0; i < spec.count(); i++) {
                Location spawnLoc = randomSpawnLocation(center, Config.Roguelike.SPAWN_RADIUS);
                LivingEntity mob = (LivingEntity) world.spawnEntity(spawnLoc, spec.entityType());
                SwordEntityArbiter.getOrAdd(mob);
                currentWaveEnemies.add(mob);
            }
        }

        Component waveMsg = Component.text("Wave " + currentWave + " of " + TOTAL_WAVES + " begins!", NamedTextColor.YELLOW);
        for (SwordPlayer sp : players) {
            sp.message(waveMsg);
        }
    }

    private void spawnRewardChest() {
        Location chestLoc = getSpawnCenter().add(0, 2, 0);
        TextDisplay display = (TextDisplay) chestLoc.getWorld().spawnEntity(chestLoc, EntityType.TEXT_DISPLAY);
        display.text(Component.text("✧ Reward Chest ✧", NamedTextColor.GOLD, TextDecoration.BOLD));
        display.setBillboard(Display.Billboard.CENTER);
        // TODO: #285 - distribute MiscItems (Soulfire Flask, Skill Scroll) once item system exists
    }

    private Location getSpawnCenter() {
        World world = Bukkit.getWorld(Config.Roguelike.SPAWN_WORLD);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        return new Location(world, Config.Roguelike.SPAWN_X, Config.Roguelike.SPAWN_Y, Config.Roguelike.SPAWN_Z);
    }

    private Location randomSpawnLocation(Location center, double radius) {
        double angle = random.nextDouble() * 2 * Math.PI;
        double distance = random.nextDouble() * radius;
        double x = center.getX() + distance * Math.cos(angle);
        double z = center.getZ() + distance * Math.sin(angle);
        return new Location(center.getWorld(), x, center.getY(), z);
    }

    private List<EnemySpec> getWaveSpec(int wave) {
        return switch (wave) {
            case 1 -> List.of(
                new EnemySpec(EntityType.PILLAGER, Config.Roguelike.WAVE_1_PILLAGERS)
            );
            case 2 -> List.of(
                new EnemySpec(EntityType.WITHER_SKELETON, Config.Roguelike.WAVE_2_WITHER_SKELETONS)
            );
            case 3 -> List.of(
                new EnemySpec(EntityType.PILLAGER, Config.Roguelike.WAVE_3_PILLAGERS),
                new EnemySpec(EntityType.WITHER_SKELETON, Config.Roguelike.WAVE_3_WITHER_SKELETONS)
            );
            default -> List.of();
        };
    }

    private record EnemySpec(EntityType entityType, int count) {}
}
