package btm.sword.system.entity.umbral;

import btm.sword.system.action.utility.thrown.ThrownItem;
import btm.sword.system.attack.Attack;
import btm.sword.system.entity.base.SwordEntity;
import btm.sword.system.entity.types.Combatant;
import btm.sword.util.display.DisplayUtil;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

// while flying and attacking on its own, no soulfire is reaped on attacks
// while in hand, higher soulfire intake on hit



@Getter
@Setter
public class UmbralBlade extends ThrownItem {
    private final Combatant wielder;
    private ItemDisplay weaponDisplay;
    private final Attack[] basicAttacks;
    private final Attack[] heavyAttacks;
    private UmbralState state = UmbralState.SHEATHED;

    private ItemStack weapon;

    private long lastActionTime = 0;
    private Location lastTargetLocation;

    Predicate<UmbralBlade> endHoverPredicate;

    public UmbralBlade(Combatant wielder, ItemStack weapon) {
        super(wielder, display -> {
            display.setItemStack(weapon);
        });
        this.wielder = wielder;
        this.weapon = weapon;
        this.basicAttacks = loadBasicAttacks();
        this.heavyAttacks = loadHeavyAttacks();
        this.endHoverPredicate = umbralBlade -> !umbralBlade.getState().equals(UmbralState.STANDBY);
    }

    public void setState(UmbralState newState) throws InterruptedException {
        this.state = newState;
        switch (newState) {
            case SHEATHED -> returnToSheath();
            case STANDBY -> hoverBehindWielder();
            case ATTACKING -> weaponDisplay.remove();
        }
    }

    public void hoverBehindWielder() {
        state = UmbralState.STANDBY;
        // follows player shoulder position smoothly
        DisplayUtil.itemDisplayFollowLerp(wielder, weaponDisplay,
                new Vector(-0.5, 1.2, 0.6),
                10, 3, endHoverPredicate, this);
    }

    public void returnToSheath() throws InterruptedException {
        state = UmbralState.RECALLING;
        BukkitTask lerpTask = DisplayUtil.itemDisplayFollowSmoothly(wielder, weaponDisplay, new Vector(), 10, 3.0, false);
        assert lerpTask != null;
        lerpTask.wait();
        // logic upon arrival:

    }

    public void performAttack(SwordEntity target, boolean heavy) {

    }

    public void lungeToTarget(SwordEntity target) {
        if (target == null) return;
        Location start = weaponDisplay.getLocation();
        Location end = target.entity().getLocation().clone().add(0, 1, 0);
        DisplayUtil.smoothTeleport(this.weaponDisplay, 6);
        // optional: particle trail + impalement call
    }

    public void recall() throws InterruptedException {
        // if too far or idle too long, return automatically
        returnToSheath();
    }

    private Attack[] loadBasicAttacks() {
        // load from config or registry later
        return new Attack[]{};
    }

    private Attack[] loadHeavyAttacks() {
        return new Attack[]{};
    }

    public void removeWeaponDisplay() {
        if (weaponDisplay != null)
            weaponDisplay.remove();
    }

    public void dispose() {
        wielder.setUmbralBladeActive(false);
        removeWeaponDisplay();
    }
}
