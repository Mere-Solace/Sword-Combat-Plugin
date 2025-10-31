package btm.sword.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link VectorUtil}.
 *
 * <p>Tests mathematical vector operations used throughout the Sword Combat Plugin,
 * including basis generation, transformations, projections, and angle calculations.</p>
 */
class VectorUtilTest {

    private static final double EPSILON = 0.0001; // Tolerance for floating-point comparisons

    @Test
    @DisplayName("UP constant should be (0, 1, 0)")
    void testUpConstant() {
        assertEquals(0, VectorUtil.UP.getX(), EPSILON);
        assertEquals(1, VectorUtil.UP.getY(), EPSILON);
        assertEquals(0, VectorUtil.UP.getZ(), EPSILON);
    }

    @Test
    @DisplayName("getBasis should return orthonormal basis for horizontal direction")
    void testGetBasisHorizontal() {
        World mockWorld = Mockito.mock(World.class);
        Location loc = new Location(mockWorld, 0, 0, 0, 0, 0); // Yaw=0, Pitch=0
        Vector dir = new Vector(0, 0, 1); // Forward (north)

        ArrayList<Vector> basis = VectorUtil.getBasis(loc, dir);

        assertEquals(3, basis.size(), "Basis should contain 3 vectors");

        Vector right = basis.get(0);
        Vector up = basis.get(1);
        Vector forward = basis.get(2);

        // Check orthogonality
        assertEquals(0, right.dot(up), EPSILON, "Right and up should be perpendicular");
        assertEquals(0, right.dot(forward), EPSILON, "Right and forward should be perpendicular");
        assertEquals(0, up.dot(forward), EPSILON, "Up and forward should be perpendicular");

        // Check normalization
        assertEquals(1, right.length(), EPSILON, "Right vector should be normalized");
        assertEquals(1, up.length(), EPSILON, "Up vector should be normalized");
        assertEquals(1, forward.length(), EPSILON, "Forward vector should be normalized");

        // Check that forward matches input direction
        assertEquals(dir.getX(), forward.getX(), EPSILON);
        assertEquals(dir.getY(), forward.getY(), EPSILON);
        assertEquals(dir.getZ(), forward.getZ(), EPSILON);
    }

    @Test
    @DisplayName("getBasis should handle straight up direction")
    void testGetBasisStraightUp() {
        World mockWorld = Mockito.mock(World.class);
        Location loc = new Location(mockWorld, 0, 0, 0, 90, 90); // Looking straight up
        Vector dir = new Vector(0, 1, 0); // Straight up

        ArrayList<Vector> basis = VectorUtil.getBasis(loc, dir);

        assertEquals(3, basis.size());

        Vector right = basis.get(0);
        Vector up = basis.get(1);
        Vector forward = basis.get(2);

        // Check orthogonality
        assertEquals(0, right.dot(up), EPSILON, "Right and up should be perpendicular");
        assertEquals(0, right.dot(forward), EPSILON, "Right and forward should be perpendicular");
        assertEquals(0, up.dot(forward), EPSILON, "Up and forward should be perpendicular");

        // Check normalization
        assertEquals(1, right.length(), EPSILON);
        assertEquals(1, up.length(), EPSILON);
        assertEquals(1, forward.length(), EPSILON);
    }

    @Test
    @DisplayName("getBasisWithoutPitch should ignore vertical angle")
    void testGetBasisWithoutPitch() {
        World mockWorld = Mockito.mock(World.class);
        Location loc = new Location(mockWorld, 0, 0, 0, 0, 45); // Yaw=0, Pitch=45

        ArrayList<Vector> basis = VectorUtil.getBasisWithoutPitch(loc);

        assertEquals(3, basis.size());

        Vector right = basis.get(0);
        Vector up = basis.get(1);
        Vector forward = basis.get(2);

        // Up should always be (0, 1, 0)
        assertEquals(0, up.getX(), EPSILON);
        assertEquals(1, up.getY(), EPSILON);
        assertEquals(0, up.getZ(), EPSILON);

        // Forward should be horizontal (Y = 0)
        assertEquals(0, forward.getY(), EPSILON, "Forward should be horizontal");

        // Check orthogonality
        assertEquals(0, right.dot(up), EPSILON);
        assertEquals(0, right.dot(forward), EPSILON);
        assertEquals(0, up.dot(forward), EPSILON);

        // Check normalization
        assertEquals(1, right.length(), EPSILON);
        assertEquals(1, up.length(), EPSILON);
        assertEquals(1, forward.length(), EPSILON);
    }

