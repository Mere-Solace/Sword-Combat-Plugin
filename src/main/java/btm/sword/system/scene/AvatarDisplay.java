package btm.sword.system.scene;

/**
 * STUB — Player avatar mannequin for the main menu scene.
 *
 * <p>In SPECTATOR mode the player is invisible to themselves, so showing the player's
 * character requires constructing a mannequin out of {@link org.bukkit.entity.ItemDisplay}
 * entities positioned and oriented to approximate a standing body.
 *
 * <p>TODO: #233-avatar — Player mannequin using ItemDisplay entities:
 * <ul>
 *   <li>HEAD: ItemDisplay with a player skull ({@link org.bukkit.inventory.meta.SkullMeta}
 *       configured with the owning player's texture UUID).</li>
 *   <li>CHEST/LEGS/FEET: ItemDisplay entities showing the player's currently equipped
 *       armor items from each equipment slot.</li>
 *   <li>Pose each part using transformation matrices to approximate a standing body shape.</li>
 *   <li>Place the mannequin at a fixed showcase position in the scene; point
 *       {@link GentleDriftCameraController} toward it.</li>
 * </ul>
 */
public class AvatarDisplay {

    private AvatarDisplay() {}
}
