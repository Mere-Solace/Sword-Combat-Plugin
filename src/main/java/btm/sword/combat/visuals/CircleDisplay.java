package btm.sword.combat.visuals;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.joml.Vector3f;

import btm.sword.combat.simulation.ParticleEffect;
import btm.sword.util.display.DrawUtil;
import btm.sword.util.display.ParticleWrapper;
import btm.sword.util.math.Basis;
import lombok.Getter;
import lombok.Setter;

/**
 * A ring/disc {@link ParticleDisplay} — uses {@link DrawUtil#circle} to draw a filled annulus
 * with configurable normal axis.
 */
@Getter
@Setter
public final class CircleDisplay extends ParticleDisplay {

    /** Choice of normal axis for the ring plane. */
    public enum Normal {
        /** Normal = world +Y. Ring lies in the world-horizontal plane. */
        WORLD_UP,
        /** Normal = attack's local +Y. Ring lies in the attack's horizontal plane. */
        ATTACK_UP,
        /** Normal = attack's local +Z (forward). Ring is perpendicular to the direction of travel. */
        ATTACK_FORWARD,
        /** Normal = attack's local +X (right). Ring is perpendicular to the attack's right axis. */
        ATTACK_RIGHT
    }

    /** Outer edge of the ring in blocks. */
    private double outerRadius;
    /** Inner edge (hole) of the ring in blocks; {@code 0} for a filled disc. */
    private double innerRadius;
    /** Radial spacing between particle rows in blocks. */
    private double spacingRadial;
    /** Angular spacing between particles around the ring in radians. */
    private double spacingArc;
    /** Normal-axis source for the ring plane. */
    private Normal normal;

    /**
     * Creates a circle display with explicit field values.
     *
     * @param anchor            origin anchor for the ring centre
     * @param originOffset      fixed local-space offset
     * @param randomOffsetRange per-axis jitter half-extents
     * @param repeatCount       number of emissions
     * @param repeatPeriodTicks ticks between emissions
     * @param particles         particles drawn at each sample point
     * @param outerRadius       outer edge in blocks
     * @param innerRadius       inner edge (hole) in blocks
     * @param spacingRadial     radial spacing in blocks
     * @param spacingArc        angular spacing in radians
     * @param normal            normal-axis source for the ring plane
     */
    public CircleDisplay(OriginAnchor anchor, Vector3f originOffset, Vector3f randomOffsetRange,
            int repeatCount, int repeatPeriodTicks, List<ParticleEffect> particles,
            double outerRadius, double innerRadius, double spacingRadial, double spacingArc,
            Normal normal) {
        super(anchor, originOffset, randomOffsetRange, repeatCount, repeatPeriodTicks, particles);
        this.outerRadius = Math.max(0.0, outerRadius);
        this.innerRadius = Math.max(0.0, Math.min(innerRadius, outerRadius));
        this.spacingRadial = spacingRadial > 0 ? spacingRadial : 0.25;
        this.spacingArc = spacingArc > 0 ? spacingArc : Math.PI / 16;
        this.normal = normal != null ? normal : Normal.ATTACK_UP;
    }

    /** Convenience factory: unit ring in the attack horizontal plane. */
    public static CircleDisplay defaults() {
        return new CircleDisplay(OriginAnchor.owning(), new Vector3f(), new Vector3f(),
            1, 0, new ArrayList<>(), 1.0, 0.8, 0.25, Math.PI / 16, Normal.ATTACK_UP);
    }

    @Override
    protected void draw(Location resolved, EffectsContext ctx) {
        Vector3f axis = switch (normal) {
            case WORLD_UP -> new Vector3f(0, 1, 0);
            case ATTACK_UP -> ctx.worldTransform().transformDirection(new Vector3f(0, 1, 0));
            case ATTACK_FORWARD -> ctx.worldTransform().transformDirection(new Vector3f(0, 0, 1));
            case ATTACK_RIGHT -> ctx.worldTransform().transformDirection(new Vector3f(1, 0, 0));
        };
        List<ParticleWrapper> wrappers = particles.stream().map(ParticleEffect::toWrapper).toList();
        DrawUtil.circle(wrappers, resolved, basisWithUp(axis),
            outerRadius, innerRadius, spacingRadial, spacingArc);
    }

    private static Basis basisWithUp(Vector3f up) {
        Vector upV = new Vector(up.x, up.y, up.z).normalize();
        Vector forwardRef = Math.abs(upV.getY()) > 0.99 ? new Vector(0, 0, 1) : new Vector(0, 1, 0);
        Vector right = upV.clone().crossProduct(forwardRef).normalize();
        Vector forward = right.clone().crossProduct(upV).normalize();
        return new Basis(right, upV, forward);
    }

    @Override
    public String shapeTypeLabel() {
        return "Circle";
    }

    @Override
    public CircleDisplay copy() {
        CircleDisplay c = new CircleDisplay(anchor,
            new Vector3f(originOffset), new Vector3f(randomOffsetRange),
            repeatCount, repeatPeriodTicks, copyParticles(),
            outerRadius, innerRadius, spacingRadial, spacingArc, normal);
        c.betweenKfRepeat = betweenKfRepeat;
        return c;
    }
}