    @Test
    @DisplayName("transformWithNewBasis should correctly transform vector")
    void testTransformWithNewBasis() {
        // Create a simple basis (standard basis)
        List<Vector> basis = new ArrayList<>();
        basis.add(new Vector(1, 0, 0)); // Right
        basis.add(new Vector(0, 1, 0)); // Up
        basis.add(new Vector(0, 0, 1)); // Forward

        Vector v = new Vector(2, 3, 4);

        Vector result = VectorUtil.transformWithNewBasis(basis, v);

        assertEquals(2, result.getX(), EPSILON);
        assertEquals(3, result.getY(), EPSILON);
        assertEquals(4, result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("transformWithNewBasis should work with rotated basis")
    void testTransformWithRotatedBasis() {
        // Create a 90-degree rotated basis (right becomes forward)
        List<Vector> basis = new ArrayList<>();
        basis.add(new Vector(0, 0, 1));  // Right = Forward
        basis.add(new Vector(0, 1, 0));  // Up = Up
        basis.add(new Vector(-1, 0, 0)); // Forward = -Right

        Vector v = new Vector(1, 0, 0); // Transform "right" in new basis

        Vector result = VectorUtil.transformWithNewBasis(basis, v);

        assertEquals(0, result.getX(), EPSILON);
        assertEquals(0, result.getY(), EPSILON);
        assertEquals(1, result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("getProjOntoPlane should project vector onto horizontal plane")
    void testGetProjOntoPlaneHorizontal() {
        Vector v = new Vector(1, 2, 3);
        Vector normal = new Vector(0, 1, 0); // Y-axis (horizontal plane normal)

        Vector projection = VectorUtil.getProjOntoPlane(v, normal);

        assertEquals(1, projection.getX(), EPSILON);
        assertEquals(0, projection.getY(), EPSILON, "Y component should be removed");
        assertEquals(3, projection.getZ(), EPSILON);
    }

    @Test
    @DisplayName("getProjOntoPlane should handle arbitrary plane normals")
    void testGetProjOntoPlaneArbitrary() {
        Vector v = new Vector(1, 1, 1);
        Vector normal = new Vector(1, 1, 1).normalize(); // Diagonal plane

        Vector projection = VectorUtil.getProjOntoPlane(v, normal);

        // Projection should be perpendicular to normal
        assertEquals(0, projection.dot(normal), EPSILON, "Projection should be perpendicular to normal");
    }

    @Test
    @DisplayName("getPitch should return 0 for horizontal vector")
    void testGetPitchHorizontal() {
        Vector v = new Vector(1, 0, 1);
        double pitch = VectorUtil.getPitch(v);
        assertEquals(0, pitch, EPSILON);
    }

    @Test
    @DisplayName("getPitch should return 90 for straight up vector")
    void testGetPitchUp() {
        Vector v = new Vector(0, 1, 0);
        double pitch = VectorUtil.getPitch(v);
        assertEquals(-90, pitch, EPSILON);
    }

    @Test
    @DisplayName("getPitch should return -90 for straight down vector")
    void testGetPitchDown() {
        Vector v = new Vector(0, -1, 0);
        double pitch = VectorUtil.getPitch(v);
        assertEquals(90, pitch, EPSILON);
    }

    @Test
    @DisplayName("getYaw should return 0 for north-facing vector")
    void testGetYawNorth() {
        Vector v = new Vector(0, 0, 1);
        double yaw = VectorUtil.getYaw(v);
        assertEquals(0, yaw, EPSILON);
    }

    @Test
    @DisplayName("getYaw should return 90 for west-facing vector")
    void testGetYawWest() {
        Vector v = new Vector(1, 0, 0);
        double yaw = VectorUtil.getYaw(v);
        assertEquals(-90, yaw, EPSILON);
    }

    @Test
    @DisplayName("getYaw should return -90 for east-facing vector")
    void testGetYawEast() {
        Vector v = new Vector(-1, 0, 0);
        double yaw = VectorUtil.getYaw(v);
        assertEquals(90, yaw, EPSILON);
    }

    @Test
    @DisplayName("getYaw should return ±180 for south-facing vector")
    void testGetYawSouth() {
        Vector v = new Vector(0, 0, -1);
        double yaw = VectorUtil.getYaw(v);
        assertTrue(Math.abs(Math.abs(yaw) - 180) < EPSILON, "Yaw should be ±180 degrees");
    }
}
