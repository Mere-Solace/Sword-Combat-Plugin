package btm.sword.config.section;

import static btm.sword.config.Config.register;

import org.bukkit.util.Vector;

import btm.sword.config.Config;

/**
 * Bézier control-point vectors for each named {@link btm.sword.combat.style.AttackType}.
 *
 * <p>Each attack type exposes four {@link Vector} fields — {@code _START}, {@code _END},
 * {@code _C1}, and {@code _C2} — corresponding to the four cubic Bézier control points.
 * Values are loaded from the {@code attack_curves} YAML section as {@code {x, y, z}} maps
 * and hot-reloaded by {@code /sword reload}.
 *
 * <p>{@link btm.sword.combat.style.AttackType} enum constants reference these fields
 * via {@link btm.sword.util.math.ControlVectors} suppliers so that live config changes
 * take effect on the next attack without a restart.
 */
public final class AttackCurveConfig {

    private AttackCurveConfig() {}

    // UMBRAL_SLASH1
    public static Vector UMBRAL_SLASH1_START = new Vector(-2, 0, 0);
    static { register("attack_curves.umbral_slash1_start", UMBRAL_SLASH1_START, Vector.class,
        v -> UMBRAL_SLASH1_START = v, Config::loadVector); }
    public static Vector UMBRAL_SLASH1_END = new Vector(2, 0, -1);
    static { register("attack_curves.umbral_slash1_end", UMBRAL_SLASH1_END, Vector.class,
        v -> UMBRAL_SLASH1_END = v, Config::loadVector); }
    public static Vector UMBRAL_SLASH1_C1 = new Vector(-1.5, 0, 2);
    static { register("attack_curves.umbral_slash1_c1", UMBRAL_SLASH1_C1, Vector.class,
        v -> UMBRAL_SLASH1_C1 = v, Config::loadVector); }
    public static Vector UMBRAL_SLASH1_C2 = new Vector(1.5, 0, 3);
    static { register("attack_curves.umbral_slash1_c2", UMBRAL_SLASH1_C2, Vector.class,
        v -> UMBRAL_SLASH1_C2 = v, Config::loadVector); }

    // UMBRAL_SLASH1_WINDUP
    public static Vector UMBRAL_SLASH1_WINDUP_START = new Vector(-1.5, 0.17, 1);
    static { register("attack_curves.umbral_slash1_windup_start", UMBRAL_SLASH1_WINDUP_START, Vector.class,
        v -> UMBRAL_SLASH1_WINDUP_START = v, Config::loadVector); }
    public static Vector UMBRAL_SLASH1_WINDUP_END = new Vector(-2.1, -0.33, -0.5);
    static { register("attack_curves.umbral_slash1_windup_end", UMBRAL_SLASH1_WINDUP_END, Vector.class,
        v -> UMBRAL_SLASH1_WINDUP_END = v, Config::loadVector); }
    public static Vector UMBRAL_SLASH1_WINDUP_C1 = new Vector(-2, 0.17, 0.5);
    static { register("attack_curves.umbral_slash1_windup_c1", UMBRAL_SLASH1_WINDUP_C1, Vector.class,
        v -> UMBRAL_SLASH1_WINDUP_C1 = v, Config::loadVector); }
    public static Vector UMBRAL_SLASH1_WINDUP_C2 = new Vector(-2, -0.08, 0);
    static { register("attack_curves.umbral_slash1_windup_c2", UMBRAL_SLASH1_WINDUP_C2, Vector.class,
        v -> UMBRAL_SLASH1_WINDUP_C2 = v, Config::loadVector); }

