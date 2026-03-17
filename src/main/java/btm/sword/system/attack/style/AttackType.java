package btm.sword.system.attack.style;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.util.Vector;

import btm.sword.config.Config;
import btm.sword.system.attack.Attack;
import btm.sword.system.attack.style.shape.ArcShape;
import btm.sword.system.attack.style.shape.BezierShape;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.utility.Prefab;
import btm.sword.utility.math.ControlVectors;
import btm.sword.utility.math.VectorUtil;

/**
 * Named attack curve presets backed by cubic Bézier control points.
 *
 * <p>Each entry's control-point vectors are read from {@link Config.AttackCurves} via
 * {@link ControlVectors} suppliers, so live config changes (e.g., {@code /sword reload})
 * take effect on the next attack without a server restart.
 */
public enum AttackType implements AttackProfile {
//region Initializations
    UMBRAL_SLASH1(new ControlVectors(
        () -> Config.AttackCurves.UMBRAL_SLASH1_START,
        () -> Config.AttackCurves.UMBRAL_SLASH1_END,
        () -> Config.AttackCurves.UMBRAL_SLASH1_C1,
        () -> Config.AttackCurves.UMBRAL_SLASH1_C2),
        e -> Attack::getRightVector
    ),
    UMBRAL_SLASH1_WINDUP(new ControlVectors(
        () -> Config.AttackCurves.UMBRAL_SLASH1_WINDUP_START,
        () -> Config.AttackCurves.UMBRAL_SLASH1_WINDUP_END,
        () -> Config.AttackCurves.UMBRAL_SLASH1_WINDUP_C1,
        () -> Config.AttackCurves.UMBRAL_SLASH1_WINDUP_C2)
    ),

    WIDE_UMBRAL_SLASH1(new ControlVectors(
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_START,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_END,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_C1,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_C2),
        e -> Attack::getRightVector
    ),
    WIDE_UMBRAL_SLASH1_WINDUP(new ControlVectors(
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_WINDUP_START,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_WINDUP_END,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_WINDUP_C1,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH1_WINDUP_C2)
    ),

    WIDE_UMBRAL_SLASH2(new ControlVectors(
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_START,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_END,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_C1,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_C2),
        e -> attack -> attack.getRightVector().multiply(-1).add(attack.getForwardVector().multiply(0.3))
    ),
    WIDE_UMBRAL_SLASH2_WINDUP(new ControlVectors(
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_WINDUP_START,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_WINDUP_END,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_WINDUP_C1,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH2_WINDUP_C2)
    ),

    WIDE_UMBRAL_SLASH3(new ControlVectors(
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_START,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_END,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_C1,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_C2),
        e -> attack -> attack.getForwardVector().add(Config.Direction.UP().multiply(-2))
    ),
    WIDE_UMBRAL_SLASH3_WINDUP(new ControlVectors(
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_WINDUP_START,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_WINDUP_END,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_WINDUP_C1,
        () -> Config.AttackCurves.WIDE_UMBRAL_SLASH3_WINDUP_C2)
    ),

    SLASH1(new ControlVectors(
        () -> Config.AttackCurves.SLASH1_START,
        () -> Config.AttackCurves.SLASH1_END,
        () -> Config.AttackCurves.SLASH1_C1,
        () -> Config.AttackCurves.SLASH1_C2),
        e -> attack -> attack.getRightVector().multiply(-0.5).add(attack.getForwardVector().multiply(0.1))
    ),
    SLASH2(new ControlVectors(
        () -> Config.AttackCurves.SLASH2_START,
        () -> Config.AttackCurves.SLASH2_END,
        () -> Config.AttackCurves.SLASH2_C1,
        () -> Config.AttackCurves.SLASH2_C2),
        e -> attack -> attack.getRightVector().multiply(0.5).add(attack.getForwardVector().multiply(0.1))
    ),
    SLASH3(new ControlVectors(
        () -> Config.AttackCurves.SLASH3_START,
        () -> Config.AttackCurves.SLASH3_END,
        () -> Config.AttackCurves.SLASH3_C1,
        () -> Config.AttackCurves.SLASH3_C2),
        e -> attack -> attack.getTo().add(attack.getForwardVector().multiply(0.5))
    ),

    UP_SMASH(new ControlVectors(
        () -> Config.AttackCurves.UP_SMASH_START,
        () -> Config.AttackCurves.UP_SMASH_END,
        () -> Config.AttackCurves.UP_SMASH_C1,
        () -> Config.AttackCurves.UP_SMASH_C2),
        e -> attack -> Config.Direction.UP().multiply(5)
    ),

