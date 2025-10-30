package btm.sword.system.action.skill;

import btm.sword.system.entity.Combatant;
import btm.sword.util.sound.SoundType;
import java.util.List;
import java.util.function.Function;
import org.bukkit.util.Vector;

// a node in a linked list of sequentially executed steps of an attack
public class SkillNode {
    private long castDuration; // -1 if indeterminate
    private double mainDamageVal;
    private double rangeMultiplier;
    private boolean orientWithPitch;
    private Function<Combatant, List<Vector>> shapeOutlineVectors;
    // using the shapeOutlineVectors:
    // aesthetic vectors are auxiliary shapes decorating the base shape
    private Function<List<Vector>, List<Vector>> aestheticVectors;
    //
    private Function<List<Vector>, List<Vector>> shapeRayTraceVectors;

    private int period;
    private int stepsPerIteration;
    private Function<Combatant, Vector> selfVelocity;
    // The list of vectors will contain 4 elements:
    // a vector from the combatant to the target,
    // and the 3 basis vectors
    private Function<List<Vector>, Vector> entityVelocity;


    private List<SoundType> castSounds;
    private List<SoundType> hitSounds;

    private boolean finished;


}
