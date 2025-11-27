package btm.sword.system.attack;

import java.util.function.Function;

import org.bukkit.util.Vector;

import btm.sword.util.math.ControlVectors;

public class GeneratedAttackProfile implements AttackProfile {
    private final ControlVectors controlVectors;
    private final Function<Attack, Vector> knockBackFunction;

    public GeneratedAttackProfile(ControlVectors controlVectors, Function<Attack, Vector> knockBackFunction) {
        this.controlVectors = controlVectors;
        this.knockBackFunction = knockBackFunction;
    }

    @Override
    public ControlVectors controlVectors() {
        return controlVectors;
    }

    @Override
    public Function<Attack, Vector> knockbackFunction() {
        return knockBackFunction;
    }
}
