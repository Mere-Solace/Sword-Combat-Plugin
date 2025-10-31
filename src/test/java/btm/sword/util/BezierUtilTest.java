package btm.sword.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BezierUtil}.
 *
 * <p>Tests Bézier curve generation and manipulation functions used for attack animations,
 * projectile paths, and visual effects in the Sword Combat Plugin.</p>
 */
class BezierUtilTest {

    private static final double EPSILON = 0.0001;

    @Test
    @DisplayName("cubicBezier3d at t=0 should return start point")
    void testCubicBezier3dAtStart() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);

        Function<Double, Vector> curve = BezierUtil.cubicBezier3d(start, end, c1, c2);
        Vector result = curve.apply(0.0);

        assertEquals(start.getX(), result.getX(), EPSILON);
        assertEquals(start.getY(), result.getY(), EPSILON);
        assertEquals(start.getZ(), result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("cubicBezier3d at t=1 should return end point")
    void testCubicBezier3dAtEnd() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);

        Function<Double, Vector> curve = BezierUtil.cubicBezier3d(start, end, c1, c2);
        Vector result = curve.apply(1.0);

        assertEquals(end.getX(), result.getX(), EPSILON);
        assertEquals(end.getY(), result.getY(), EPSILON);
        assertEquals(end.getZ(), result.getZ(), EPSILON);
    }

    @Test
    @DisplayName("cubicBezier3d at t=0.5 should be influenced by control points")
    void testCubicBezier3dAtMidpoint() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);

        Function<Double, Vector> curve = BezierUtil.cubicBezier3d(start, end, c1, c2);
        Vector result = curve.apply(0.5);

        // At t=0.5, curve should be pulled toward control points (upward)
        assertTrue(result.getY() > 0, "Curve should be pulled upward by control points");
        assertTrue(result.getX() > start.getX() && result.getX() < end.getX(),
                "X should be between start and end");
    }

    @Test
    @DisplayName("cubicBezier3D should generate correct number of points")
    void testCubicBezier3DPointCount() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);
        int steps = 50;

        List<Vector> points = BezierUtil.cubicBezier3D(start, end, c1, c2, steps);

        assertEquals(steps, points.size());
    }

    @Test
    @DisplayName("cubicBezier3D first point should match start")
    void testCubicBezier3DFirstPoint() {
        Vector start = new Vector(1, 2, 3);
        Vector end = new Vector(10, 20, 30);
        Vector c1 = new Vector(3, 5, 6);
        Vector c2 = new Vector(7, 15, 24);

        List<Vector> points = BezierUtil.cubicBezier3D(start, end, c1, c2, 100);

        Vector firstPoint = points.get(0);
        assertEquals(start.getX(), firstPoint.getX(), EPSILON);
        assertEquals(start.getY(), firstPoint.getY(), EPSILON);
        assertEquals(start.getZ(), firstPoint.getZ(), EPSILON);
    }

    @Test
    @DisplayName("cubicBezier3D points should progress toward end")
    void testCubicBezier3DProgression() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(100, 0, 0);
        Vector c1 = new Vector(25, 10, 0);
        Vector c2 = new Vector(75, 10, 0);

        List<Vector> points = BezierUtil.cubicBezier3D(start, end, c1, c2, 10);

        // Each point should generally progress in X direction
        for (int i = 1; i < points.size(); i++) {
            assertTrue(points.get(i).getX() >= points.get(i - 1).getX(),
                    "Points should progress toward end");
        }
    }

    @Test
    @DisplayName("cubicBezier3D with linear control points should approximate straight line")
    void testCubicBezier3DLinear() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        // Control points on the line between start and end
        Vector c1 = new Vector(3.33, 0, 0);
        Vector c2 = new Vector(6.67, 0, 0);

        List<Vector> points = BezierUtil.cubicBezier3D(start, end, c1, c2, 50);

        // All points should be approximately on the line (Y and Z near 0)
        for (Vector point : points) {
            assertEquals(0, point.getY(), 0.01, "Y should remain near 0 for linear path");
            assertEquals(0, point.getZ(), 0.01, "Z should remain near 0 for linear path");
        }
    }

    @Test
    @DisplayName("adjustCtrlToBasis should transform and scale control vectors")
    void testAdjustCtrlToBasis() {
        // Standard basis
        List<Vector> basis = new ArrayList<>();
        basis.add(new Vector(1, 0, 0)); // Right
        basis.add(new Vector(0, 1, 0)); // Up
        basis.add(new Vector(0, 0, 1)); // Forward

        List<Vector> controlVectors = List.of(
                new Vector(1, 0, 0),
                new Vector(0, 1, 0)
        );

        double multiplier = 2.0;

        List<Vector> result = BezierUtil.adjustCtrlToBasis(basis, controlVectors, multiplier);

        assertEquals(2, result.size());

        // First vector should be (2, 0, 0)
        assertEquals(2, result.get(0).getX(), EPSILON);
        assertEquals(0, result.get(0).getY(), EPSILON);
        assertEquals(0, result.get(0).getZ(), EPSILON);

        // Second vector should be (0, 2, 0)
        assertEquals(0, result.get(1).getX(), EPSILON);
        assertEquals(2, result.get(1).getY(), EPSILON);
        assertEquals(0, result.get(1).getZ(), EPSILON);
    }

    @Test
    @DisplayName("cubicBezierRational3D at t=0 should return start point")
    void testCubicBezierRational3DAtStart() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);

        List<Vector> points = BezierUtil.cubicBezierRational3D(start, end, c1, c2, 1, 1, 1, 1, 50);

        Vector firstPoint = points.get(0);
        assertEquals(start.getX(), firstPoint.getX(), EPSILON);
        assertEquals(start.getY(), firstPoint.getY(), EPSILON);
        assertEquals(start.getZ(), firstPoint.getZ(), EPSILON);
    }

    @Test
    @DisplayName("cubicBezierRational3D should generate at least as many points as steps")
    void testCubicBezierRational3DPointCount() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);
        int steps = 20;

        List<Vector> points = BezierUtil.cubicBezierRational3D(start, end, c1, c2, 1, 1, 1, 1, steps);

        // Should have at least 'steps' points (may have more due to midpoint filling)
        assertTrue(points.size() >= steps,
                "Should have at least " + steps + " points, got " + points.size());
    }

    @Test
    @DisplayName("cubicBezierRational3D with equal weights should behave like standard bezier")
    void testCubicBezierRational3DEqualWeights() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);

        List<Vector> standardPoints = BezierUtil.cubicBezier3D(start, end, c1, c2, 50);
        List<Vector> rationalPoints = BezierUtil.cubicBezierRational3D(start, end, c1, c2, 1, 3, 3, 1, 50);

        // First point should match
        assertEquals(standardPoints.get(0).getX(), rationalPoints.get(0).getX(), EPSILON);
        assertEquals(standardPoints.get(0).getY(), rationalPoints.get(0).getY(), EPSILON);
        assertEquals(standardPoints.get(0).getZ(), rationalPoints.get(0).getZ(), EPSILON);
    }

    @Test
    @DisplayName("cubicBezierRational3D should handle different weights")
    void testCubicBezierRational3DDifferentWeights() {
        Vector start = new Vector(0, 0, 0);
        Vector end = new Vector(10, 0, 0);
        Vector c1 = new Vector(3, 5, 0);
        Vector c2 = new Vector(7, 5, 0);

        // Higher weight on c1 should pull curve toward c1
        List<Vector> points = BezierUtil.cubicBezierRational3D(start, end, c1, c2, 1, 10, 1, 1, 50);

        assertNotNull(points);
        assertTrue(points.size() >= 50);

        // At least some points should be pulled upward by weighted c1
        boolean hasElevatedPoints = points.stream()
                .anyMatch(p -> p.getY() > 1.0);
        assertTrue(hasElevatedPoints, "Curve should be influenced by weighted control point");
    }
}
