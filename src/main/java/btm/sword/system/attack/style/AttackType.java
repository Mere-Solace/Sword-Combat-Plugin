package btm.sword.system.attack.style;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.Attack;
import btm.sword.utility.Prefab;
import btm.sword.utility.math.ControlVectors;

// TODO: consider - add base range multiplier as well
public enum AttackType implements AttackProfile {
    UMBRAL_SLASH1(ControlVectors.of(
        new Vector(-2,0,0),
        new Vector(2,0,-1),
        new Vector(-1.5,0,2),
        new Vector(1.5,0,3)),
        Attack::getRightVector
    ),
    UMBRAL_SLASH1_WINDUP(ControlVectors.of(
        new Vector(-1.5,0.17,1),
        new Vector(-2.1,-0.33,-0.5),
        new Vector(-2,0.17,0.5),
        new Vector(-2,-0.08,0))
    ),

    WIDE_UMBRAL_SLASH1(ControlVectors.of(
        new Vector(-5.7615,0,2.171),
        new Vector(5.845,0,-0.334),
        new Vector(-2.505,0,3.34),
        new Vector(2.505,0,5.01)),
        Attack::getRightVector
    ),
    WIDE_UMBRAL_SLASH1_WINDUP(ControlVectors.of(
        new Vector(-1.66,0.17,-0.5),
        new Vector(-5,0.27,0.83),
        new Vector(-2.5,1.03,1.7),
        new Vector(-3.77,0.51,2.26))
    ),

    WIDE_UMBRAL_SLASH2(ControlVectors.of(
        new Vector(4.008,-1.002,-1.169),
        new Vector(-3.841,1.67,4.008),
        new Vector(1.67,0,2.839),
        new Vector(-1.336,0,5.01)),
        attack -> attack.getRightVector().multiply(-1).add(attack.getForwardVector().multiply(0.3))
    ),
    WIDE_UMBRAL_SLASH2_WINDUP(ControlVectors.of(
        new Vector(5.344,-0.2171,-1.002),
        new Vector(4.509,-1.503,-2.338),
        new Vector(4.7261,0,-0.501),
        new Vector(3.674,-0.4008,-1.5698))
    ),

    WIDE_UMBRAL_SLASH3(ControlVectors.of(
        new Vector(0,5.177,-0.334),
        new Vector(0,-3.674,1.336),
        new Vector(0,1.7368,1.67),
        new Vector(0,-0.167,2.9058)),
        attack -> attack.getForwardVector().add(Config.Direction.UP().multiply(-2))
    ),
    WIDE_UMBRAL_SLASH3_WINDUP(ControlVectors.of(
        new Vector(-3.34,2.0708,3.34),
        new Vector(0.334,4.0915,0),
        new Vector(1.67,0,1.67),
        new Vector(-1.336,0,3.841))
    ),

    SLASH1(ControlVectors.of(
        new Vector(-2.06, -1.26, -0.5),
        new Vector(3.26, 0.79, -0.4),
        new Vector(-2.3, -0.16,3),
        new Vector(1.9, 0.21, 5)),
    attack -> attack.getRightVector().multiply(-0.5).add(attack.getForwardVector().multiply(0.1))
    ),
    SLASH2(ControlVectors.of(
        new Vector(2.6,-1.3,-1.2),
        new Vector(-3,0.9,1.3),
        new Vector(1.6,-0.7,7),
        new Vector(-2.6,1.05,1.85)),
    attack -> attack.getRightVector().multiply(0.5).add(attack.getForwardVector().multiply(0.1))
    ),
    SLASH3(ControlVectors.of(
        new Vector(1.2,2.8,-1.5),
        new Vector(-1.1,-2.2,-0.9),
        new Vector(1,1.96,4.3),
        new Vector(-1.1,-1.77,5)),
    attack -> attack.getTo().add(attack.getForwardVector().multiply(0.5))
    ),

    UP_SMASH(ControlVectors.of(
        new Vector(0.66,-1.53,-0.5),
        new Vector(-0.4,0.67,-0.9),
        new Vector(0.56,-0.89,2.1),
        new Vector(-0.4,1.37,1.65)),
        attack -> Config.Direction.UP().multiply(5)
    ),

    LUNGE1(ControlVectors.of(
        new Vector(0.37,0,2),
        new Vector(0,0,20),
        new Vector(1.1,0,3.1),
        new Vector(0,0,2.46)),
        attack -> new Vector()
    ),

