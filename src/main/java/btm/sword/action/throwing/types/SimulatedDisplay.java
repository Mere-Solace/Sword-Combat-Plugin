package btm.sword.action.throwing.types;

import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.action.throwing.InteractiveItem;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base for display-backed simulated items (thrown or dropped).
 * <p>
 * Holds the shared {@link ItemDisplay} and {@link ItemStack} fields, and provides
 * material-based orientation logic used by both {@link ThrownItem} and {@link DroppedItem}.
 * Physics models are intentionally kept separate in each subclass.
 */
@Getter
@Setter
public abstract class SimulatedDisplay implements InteractiveItem {
    protected ItemDisplay display;
    protected ItemStack itemStack;

    /**
     * Applies a material-based {@link Transformation} to the display so that swords, axes,
     * shields, and other tools are oriented correctly in-hand and mid-flight.
     * <p>
     * Subclasses may override this method to apply additional offsets.
     */
    protected void determineOrientation() {
        String name = display.getItemStack().getType().toString();

        // TODO: use CONFIG
        if (name.endsWith("_SWORD")) {
            display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf()
                    .rotateY((float) Math.PI / 2)
                    .rotateZ((float) Math.PI / 2),
                new Vector3f(1, 1, 1),
                new Quaternionf()
            ));
        }
        else if (name.endsWith("AXE") || name.endsWith("_HOE") || name.endsWith("_SHOVEL")) {
            display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateY((float) -Math.PI / 2)
                    .rotateZ((float) Math.PI / 4),
                new Vector3f(1.5f, 1.5f, 1.5f),
                new Quaternionf()
            ));
        }
        else if (display.getItemStack().getType() == Material.SHIELD) {
            display.setTransformation(new Transformation(
                new Vector3f(0, 0, 0),
                new Quaternionf().rotateY((float) (Math.PI / 1.01f) * 0),
                new Vector3f(1, 1, 1),
                new Quaternionf()
            ));
        }
        else {
            display.setTransformation(new Transformation(
                new Vector3f(),
                new Quaternionf().rotateZ((float) Math.PI / 8),
                new Vector3f(1, 1, 1),
                new Quaternionf()
            ));
        }
    }
}
