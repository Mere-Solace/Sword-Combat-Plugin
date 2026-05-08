package btm.sword.combat.style;

import java.util.function.Function;

import javax.annotation.Nullable;

import org.bukkit.util.Vector;

import btm.sword.combat.attack.Attack;
import btm.sword.combat.style.shape.ArcShape;
import btm.sword.combat.style.shape.BezierShape;
import btm.sword.config.section.AttackCurveConfig;
import btm.sword.config.section.DirectionConfig;
import btm.sword.config.section.UmbralBladeConfig;
import btm.sword.entity.base.SwordEntity;
import btm.sword.util.math.ControlVectors;
import btm.sword.util.math.VectorUtil;
import btm.sword.util.prefab.Prefab;

/**
 * Named attack curve presets backed by cubic Bézier control points.
 *
 * <p>Each entry's control-point vectors are read from {@link AttackCurveConfig} via
 * {@link ControlVectors} suppliers, so live config changes (e.g., {@code /sword reload})
 * take effect on the next attack without a server restart.
 */
public enum AttackType implements AttackProfile {
//region Initializations
    UMBRAL_SLASH1(new ControlVectors(
        () -> AttackCurveConfig.UMBRAL_SLASH1_START,
        () -> AttackCurveConfig.UMBRAL_SLASH1_END,
        () -> AttackCurveConfig.UMBRAL_SLASH1_C1,
        () -> AttackCurveConfig.UMBRAL_SLASH1_C2),
        e -> Attack::getRightVector
    ),
    UMBRAL_SLASH1_WINDUP(new ControlVectors(
        () -> AttackCurveConfig.UMBRAL_SLASH1_WINDUP_START,
        () -> AttackCurveConfig.UMBRAL_SLASH1_WINDUP_END,
        () -> AttackCurveConfig.UMBRAL_SLASH1_WINDUP_C1,
        () -> AttackCurveConfig.UMBRAL_SLASH1_WINDUP_C2)
    ),

    WIDE_UMBRAL_SLASH1(new ControlVectors(
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_START,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_END,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_C1,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_C2),
        e -> Attack::getRightVector
    ),
    WIDE_UMBRAL_SLASH1_WINDUP(new ControlVectors(
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_WINDUP_START,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_WINDUP_END,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_WINDUP_C1,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH1_WINDUP_C2)
    ),

    WIDE_UMBRAL_SLASH2(new ControlVectors(
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_START,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_END,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_C1,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_C2),
        e -> attack -> attack.getRightVector().multiply(-1).add(attack.getForwardVector().multiply(0.3))
    ),
    WIDE_UMBRAL_SLASH2_WINDUP(new ControlVectors(
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_WINDUP_START,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_WINDUP_END,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_WINDUP_C1,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH2_WINDUP_C2)
    ),

    WIDE_UMBRAL_SLASH3(new ControlVectors(
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_START,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_END,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_C1,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_C2),
        e -> attack -> attack.getForwardVector().add(DirectionConfig.up().multiply(-2))
    ),
    WIDE_UMBRAL_SLASH3_WINDUP(new ControlVectors(
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_WINDUP_START,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_WINDUP_END,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_WINDUP_C1,
        () -> AttackCurveConfig.WIDE_UMBRAL_SLASH3_WINDUP_C2)
    ),

    SLASH1(new ControlVectors(
        () -> AttackCurveConfig.SLASH1_START,
        () -> AttackCurveConfig.SLASH1_END,
        () -> AttackCurveConfig.SLASH1_C1,
        () -> AttackCurveConfig.SLASH1_C2),
        e -> attack -> attack.getRightVector().multiply(-0.5).add(attack.getForwardVector().multiply(0.1))
    ),
    SLASH2(new ControlVectors(
        () -> AttackCurveConfig.SLASH2_START,
        () -> AttackCurveConfig.SLASH2_END,
        () -> AttackCurveConfig.SLASH2_C1,
        () -> AttackCurveConfig.SLASH2_C2),
        e -> attack -> attack.getRightVector().multiply(0.5).add(attack.getForwardVector().multiply(0.1))
    ),
    SLASH3(new ControlVectors(
        () -> AttackCurveConfig.SLASH3_START,
        () -> AttackCurveConfig.SLASH3_END,
        () -> AttackCurveConfig.SLASH3_C1,
        () -> AttackCurveConfig.SLASH3_C2),
        e -> attack -> attack.getTo().add(attack.getForwardVector().multiply(0.5))
    ),