    F_DASH_ATTACK(ControlVectors.of(
        new Vector(-1.95,-0.76,0.9),
        new Vector(1.2,1.1,7.3),
        new Vector(-1.6,-0.57,2.7),
        new Vector(-0.93,0,4.9)),
    attack -> new Vector()
    ),
    B_DASH_ATTACK(ControlVectors.of(
        new Vector(0.696,2.2388,1.74),
        new Vector(-0.8932,-3.2016,0.116),
        new Vector(0.3132,0.4176,2.204),
        new Vector(-0.58,-1.74,1.3572)),
        Attack::getForwardVector
    ),
    R_STRAFE_ATTACK(ControlVectors.of(
        new Vector(-1.503,-0.4008,0.668),
        new Vector(4.676,0.334,1.169),
        new Vector(0.167,0,1.4362),
        new Vector(2.2211,0,1.7702)),
        Attack::getRightVector
    ),
    L_STRAFE_ATTACK(ControlVectors.of(
        new Vector(1.503,-0.4008,0.668),
        new Vector(-4.676,0.334,1.169),
        new Vector(-0.167,0,1.4362),
        new Vector(-2.2211,0,1.7702)),
        attack -> attack.getRightVector().multiply(-1)
    ),

    D_AIR(ControlVectors.of(
        new Vector(-0.35, 2.53, 0.56),
        new Vector(0, -3.42, -0.581),
        new Vector(0.329, -0.165, 4.97),
        new Vector(-0.07, -6.15, 0.98)
    )),
    N_AIR(ControlVectors.of(
        new Vector(1.0961, 1.742, -1.13),
        new Vector(0, -1.987, -0.791),
        new Vector(-0.2825, 0.951, 9.153),
        new Vector(-0.7458, -5.151, -1.808)
    )),

    R_SIDESTEP(ControlVectors.of(
        new Vector(-1.3,1.03,2),
        new Vector(8.2,1.03,-1.9),
        new Vector(-7,-1.73,3.3),
        new Vector(9,-0.93,5)
    )),

    L_SIDESTEP(ControlVectors.of(
        new Vector(1.3,1.03,2),
        new Vector(-8.2,1.03,-1.9),
        new Vector(7,-1.73,3.3),
        new Vector(-9,-0.93,5)
    )),

    OVERHEAD_SLAM(ControlVectors.of(
        new Vector(0, 3.0, -0.5),
        new Vector(0, -4.0, 0.5),
        new Vector(0, 2.0, 1.0),
        new Vector(0, -2.0, 2.0)),
        attack -> Config.Direction.DOWN().multiply(3)
    ),
    AERIAL_SPIKE(ControlVectors.of(
        new Vector(0, 2.5, -0.5),
        new Vector(0, -5.0, 1.5),
        new Vector(0, 1.5, 0.5),
        new Vector(0, -3.0, 2.5)),
        attack -> Config.Direction.DOWN().multiply(5)
    ),

    DEFAULT(ControlVectors.of(
        Config.Direction.UP(),
        Config.Direction.DOWN(),
        Config.Direction.OUT_UP(),
        Config.Direction.OUT_DOWN()
    ));

    private final AttackShape shape;
    private final Function<Attack, Vector> knockbackFunction;
    private final Vector normalVector;

    AttackType(ControlVectors ctrlVectors) {
        this(ctrlVectors, Prefab.Instructions.DEFAULT_KNOCKBACK);
    }

    AttackType(ControlVectors ctrlVectors, Function<Attack, Vector> knockback) {
        this(ctrlVectors, knockback, null);
    }

    AttackType(ControlVectors ctrlVectors, Function<Attack, Vector> knockback, @Nullable Vector normalVector) {
        this(BezierShape.of(ctrlVectors), knockback, normalVector);
    }

    AttackType(AttackShape shape, Function<Attack, Vector> knockback) {
        this(shape, knockback, null);
    }

    AttackType(AttackShape shape, Function<Attack, Vector> knockback, @Nullable Vector normalVector) {
        this.shape = shape;
        this.knockbackFunction = knockback;
        this.normalVector = normalVector;
    }

    /**
     * Returns the {@link AttackShape} backing this attack type's sweep path.
     *
     * @return the shape for this attack type
     */
    @Override
    public AttackShape shape() {
        return shape;
    }

    /**
     * Returns the raw {@link ControlVectors} for this attack type.
     *
     * <p>Intended for non-attack consumers that need the Bézier control points directly
     * (e.g., blade lunge movement). Only valid for entries backed by a {@link BezierShape}.
     *
     * @return the local-space control vectors for this attack type
     * @throws UnsupportedOperationException if this entry's shape is not a {@link BezierShape}
     */
    public ControlVectors controlVectors() {
        if (!(shape instanceof BezierShape bezierShape)) {
            throw new UnsupportedOperationException(
                name() + " is not backed by a BezierShape — controlVectors() is unavailable");
        }
        return bezierShape.controlVectors();
    }

    @Override
    public Function<Attack, Vector> knockbackFunction() {
        return knockbackFunction;
    }

    @Override
    public Vector normalVector() {
        return normalVector;
    }
}