    // WIDE_UMBRAL_SLASH1
    public static Vector WIDE_UMBRAL_SLASH1_START = new Vector(-5.7615, 0, 2.171);
    static { register("attack_curves.wide_umbral_slash1_start", WIDE_UMBRAL_SLASH1_START, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_START = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH1_END = new Vector(5.845, 0, -0.334);
    static { register("attack_curves.wide_umbral_slash1_end", WIDE_UMBRAL_SLASH1_END, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_END = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH1_C1 = new Vector(-2.505, 0, 3.34);
    static { register("attack_curves.wide_umbral_slash1_c1", WIDE_UMBRAL_SLASH1_C1, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_C1 = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH1_C2 = new Vector(2.505, 0, 5.01);
    static { register("attack_curves.wide_umbral_slash1_c2", WIDE_UMBRAL_SLASH1_C2, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_C2 = v, Config::loadVector); }

    // WIDE_UMBRAL_SLASH1_WINDUP
    public static Vector WIDE_UMBRAL_SLASH1_WINDUP_START = new Vector(-1.66, 0.17, -0.5);
    static { register("attack_curves.wide_umbral_slash1_windup_start", WIDE_UMBRAL_SLASH1_WINDUP_START, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_WINDUP_START = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH1_WINDUP_END = new Vector(-5, 0.27, 0.83);
    static { register("attack_curves.wide_umbral_slash1_windup_end", WIDE_UMBRAL_SLASH1_WINDUP_END, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_WINDUP_END = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH1_WINDUP_C1 = new Vector(-2.5, 1.03, 1.7);
    static { register("attack_curves.wide_umbral_slash1_windup_c1", WIDE_UMBRAL_SLASH1_WINDUP_C1, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_WINDUP_C1 = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH1_WINDUP_C2 = new Vector(-3.77, 0.51, 2.26);
    static { register("attack_curves.wide_umbral_slash1_windup_c2", WIDE_UMBRAL_SLASH1_WINDUP_C2, Vector.class,
        v -> WIDE_UMBRAL_SLASH1_WINDUP_C2 = v, Config::loadVector); }

    // WIDE_UMBRAL_SLASH2
    public static Vector WIDE_UMBRAL_SLASH2_START = new Vector(4.008, -1.002, -1.169);
    static { register("attack_curves.wide_umbral_slash2_start", WIDE_UMBRAL_SLASH2_START, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_START = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH2_END = new Vector(-3.841, 1.67, 4.008);
    static { register("attack_curves.wide_umbral_slash2_end", WIDE_UMBRAL_SLASH2_END, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_END = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH2_C1 = new Vector(1.67, 0, 2.839);
    static { register("attack_curves.wide_umbral_slash2_c1", WIDE_UMBRAL_SLASH2_C1, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_C1 = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH2_C2 = new Vector(-1.336, 0, 5.01);
    static { register("attack_curves.wide_umbral_slash2_c2", WIDE_UMBRAL_SLASH2_C2, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_C2 = v, Config::loadVector); }

    // WIDE_UMBRAL_SLASH2_WINDUP
    public static Vector WIDE_UMBRAL_SLASH2_WINDUP_START = new Vector(5.344, -0.2171, -1.002);
    static { register("attack_curves.wide_umbral_slash2_windup_start", WIDE_UMBRAL_SLASH2_WINDUP_START, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_WINDUP_START = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH2_WINDUP_END = new Vector(4.509, -1.503, -2.338);
    static { register("attack_curves.wide_umbral_slash2_windup_end", WIDE_UMBRAL_SLASH2_WINDUP_END, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_WINDUP_END = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH2_WINDUP_C1 = new Vector(4.7261, 0, -0.501);
    static { register("attack_curves.wide_umbral_slash2_windup_c1", WIDE_UMBRAL_SLASH2_WINDUP_C1, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_WINDUP_C1 = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH2_WINDUP_C2 = new Vector(3.674, -0.4008, -1.5698);
    static { register("attack_curves.wide_umbral_slash2_windup_c2", WIDE_UMBRAL_SLASH2_WINDUP_C2, Vector.class,
        v -> WIDE_UMBRAL_SLASH2_WINDUP_C2 = v, Config::loadVector); }

    // WIDE_UMBRAL_SLASH3
    public static Vector WIDE_UMBRAL_SLASH3_START = new Vector(0, 5.177, -0.334);
    static { register("attack_curves.wide_umbral_slash3_start", WIDE_UMBRAL_SLASH3_START, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_START = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH3_END = new Vector(0, -3.674, 1.336);
    static { register("attack_curves.wide_umbral_slash3_end", WIDE_UMBRAL_SLASH3_END, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_END = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH3_C1 = new Vector(0, 1.7368, 1.67);
    static { register("attack_curves.wide_umbral_slash3_c1", WIDE_UMBRAL_SLASH3_C1, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_C1 = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH3_C2 = new Vector(0, -0.167, 2.9058);
    static { register("attack_curves.wide_umbral_slash3_c2", WIDE_UMBRAL_SLASH3_C2, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_C2 = v, Config::loadVector); }

    // WIDE_UMBRAL_SLASH3_WINDUP
    public static Vector WIDE_UMBRAL_SLASH3_WINDUP_START = new Vector(-3.34, 2.0708, 3.34);
    static { register("attack_curves.wide_umbral_slash3_windup_start", WIDE_UMBRAL_SLASH3_WINDUP_START, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_WINDUP_START = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH3_WINDUP_END = new Vector(0.334, 4.0915, 0);
    static { register("attack_curves.wide_umbral_slash3_windup_end", WIDE_UMBRAL_SLASH3_WINDUP_END, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_WINDUP_END = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH3_WINDUP_C1 = new Vector(1.67, 0, 1.67);
    static { register("attack_curves.wide_umbral_slash3_windup_c1", WIDE_UMBRAL_SLASH3_WINDUP_C1, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_WINDUP_C1 = v, Config::loadVector); }
    public static Vector WIDE_UMBRAL_SLASH3_WINDUP_C2 = new Vector(-1.336, 0, 3.841);
    static { register("attack_curves.wide_umbral_slash3_windup_c2", WIDE_UMBRAL_SLASH3_WINDUP_C2, Vector.class,
        v -> WIDE_UMBRAL_SLASH3_WINDUP_C2 = v, Config::loadVector); }

    // SLASH1
    public static Vector SLASH1_START = new Vector(-2.06, -1.26, -0.5);
    static { register("attack_curves.slash1_start", SLASH1_START, Vector.class,
        v -> SLASH1_START = v, Config::loadVector); }
    public static Vector SLASH1_END = new Vector(3.26, 0.79, -0.4);
    static { register("attack_curves.slash1_end", SLASH1_END, Vector.class,
        v -> SLASH1_END = v, Config::loadVector); }
    public static Vector SLASH1_C1 = new Vector(-2.3, -0.16, 3);
    static { register("attack_curves.slash1_c1", SLASH1_C1, Vector.class,
        v -> SLASH1_C1 = v, Config::loadVector); }
    public static Vector SLASH1_C2 = new Vector(1.9, 0.21, 5);
    static { register("attack_curves.slash1_c2", SLASH1_C2, Vector.class,
        v -> SLASH1_C2 = v, Config::loadVector); }

    // SLASH2
    public static Vector SLASH2_START = new Vector(2.6, -1.3, -1.2);
    static { register("attack_curves.slash2_start", SLASH2_START, Vector.class,
        v -> SLASH2_START = v, Config::loadVector); }
    public static Vector SLASH2_END = new Vector(-3, 0.9, 1.3);
    static { register("attack_curves.slash2_end", SLASH2_END, Vector.class,
        v -> SLASH2_END = v, Config::loadVector); }
    public static Vector SLASH2_C1 = new Vector(1.6, -0.7, 7);
    static { register("attack_curves.slash2_c1", SLASH2_C1, Vector.class,
        v -> SLASH2_C1 = v, Config::loadVector); }
    public static Vector SLASH2_C2 = new Vector(-2.6, 1.05, 1.85);
    static { register("attack_curves.slash2_c2", SLASH2_C2, Vector.class,
        v -> SLASH2_C2 = v, Config::loadVector); }

    // SLASH3
    public static Vector SLASH3_START = new Vector(1.2, 2.8, -1.5);
    static { register("attack_curves.slash3_start", SLASH3_START, Vector.class,
        v -> SLASH3_START = v, Config::loadVector); }
    public static Vector SLASH3_END = new Vector(-1.1, -2.2, -0.9);
    static { register("attack_curves.slash3_end", SLASH3_END, Vector.class,
        v -> SLASH3_END = v, Config::loadVector); }
    public static Vector SLASH3_C1 = new Vector(1, 1.96, 4.3);
    static { register("attack_curves.slash3_c1", SLASH3_C1, Vector.class,
        v -> SLASH3_C1 = v, Config::loadVector); }
    public static Vector SLASH3_C2 = new Vector(-1.1, -1.77, 5);
    static { register("attack_curves.slash3_c2", SLASH3_C2, Vector.class,
        v -> SLASH3_C2 = v, Config::loadVector); }

    // UP_SMASH
    public static Vector UP_SMASH_START = new Vector(0.66, -1.53, -0.5);
    static { register("attack_curves.up_smash_start", UP_SMASH_START, Vector.class,
        v -> UP_SMASH_START = v, Config::loadVector); }
    public static Vector UP_SMASH_END = new Vector(-0.4, 0.67, -0.9);
    static { register("attack_curves.up_smash_end", UP_SMASH_END, Vector.class,
        v -> UP_SMASH_END = v, Config::loadVector); }
    public static Vector UP_SMASH_C1 = new Vector(0.56, -0.89, 2.1);
    static { register("attack_curves.up_smash_c1", UP_SMASH_C1, Vector.class,
        v -> UP_SMASH_C1 = v, Config::loadVector); }
    public static Vector UP_SMASH_C2 = new Vector(-0.4, 1.37, 1.65);
    static { register("attack_curves.up_smash_c2", UP_SMASH_C2, Vector.class,
        v -> UP_SMASH_C2 = v, Config::loadVector); }

    // LUNGE1
    public static Vector LUNGE1_START = new Vector(0.37, 0, 2);
    static { register("attack_curves.lunge1_start", LUNGE1_START, Vector.class,
        v -> LUNGE1_START = v, Config::loadVector); }
    public static Vector LUNGE1_END = new Vector(0, 0, 20);
    static { register("attack_curves.lunge1_end", LUNGE1_END, Vector.class,
        v -> LUNGE1_END = v, Config::loadVector); }
    public static Vector LUNGE1_C1 = new Vector(1.1, 0, 3.1);
    static { register("attack_curves.lunge1_c1", LUNGE1_C1, Vector.class,
        v -> LUNGE1_C1 = v, Config::loadVector); }
    public static Vector LUNGE1_C2 = new Vector(0, 0, 2.46);
    static { register("attack_curves.lunge1_c2", LUNGE1_C2, Vector.class,
        v -> LUNGE1_C2 = v, Config::loadVector); }

    // F_DASH_ATTACK
    public static Vector F_DASH_ATTACK_START = new Vector(-1.95, -0.76, 0.9);
    static { register("attack_curves.f_dash_attack_start", F_DASH_ATTACK_START, Vector.class,
        v -> F_DASH_ATTACK_START = v, Config::loadVector); }
    public static Vector F_DASH_ATTACK_END = new Vector(1.2, 1.1, 7.3);
    static { register("attack_curves.f_dash_attack_end", F_DASH_ATTACK_END, Vector.class,
        v -> F_DASH_ATTACK_END = v, Config::loadVector); }
    public static Vector F_DASH_ATTACK_C1 = new Vector(-1.6, -0.57, 2.7);
    static { register("attack_curves.f_dash_attack_c1", F_DASH_ATTACK_C1, Vector.class,
        v -> F_DASH_ATTACK_C1 = v, Config::loadVector); }
    public static Vector F_DASH_ATTACK_C2 = new Vector(-0.93, 0, 4.9);
    static { register("attack_curves.f_dash_attack_c2", F_DASH_ATTACK_C2, Vector.class,
        v -> F_DASH_ATTACK_C2 = v, Config::loadVector); }

    // B_DASH_ATTACK
    public static Vector B_DASH_ATTACK_START = new Vector(0.696, 2.2388, 1.74);
    static { register("attack_curves.b_dash_attack_start", B_DASH_ATTACK_START, Vector.class,
        v -> B_DASH_ATTACK_START = v, Config::loadVector); }
    public static Vector B_DASH_ATTACK_END = new Vector(-0.8932, -3.2016, 0.116);
    static { register("attack_curves.b_dash_attack_end", B_DASH_ATTACK_END, Vector.class,
        v -> B_DASH_ATTACK_END = v, Config::loadVector); }
    public static Vector B_DASH_ATTACK_C1 = new Vector(0.3132, 0.4176, 2.204);
    static { register("attack_curves.b_dash_attack_c1", B_DASH_ATTACK_C1, Vector.class,
        v -> B_DASH_ATTACK_C1 = v, Config::loadVector); }
    public static Vector B_DASH_ATTACK_C2 = new Vector(-0.58, -1.74, 1.3572);
    static { register("attack_curves.b_dash_attack_c2", B_DASH_ATTACK_C2, Vector.class,
        v -> B_DASH_ATTACK_C2 = v, Config::loadVector); }

    // R_STRAFE_ATTACK
    public static Vector R_STRAFE_ATTACK_START = new Vector(-1.503, -0.4008, 0.668);
    static { register("attack_curves.r_strafe_attack_start", R_STRAFE_ATTACK_START, Vector.class,
        v -> R_STRAFE_ATTACK_START = v, Config::loadVector); }
    public static Vector R_STRAFE_ATTACK_END = new Vector(4.676, 0.334, 1.169);
    static { register("attack_curves.r_strafe_attack_end", R_STRAFE_ATTACK_END, Vector.class,
        v -> R_STRAFE_ATTACK_END = v, Config::loadVector); }
    public static Vector R_STRAFE_ATTACK_C1 = new Vector(0.167, 0, 1.4362);
    static { register("attack_curves.r_strafe_attack_c1", R_STRAFE_ATTACK_C1, Vector.class,
        v -> R_STRAFE_ATTACK_C1 = v, Config::loadVector); }
    public static Vector R_STRAFE_ATTACK_C2 = new Vector(2.2211, 0, 1.7702);
    static { register("attack_curves.r_strafe_attack_c2", R_STRAFE_ATTACK_C2, Vector.class,
        v -> R_STRAFE_ATTACK_C2 = v, Config::loadVector); }

    // L_STRAFE_ATTACK
    public static Vector L_STRAFE_ATTACK_START = new Vector(1.503, -0.4008, 0.668);
    static { register("attack_curves.l_strafe_attack_start", L_STRAFE_ATTACK_START, Vector.class,
        v -> L_STRAFE_ATTACK_START = v, Config::loadVector); }
    public static Vector L_STRAFE_ATTACK_END = new Vector(-4.676, 0.334, 1.169);
    static { register("attack_curves.l_strafe_attack_end", L_STRAFE_ATTACK_END, Vector.class,
        v -> L_STRAFE_ATTACK_END = v, Config::loadVector); }
    public static Vector L_STRAFE_ATTACK_C1 = new Vector(-0.167, 0, 1.4362);
    static { register("attack_curves.l_strafe_attack_c1", L_STRAFE_ATTACK_C1, Vector.class,
        v -> L_STRAFE_ATTACK_C1 = v, Config::loadVector); }
    public static Vector L_STRAFE_ATTACK_C2 = new Vector(-2.2211, 0, 1.7702);
    static { register("attack_curves.l_strafe_attack_c2", L_STRAFE_ATTACK_C2, Vector.class,
        v -> L_STRAFE_ATTACK_C2 = v, Config::loadVector); }

    // D_AIR
    public static Vector D_AIR_START = new Vector(-0.35, 2.53, 0.56);
    static { register("attack_curves.d_air_start", D_AIR_START, Vector.class,
        v -> D_AIR_START = v, Config::loadVector); }
    public static Vector D_AIR_END = new Vector(0, -3.42, -0.581);
    static { register("attack_curves.d_air_end", D_AIR_END, Vector.class,
        v -> D_AIR_END = v, Config::loadVector); }
    public static Vector D_AIR_C1 = new Vector(0.329, -0.165, 4.97);
    static { register("attack_curves.d_air_c1", D_AIR_C1, Vector.class,
        v -> D_AIR_C1 = v, Config::loadVector); }
    public static Vector D_AIR_C2 = new Vector(-0.07, -6.15, 0.98);
    static { register("attack_curves.d_air_c2", D_AIR_C2, Vector.class,
        v -> D_AIR_C2 = v, Config::loadVector); }

    // N_AIR
    public static Vector N_AIR_START = new Vector(1.0961, 1.742, -1.13);
    static { register("attack_curves.n_air_start", N_AIR_START, Vector.class,
        v -> N_AIR_START = v, Config::loadVector); }
    public static Vector N_AIR_END = new Vector(0, -1.987, -0.791);
    static { register("attack_curves.n_air_end", N_AIR_END, Vector.class,
        v -> N_AIR_END = v, Config::loadVector); }
    public static Vector N_AIR_C1 = new Vector(-0.2825, 0.951, 9.153);
    static { register("attack_curves.n_air_c1", N_AIR_C1, Vector.class,
        v -> N_AIR_C1 = v, Config::loadVector); }
    public static Vector N_AIR_C2 = new Vector(-0.7458, -5.151, -1.808);
    static { register("attack_curves.n_air_c2", N_AIR_C2, Vector.class,
        v -> N_AIR_C2 = v, Config::loadVector); }
}