    LUNGE1(new ControlVectors(
        () -> Config.AttackCurves.LUNGE1_START,
        () -> Config.AttackCurves.LUNGE1_END,
        () -> Config.AttackCurves.LUNGE1_C1,
        () -> Config.AttackCurves.LUNGE1_C2),
        e -> attack -> new Vector()
    ),

    F_DASH_ATTACK(new ControlVectors(
        () -> Config.AttackCurves.F_DASH_ATTACK_START,
        () -> Config.AttackCurves.F_DASH_ATTACK_END,
        () -> Config.AttackCurves.F_DASH_ATTACK_C1,
        () -> Config.AttackCurves.F_DASH_ATTACK_C2),
        e -> attack -> new Vector()
    ),
    B_DASH_ATTACK(new ControlVectors(
        () -> Config.AttackCurves.B_DASH_ATTACK_START,
        () -> Config.AttackCurves.B_DASH_ATTACK_END,
        () -> Config.AttackCurves.B_DASH_ATTACK_C1,
        () -> Config.AttackCurves.B_DASH_ATTACK_C2),
        e -> Attack::getForwardVector
    ),
    R_STRAFE_ATTACK(new ControlVectors(
        () -> Config.AttackCurves.R_STRAFE_ATTACK_START,
        () -> Config.AttackCurves.R_STRAFE_ATTACK_END,
        () -> Config.AttackCurves.R_STRAFE_ATTACK_C1,
        () -> Config.AttackCurves.R_STRAFE_ATTACK_C2),
        e -> Attack::getRightVector
    ),
    L_STRAFE_ATTACK(new ControlVectors(
        () -> Config.AttackCurves.L_STRAFE_ATTACK_START,
        () -> Config.AttackCurves.L_STRAFE_ATTACK_END,
        () -> Config.AttackCurves.L_STRAFE_ATTACK_C1,
        () -> Config.AttackCurves.L_STRAFE_ATTACK_C2),
        e -> attack -> attack.getRightVector().multiply(-1)
    ),

    D_AIR(new ControlVectors(
        () -> Config.AttackCurves.D_AIR_START,
        () -> Config.AttackCurves.D_AIR_END,
        () -> Config.AttackCurves.D_AIR_C1,
        () -> Config.AttackCurves.D_AIR_C2)
    ),
    N_AIR(new ControlVectors(
        () -> Config.AttackCurves.N_AIR_START,
        () -> Config.AttackCurves.N_AIR_END,
        () -> Config.AttackCurves.N_AIR_C1,
        () -> Config.AttackCurves.N_AIR_C2)
    ),

    DEFAULT(new ControlVectors(
        Config.Direction::UP,
        Config.Direction::DOWN,
        Config.Direction::OUT_UP,
        Config.Direction::OUT_DOWN
    )),

    BLADE_RETRIEVAL_CIRCULAR_SLASH(
        new ArcShape(Config.UmbralBlade.CIRCULAR_SLASH_RADIUS, Config.UmbralBlade.CIRCULAR_SLASH_START_ANGLE,
            Config.UmbralBlade.CIRCULAR_SLASH_END_ANGLE, Config.UmbralBlade.CIRCULAR_SLASH_HEIGHT),
        e -> attack -> VectorUtil.getVectorTo(e, attack.getAttacker(), Config.UmbralBlade.CIRCULAR_SLASH_KNOCKBACK)
    );
//endregion

    private final AttackShape shape;
    private final Function<SwordEntity, Function<Attack, Vector>> knockbackFunction;
    private final Vector normalVector;

    AttackType(ControlVectors ctrlVectors) {
        this(ctrlVectors, Prefab.Instructions.DEFAULT_KNOCKBACK);
    }

    AttackType(ControlVectors ctrlVectors, Function<SwordEntity, Function<Attack, Vector>> knockback) {
        this(ctrlVectors, knockback, null);
    }

    AttackType(ControlVectors ctrlVectors, Function<SwordEntity, Function<Attack, Vector>> knockback, @Nullable Vector normalVector) {
        this(BezierShape.of(ctrlVectors), knockback, normalVector);
    }

    AttackType(AttackShape shape, Function<SwordEntity, Function<Attack, Vector>> knockback) {
        this(shape, knockback, null);
    }

    AttackType(AttackShape shape, Function<SwordEntity, Function<Attack, Vector>> knockback, @Nullable Vector normalVector) {
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
    public Function<SwordEntity, Function<Attack, Vector>> knockbackFunction() {
        return knockbackFunction;
    }

    @Override
    public Vector normalVector() {
        return normalVector;
    }
}
