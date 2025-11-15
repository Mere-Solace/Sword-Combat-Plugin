package btm.sword.system.attack;

import java.util.List;

import org.bukkit.util.Vector;

import btm.sword.util.Prefab;

public enum AttackType {
    // TODO: Associate knockback vector calculation function/Consumer with the ctrl vecs.

    UMBRAL_SLASH1(List.of(
        new Vector(-2,0,0),
        new Vector(2,0,-1),
        new Vector(-1.5,0,2),
        new Vector(1.5,0,3)
    )),
    UMBRAL_SLASH1_WINDUP(List.of(
        new Vector(-1.5,0.17,1),
        new Vector(-2.1,-0.33,-0.5),
        new Vector(-2,0.17,0.5),
        new Vector(-2,-0.08,0)
    )),

    WIDE_UMBRAL_SLASH1(List.of(
        new Vector(-5.7615,0,2.171),
        new Vector(5.845,0,-0.334),
        new Vector(-2.505,0,3.34),
        new Vector(2.505,0,5.01)
    )),
    WIDE_UMBRAL_SLASH1_WINDUP(List.of(
        new Vector(-1.66,0.17,-0.5),
        new Vector(-5,0.27,0.83),
        new Vector(-2.5,1.03,1.7),
        new Vector(-3.77,0.51,2.26)
    )),



    SLASH1(List.of(
            new Vector(-2.06, -1.26, -0.5),
            new Vector(3.26, 0.79, -0.4),
            new Vector(-2.3, -0.16,3),
            new Vector(1.9, 0.21, 5)
    )),
    SLASH2(List.of(
        new Vector(2.6, -1.21, -1.2),
            new Vector(-1.47, 1.99, 0),
            new Vector(1.6, -0.11, 7),
            new Vector(-3.66, 0.26, 1.85)
    )),
    SLASH3(List.of(
        new Vector(-0.15,2.8,-1.5),
            new Vector(-1.1,-2.2,-0.9),
            new Vector(1.74,1.96,4.3),
            new Vector(-1.1,-1.77,5)
    )),

    UP_SMASH(List.of(
        new Vector(0.66,-1.53,-0.5),
        new Vector(-0.4,0.67,-0.9),
        new Vector(0.56,-0.89,2.1),
        new Vector(-0.4,1.37,1.65)
    )),

//    LUNGE1(Prefab.ControlVectors.UP_SMASH),

    D_AIR(List.of(
        new Vector(-0.35, 2.53, 0.56),
        new Vector(0, -3.42, -0.581),
        new Vector(0.329, -0.165, 4.97),
        new Vector(-0.07, -6.15, 0.98)
    )),
    N_AIR(List.of(
        new Vector(1.0961, 1.742, -1.13),
        new Vector(0, -1.987, -0.791),
        new Vector(-0.2825, 0.951, 9.153),
        new Vector(-0.7458, -5.151, -1.808)
    )),

    R_SIDESTEP(List.of(
        new Vector(-1.3,1.03,2),
        new Vector(8.2,1.03,-1.9),
        new Vector(-7,-1.73,3.3),
        new Vector(9,-0.93,5)
    )),

    L_SIDESTEP(List.of(
        new Vector(1.3,1.03,2),
        new Vector(-8.2,1.03,-1.9),
        new Vector(7,-1.73,3.3),
        new Vector(-9,-0.93,5)
    )),

    DASH(List.of(
        Prefab.Direction.UP,
        Prefab.Direction.DOWN
    )),

    DEFAULT(List.of(
        Prefab.Direction.UP,
        Prefab.Direction.DOWN,
        Prefab.Direction.OUT_UP,
        Prefab.Direction.OUT_DOWN
    ));

    private final List<Vector> controlVectors;

    AttackType(List<Vector> ctrlVectors) {
        this.controlVectors = ctrlVectors;
    }

    public List<Vector> controlVectors() {
        return controlVectors;
    }
}
