package btm.sword.system.attack.def;

import btm.sword.system.attack.simulation.CapsuleVolume;
import btm.sword.system.attack.simulation.ObbVolume;
import btm.sword.system.attack.simulation.Volume;

/**
 * Classifies the collision primitive used by an {@link AttackDef}.
 * Determines which {@link Volume} subtype is allocated when the attack is activated.
 */
public enum AttackPrimitive {

    /** Oriented bounding box volume — used with {@code KeyframedTrajectory}. Allocates {@link ObbVolume}. */
    VOLUME {
        @Override
        public Volume createVolume() {
            return new ObbVolume();
        }
    },

    /** Capsule sweep volume — used with {@code SweepTrajectory}. Allocates {@link CapsuleVolume}. */
    SWEEP {
        @Override
        public Volume createVolume() {
            return new CapsuleVolume();
        }
    };

    /**
     * Allocates a fresh {@link Volume} buffer appropriate for this primitive type.
     *
     * @return a new, empty volume buffer
     */
    public abstract Volume createVolume();
}
