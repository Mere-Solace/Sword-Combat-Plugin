package btm.sword.utility.display;

import org.bukkit.Particle;
import org.bukkit.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Renders an OBB (Oriented Bounding Box) as a particle wireframe on the main thread.
 *
 * <p>Used by both the dev-mode {@code VolumeEditorMode} and the live
 * {@code VolumeSimulation} hitbox-outline visualization.</p>
 */
public final class ObbWireframe {

    private static final float EDGE_SPACING = 0.18f;
    private static final int SPHERE_SEGMENTS = 16;

    private ObbWireframe() {}

    /**
     * Renders the 12 edges of an OBB as a dust-particle wireframe.
     *
     * @param world       world to spawn particles in
     * @param center      world-space OBB centre
     * @param halfExtents OBB half-extents along its local axes
     * @param rotation    OBB orientation as a unit quaternion
     * @param dust        colour/size to use for the dust particles
     * @return total particle spawn calls made
     */
    public static int renderObb(World world, Vector3f center, Vector3f halfExtents,
            Quaternionf rotation, Particle.DustOptions dust) {
        Vector3f ax = rotation.transform(new Vector3f(halfExtents.x, 0, 0), new Vector3f());
        Vector3f ay = rotation.transform(new Vector3f(0, halfExtents.y, 0), new Vector3f());
        Vector3f az = rotation.transform(new Vector3f(0, 0, halfExtents.z), new Vector3f());

        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            float sx = (i & 1) != 0 ? 1f : -1f;
            float sy = (i & 2) != 0 ? 1f : -1f;
            float sz = (i & 4) != 0 ? 1f : -1f;
            corners[i] = new Vector3f(center)
                .add(ax.x * sx, ax.y * sx, ax.z * sx)
                .add(ay.x * sy, ay.y * sy, ay.z * sy)
                .add(az.x * sz, az.y * sz, az.z * sz);
        }

        int count = 0;
        count += drawEdge(world, corners[0], corners[1], dust);
        count += drawEdge(world, corners[2], corners[3], dust);
        count += drawEdge(world, corners[4], corners[5], dust);
        count += drawEdge(world, corners[6], corners[7], dust);
        count += drawEdge(world, corners[0], corners[2], dust);
        count += drawEdge(world, corners[1], corners[3], dust);
        count += drawEdge(world, corners[4], corners[6], dust);
        count += drawEdge(world, corners[5], corners[7], dust);
        count += drawEdge(world, corners[0], corners[4], dust);
        count += drawEdge(world, corners[1], corners[5], dust);
        count += drawEdge(world, corners[2], corners[6], dust);
        count += drawEdge(world, corners[3], corners[7], dust);
        return count;
    }

    /**
     * Renders three orthogonal great circles to represent a sphere.
     *
     * @param world  world to spawn particles in
     * @param center world-space sphere centre
     * @param radius sphere radius
     * @param dust   colour/size to use for the dust particles
     * @return total particle spawn calls made
     */
    public static int renderSphere(World world, Vector3f center, float radius,
            Particle.DustOptions dust) {
        int count = 0;
        for (int ring = 0; ring < 3; ring++) {
            for (int i = 0; i < SPHERE_SEGMENTS; i++) {
                float a0 = (float) (2 * Math.PI * i / SPHERE_SEGMENTS);
                float a1 = (float) (2 * Math.PI * (i + 1) / SPHERE_SEGMENTS);
                float cos0 = (float) Math.cos(a0);
                float sin0 = (float) Math.sin(a0);
                float cos1 = (float) Math.cos(a1);
                float sin1 = (float) Math.sin(a1);
                Vector3f p0;
                Vector3f p1;
                if (ring == 0) {
                    p0 = new Vector3f(center.x + radius * cos0, center.y, center.z + radius * sin0);
                    p1 = new Vector3f(center.x + radius * cos1, center.y, center.z + radius * sin1);
                } else if (ring == 1) {
                    p0 = new Vector3f(center.x + radius * cos0, center.y + radius * sin0, center.z);
                    p1 = new Vector3f(center.x + radius * cos1, center.y + radius * sin1, center.z);
                } else {
                    p0 = new Vector3f(center.x, center.y + radius * sin0, center.z + radius * cos0);
                    p1 = new Vector3f(center.x, center.y + radius * sin1, center.z + radius * cos1);
                }
                count += drawEdge(world, p0, p1, dust);
            }
        }
        return count;
    }

    /**
     * Draws a single edge between two world-space points using evenly-spaced dust particles.
     *
     * @param world world to spawn particles in
     * @param a     start point
     * @param b     end point
     * @param dust  colour/size to use
     * @return number of particle spawn calls made
     */
    public static int drawEdge(World world, Vector3f a, Vector3f b, Particle.DustOptions dust) {
        float dx = b.x - a.x;
        float dy = b.y - a.y;
        float dz = b.z - a.z;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-4f) return 0;
        int steps = Math.max(1, (int) (len / EDGE_SPACING));
        float sx = dx / steps;
        float sy = dy / steps;
        float sz = dz / steps;
        for (int i = 0; i <= steps; i++) {
            world.spawnParticle(Particle.DUST,
                a.x + sx * i, a.y + sy * i, a.z + sz * i,
                1, 0, 0, 0, 0, dust);
        }
        return steps + 1;
    }
}
