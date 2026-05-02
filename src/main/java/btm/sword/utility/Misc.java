package btm.sword.utility;

import java.util.function.Consumer;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

import btm.sword.config.Config;
import btm.sword.control.TimeArbiter;
import btm.sword.input.InputRegistrar;
import btm.sword.system.entity.impl.Combatant;
import btm.sword.system.entity.impl.SwordPlayer;

/** Miscellaneous utility actions and helpers that do not fit into a dedicated utility class. */
public final class Misc {

    private Misc() {}

    public static final Consumer<Combatant> TURN = c -> {
        if (!(c instanceof SwordPlayer sp)) return;
        ProtocolManager man = ProtocolLibrary.getProtocolManager();

        Location loc = sp.player().getLocation();

        float newYaw = loc.getYaw() + 90.0f;      // turn 90 degrees
        float newPitch = loc.getPitch();          // keep same pitch

        // 1. Create the Packet
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.POSITION);

        // 2. Get the existing "template" object to find its Class/Constructor
        Object template = packet.getModifier().read(1);
        Class<?> recordClass = template.getClass();

        try {
            // The PositionMoveRotation Record constructor in 1.21.x:
            // (Vec3 pos, Vec3 delta, float yaw, float pitch)
            // Note: Vec3 is the NMS version, but we can use ProtocolLib's wrappers or reflection

            // For an animation system, it's often easier to just let ProtocolLib
            // handle the instantiation if you have the right wrapper, but since
            // it's being picky, we do it via the Constructor:

            // 1. Get the NMS Vec3 class
            Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
            java.lang.reflect.Constructor<?> vecConstructor = vec3Class.getConstructor(double.class, double.class, double.class);

            // 2. Create the ACTUAL NMS Vec3 objects
            Object vecPos = vecConstructor.newInstance(loc.getX(), loc.getY(), loc.getZ());
            Object vecDelta = vecConstructor.newInstance(0.0, 0.0, 0.0); // Velocity

            // 3. Now the Record constructor will accept them
            java.lang.reflect.Constructor<?> constructor = recordClass.getConstructors()[0];
            Object newRecord = constructor.newInstance(
                vecPos,
                vecDelta,
                newYaw,
                newPitch
            );

            // 3. Write it back to the packet
            packet.getModifier().write(0, 0); // Teleport ID
            packet.getModifier().write(1, newRecord); // The Record
            packet.getModifier().write(2, new java.util.HashSet<>()); // Relative Flags

            man.sendServerPacket(sp.player(), packet);

        } catch (Exception e) {
            e.printStackTrace();
        }


        Debug.system("Sent that packet...");
    };

    public static final Consumer<Combatant> SPIN_UP = c -> {
        if (!(c instanceof SwordPlayer sp)) return;

        ArmorStand marker = (ArmorStand) sp.player().getWorld().spawnEntity(sp.locFromEyeDir(3), EntityType.ARMOR_STAND);

        marker.setBasePlate(false);
        marker.setArms(false);
        marker.setGravity(false);
        marker.setCollidable(false);
        marker.setMarker(true);
        marker.setInvisible(true);
        marker.setInvulnerable(true);

        sp.player().setGameMode(GameMode.SPECTATOR);

        sp.player().setSpectatorTarget(marker);

        TimeArbiter.runFixedIterationTaskTimer(
            null,
            () -> {

                Location loc = marker.getLocation().add(Config.Direction.up().multiply(0.005));
                Vector dir = loc.getDirection();
                dir.rotateAroundAxis(Config.Direction.up(), Math.PI / 360);

                loc.setDirection(dir);
                marker.teleport(loc);
            },
            0, 50, 2000,
            InputRegistrar.class, "initializeInputTree",
            () -> sp.message("done")
        );
    };


    public static final Consumer<Combatant> CAMERA_TEST = c -> {
        if (!(c instanceof SwordPlayer sp)) return;

        Vector dummyDir = sp.dir().multiply(-1);
        Entity dummy = sp.player().getWorld().spawnEntity(sp.locFromEyeDir(10).setDirection(dummyDir), EntityType.SPIDER);

        dummy.setInvisible(true);


    };
}
