package btm.sword.system.attack;

import java.util.function.Supplier;

public class HitValuePacket {
    private final Supplier<Float> reapedSoulfire;
    private final Supplier<Integer> invulnerableTicks;
    private final Supplier<Integer> shardDamage;
    private final Supplier<Float> toughnessDamage;
    private final Supplier<Float> soulfireLoss;

    public HitValuePacket(
        Supplier<Float> reapedSoulfire,
        Supplier<Integer> invulnerableTicks,
        Supplier<Integer> shardDamage,
        Supplier<Float> toughnessDamage,
        Supplier<Float> soulfireLoss) {
        this.reapedSoulfire = reapedSoulfire;
        this.invulnerableTicks = invulnerableTicks;
        this.shardDamage = shardDamage;
        this.toughnessDamage = toughnessDamage;
        this.soulfireLoss = soulfireLoss;
    }

    public float reapedSoulfire() {
        return reapedSoulfire.get();
    }

    public int invulnerableTicks() {
        return invulnerableTicks.get();
    }

    public int shardDamage() {
        return shardDamage.get();
    }

    public float toughnessDamage() {
        return toughnessDamage.get();
    }

    public float soulfireLoss() {
        return soulfireLoss.get();
    }
}
