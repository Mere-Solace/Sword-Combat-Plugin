package btm.sword.combat.style;

import java.util.HashMap;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import btm.sword.item.core.KeyRegistry;

/** Defines the attack style of a weapon, mapping it to a set of combo and situational attack profiles. */
public enum WeaponAttackStyle {
    PUNCH("punch"),
    SLASH("slash", List.of(
        AttackType.SLASH1,
        AttackType.SLASH2,
        AttackType.SLASH3),
        AttackType.N_AIR,
        AttackType.D_AIR,
        AttackType.F_DASH_ATTACK,
        AttackType.B_DASH_ATTACK,
        AttackType.R_STRAFE_ATTACK,
        AttackType.L_STRAFE_ATTACK
    ),
    THRUST("thrust"),
    BASH("bash");


    private final String string;
    private final List<AttackProfile> attackChain;
    private final AttackProfile neutralAirAttack;
    private final AttackProfile downAirAttack;
    private final AttackProfile forwardDashAttack;
    private final AttackProfile backwardDashAttack;
    private final AttackProfile rightStrafeAttack;
    private final AttackProfile leftStrafeAttack;

    private static final HashMap<String, WeaponAttackStyle> MAP_TO_ENUM = new HashMap<>();

    static {
        for (WeaponAttackStyle type : values()) {
            MAP_TO_ENUM.put(type.toString().toLowerCase(), type);
        }
    }

    WeaponAttackStyle(String string,
                      List<AttackProfile> attackChain,
                      AttackProfile neutralAirAttack,
                      AttackProfile downAirAttack,
                      AttackProfile forwardDashAttack,
                      AttackProfile backwardDashAttack,
                      AttackProfile rightStrafeAttack,
                      AttackProfile leftStrafeAttack) {
        this.string = string;
        this.attackChain = attackChain;
        this.neutralAirAttack = neutralAirAttack;
        this.downAirAttack = downAirAttack;
        this.forwardDashAttack = forwardDashAttack;
        this.backwardDashAttack = backwardDashAttack;
        this.rightStrafeAttack = rightStrafeAttack;
        this.leftStrafeAttack = leftStrafeAttack;
    }

    WeaponAttackStyle(String string) { // only for punch
        this(string,
            null, null, null,
            null, null, null, null);
    }

    /** Returns the lowercase string key used to look up this style from persistent data. */
    public String string() {
        return string;
    }

    /** Returns the ordered list of ground combo attack profiles. */
    public List<AttackProfile> attacks() {
        return attackChain;
    }

    /** Returns the neutral air attack profile. */
    public AttackProfile neutralAir() {
        return neutralAirAttack;
    }

    /** Returns the down air attack profile. */
    public AttackProfile downAir() {
        return downAirAttack;
    }

    /** Returns the forward dash attack profile. */
    public AttackProfile fDash() {
        return forwardDashAttack;
    }

    /** Returns the backward dash attack profile. */
    public AttackProfile bDash() {
        return backwardDashAttack;
    }

    /** Returns the right strafe attack profile. */
    public AttackProfile rStrafe() {
        return rightStrafeAttack;
    }

    /** Returns the left strafe attack profile. */
    public AttackProfile lStrafe() {
        return leftStrafeAttack;
    }

    /** Resolves the attack style from the item's persistent data, defaulting to PUNCH if not found. */
    public static WeaponAttackStyle fromString(ItemStack item) {
        return MAP_TO_ENUM.getOrDefault(KeyRegistry.getKeyField(item, KeyRegistry.ATTACK_STYLE_KEY, PersistentDataType.STRING), PUNCH);
    }
}