    UP_SMASH(new ControlVectors(
        () -> AttackCurveConfig.UP_SMASH_START,
        () -> AttackCurveConfig.UP_SMASH_END,
        () -> AttackCurveConfig.UP_SMASH_C1,
        () -> AttackCurveConfig.UP_SMASH_C2),
        e -> attack -> DirectionConfig.up().multiply(5)
    ),

    LUNGE1(new ControlVectors(
        () -> AttackCurveConfig.LUNGE1_START,
        () -> AttackCurveConfig.LUNGE1_END,
        () -> AttackCurveConfig.LUNGE1_C1,
        () -> AttackCurveConfig.LUNGE1_C2),
        e -> attack -> new Vector()
    ),

    F_DASH_ATTACK(new ControlVectors(
        () -> AttackCurveConfig.F_DASH_ATTACK_START,
        () -> AttackCurveConfig.F_DASH_ATTACK_END,
        () -> AttackCurveConfig.F_DASH_ATTACK_C1,
        () -> AttackCurveConfig.F_DASH_ATTACK_C2),
        e -> attack -> new Vector()
    ),
    B_DASH_ATTACK(new ControlVectors(
        () -> AttackCurveConfig.B_DASH_ATTACK_START,
        () -> AttackCurveConfig.B_DASH_ATTACK_END,
        () -> AttackCurveConfig.B_DASH_ATTACK_C1,
        () -> AttackCurveConfig.B_DASH_ATTACK_C2),
        e -> Attack::getForwardVector
    ),
    R_STRAFE_ATTACK(new ControlVectors(
        () -> AttackCurveConfig.R_STRAFE_ATTACK_START,
        () -> AttackCurveConfig.R_STRAFE_ATTACK_END,
        () -> AttackCurveConfig.R_STRAFE_ATTACK_C1,
        () -> AttackCurveConfig.R_STRAFE_ATTACK_C2),
        e -> Attack::getRightVector
    ),
    L_STRAFE_ATTACK(new ControlVectors(
        () -> AttackCurveConfig.L_STRAFE_ATTACK_START,
        () -> AttackCurveConfig.L_STRAFE_ATTACK_END,
        () -> AttackCurveConfig.L_STRAFE_ATTACK_C1,
        () -> AttackCurveConfig.L_STRAFE_ATTACK_C2),
        e -> attack -> attack.getRightVector().multiply(-1)
    ),

    D_AIR(new ControlVectors(
        () -> AttackCurveConfig.D_AIR_START,
        () -> AttackCurveConfig.D_AIR_END,
        () -> AttackCurveConfig.D_AIR_C1,
        () -> AttackCurveConfig.D_AIR_C2)
    ),
    N_AIR(new ControlVectors(
        () -> AttackCurveConfig.N_AIR_START,
        () -> AttackCurveConfig.N_AIR_END,
        () -> AttackCurveConfig.N_AIR_C1,
        () -> AttackCurveConfig.N_AIR_C2)
    ),

    DEFAULT(new ControlVectors(
        DirectionConfig::up,
        DirectionConfig::down,
        DirectionConfig::outUp,
        DirectionConfig::outDown
    )),

    BLADE_RETRIEVAL_CIRCULAR_SLASH(
        new ArcShape(UmbralBladeConfig.CIRCULAR_SLASH_RADIUS, UmbralBladeConfig.CIRCULAR_SLASH_START_ANGLE,
            UmbralBladeConfig.CIRCULAR_SLASH_END_ANGLE, UmbralBladeConfig.CIRCULAR_SLASH_HEIGHT),
        e -> attack -> VectorUtil.getVectorTo(e, attack.getAttacker(), UmbralBladeConfig.CIRCULAR_SLASH_KNOCKBACK)
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
