package btm.sword.system.scene;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Manages the collection of {@link TextDisplay} and {@link ItemDisplay} entities
 * shown during the main menu scene.
 * <p>
 * All entities are positioned relative to the camera anchor each tick so they
 * stay in the same visual position as the camera drifts. Content is currently
 * hard-coded; future work can expose it via {@code Config.Scene}.
 * </p>
 *
 * <h2>Entities</h2>
 * <ul>
 *   <li>Title text display — game name.</li>
 *   <li>Subtitle text display — tagline / press-shift prompt.</li>
 *   <li>Two decorative {@link ItemDisplay} entities showing floating swords.</li>
 * </ul>
 */
public class HudDisplayGroup {

    private TextDisplay titleDisplay;
    private TextDisplay subtitleDisplay;
    private final List<ItemDisplay> decorItems = new ArrayList<>();

    /**
     * Spawns all HUD entities relative to the given anchor location.
     *
     * @param anchor the world-space anchor point (camera position) for initial placement
     */
    public void spawn(Location anchor) {
        Location titleLoc = anchor.clone().add(0, 1.2, 0);
        titleDisplay = (TextDisplay) anchor.getWorld().spawnEntity(titleLoc, EntityType.TEXT_DISPLAY);
        titleDisplay.text(Component.text("Sword: Combat Evolved")
            .color(NamedTextColor.WHITE)
            .decorate(TextDecoration.BOLD));
        titleDisplay.setBillboard(Display.Billboard.CENTER);
        titleDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        titleDisplay.setShadowed(true);
        titleDisplay.setTransformation(new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f(0, 0, 1, 0),
            new Vector3f(0.5f, 0.5f, 0.5f),
            new AxisAngle4f(0, 0, 1, 0)
        ));

        Location subtitleLoc = anchor.clone().add(0, 0.7, 0);
        subtitleDisplay = (TextDisplay) anchor.getWorld().spawnEntity(subtitleLoc, EntityType.TEXT_DISPLAY);
        subtitleDisplay.text(Component.text("Press Shift to enter the world")
            .color(NamedTextColor.GRAY));
        subtitleDisplay.setBillboard(Display.Billboard.CENTER);
        subtitleDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        subtitleDisplay.setTransformation(new Transformation(
            new Vector3f(0, 0, 0),
            new AxisAngle4f(0, 0, 1, 0),
            new Vector3f(0.3f, 0.3f, 0.3f),
            new AxisAngle4f(0, 0, 1, 0)
        ));

        Location item1Loc = anchor.clone().add(-1.5, 0.5, 0);
        ItemDisplay item1 = (ItemDisplay) anchor.getWorld().spawnEntity(item1Loc, EntityType.ITEM_DISPLAY);
        item1.setItemStack(new ItemStack(Material.DIAMOND_SWORD));
        item1.setBillboard(Display.Billboard.VERTICAL);
        decorItems.add(item1);

        Location item2Loc = anchor.clone().add(1.5, 0.5, 0);
        ItemDisplay item2 = (ItemDisplay) anchor.getWorld().spawnEntity(item2Loc, EntityType.ITEM_DISPLAY);
        item2.setItemStack(new ItemStack(Material.NETHERITE_SWORD));
        item2.setBillboard(Display.Billboard.VERTICAL);
        decorItems.add(item2);
    }

    /**
     * Repositions all HUD entities relative to the new anchor location.
     * Called each tick as the camera drifts.
     *
     * @param anchor the updated world-space camera anchor
     * @param tick   current tick counter used to animate decorative item orbit
     */
    public void tick(Location anchor, int tick) {
        if (titleDisplay != null) {
            titleDisplay.teleport(anchor.clone().add(0, 1.2, 0));
        }
        if (subtitleDisplay != null) {
            subtitleDisplay.teleport(anchor.clone().add(0, 0.7, 0));
        }

        double orbitRadius = 1.5;
        double orbitSpeed = 0.02;
        for (int i = 0; i < decorItems.size(); i++) {
            double angle = tick * orbitSpeed + Math.PI * i;
            double dx = Math.cos(angle) * orbitRadius;
            double dz = Math.sin(angle) * orbitRadius;
            decorItems.get(i).teleport(anchor.clone().add(dx, 0.5, dz));
        }
    }

    /**
     * Despawns all HUD entities from the world.
     */
    public void remove() {
        if (titleDisplay != null) {
            titleDisplay.remove();
            titleDisplay = null;
        }
        if (subtitleDisplay != null) {
            subtitleDisplay.remove();
            subtitleDisplay = null;
        }
        for (ItemDisplay item : decorItems) {
            item.remove();
        }
        decorItems.clear();
    }
}
