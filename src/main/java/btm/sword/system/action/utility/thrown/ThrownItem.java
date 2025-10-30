package btm.sword.system.action.utility.thrown;

import btm.sword.Sword;
import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;
import btm.sword.system.entity.SwordEntityArbiter;
import btm.sword.system.entity.SwordPlayer;
import btm.sword.util.*;
import btm.sword.util.sound.SoundType;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Represents a thrown item entity that is actively simulated in the world.
 * <p>
 * Handles all aspects of the throw lifecycle — initialization, motion physics,
 * collision detection, and interaction outcomes such as hitting entities,
 * embedding in blocks, or being caught.
 */
@Getter
@Setter
public class ThrownItem {
	private final ItemDisplay display;
	private final Combatant thrower;
	private final ParticleWrapper blockTrail;
	
	private float xDisplayOffset;
	private float yDisplayOffset;
	private float zDisplayOffset;
	
	private Location origin;
	private Location cur;
	private Location prev;
	private Vector velocity;
	private double initialVelocity;
	
	int t = 0;
	private Function<Integer, Vector> positionFunction;
	private Function<Integer, Vector> velocityFunction;

	private boolean grounded;
	private Block stuckBlock;
	
	private boolean hit;
	private SwordEntity hitEntity;
	
	private boolean caught;
	
	private BukkitTask disposeTask;

    /**
     * Constructs a new thrown item instance with the given thrower and display.
     *
     * @param thrower The entity performing the throw.
     * @param display The {@link ItemDisplay} representing the thrown object.
     */
	public ThrownItem(Combatant thrower, ItemDisplay display) {
		this.thrower = thrower;
		this.display = display;
		
		blockTrail = display.getItemStack().getType().isBlock() ?
				new ParticleWrapper(Particle.BLOCK, 5, 0.25,  0.25,  0.25, display.getItemStack().getType().createBlockData()) :
				null;
		
		if (thrower instanceof SwordPlayer sp) {
			sp.setMainHandItemStackDuringThrow(sp.getMainItemStackAtTimeOfHold());
			sp.setOffHandItemStackDuringThrow(sp.getOffItemStackAtTimeOfHold());
		}
		else {
            thrower.setMainHandItemStackDuringThrow(thrower.getItemStackInHand(true));
            thrower.setOffHandItemStackDuringThrow(thrower.getItemStackInHand(false));
		}
        // Base values for where the ItemDisplay is held in relation to the player's eye location
		xDisplayOffset = -0.5f;
		yDisplayOffset = 0.1f;
		zDisplayOffset = 0.5f;
	}

    /**
     * Called when the item is primed to be thrown (held ready but not yet released).
     * <p>
     * Manages visual positioning, cancels premature throws, and displays in-hand effects.
     */
	public void onReady() {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(false);
            sp.setThrownItemIndex();

            // Interacting with an entity will cause the shield holding mechanic to falter
            if (sp.isInteractingWithEntity()) {
                sp.setAttemptingThrow(false);
                sp.setThrowSuccessful(true);
                // this throw should be weaker because it's automatic. Could turn into a lunge or thrust or smth else
                sp.getThrownItem().onRelease(2);
                thrower.setItemTypeInHand(Material.AIR, true);
                sp.endHoldingRight();
                sp.resetTree();
                return;
            }
        }

        determineOrientation();

		LivingEntity ex = thrower.entity();
		
		new BukkitRunnable() {
			int i = 0;
			@Override
			public void run() {
				if (thrower.isThrowCancelled()) {
					display.remove();
					ThrowAction.throwCancel(thrower);
					thrower.setThrownItem(null);
					cancel();
					return;
				}
				else if (thrower.isThrowSuccessful()) {
					thrower.setItemTypeInHand(Material.AIR, true);
//					ItemStack toReturn = thrower.getOffHandItemStackDuringThrow();
//					thrower.setItemStackInHand(toReturn, false);
					cancel();
					return;
				}
				
				if (thrower instanceof SwordPlayer sp) {
					if (!sp.isChangingHandIndex() && sp.getCurrentInvIndex() == sp.getThrownItemIndex()) {
						if (i < 10)
							sp.itemNameDisplay("- HURL IT AT 'EM SOLDIER! -", TextColor.color(100, 100, 100), null);
						else
							sp.itemNameDisplay("| HURL IT AT 'EM SOLDIER! |", TextColor.color(150, 150, 150), null);
						
						if (i > 20) i = 0;
						i++;
					}
				}
				
				ex.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 1, 2));

                DisplayUtil.smoothTeleport(display, 2);
                display.teleport(ex.getEyeLocation());

			}
		}.runTaskTimer(Sword.getInstance(), 0L, 1L);
	}

    /**
     * Called when the item is released (actually thrown).
     * <p>
     * Initializes trajectory, physics parameters, and continuous motion updates.
     *
     * @param initialVelocity The starting velocity magnitude of the throw.
     */
	public void onRelease(double initialVelocity) {
        if (thrower instanceof SwordPlayer sp) {
            sp.setThrewItem(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    sp.setThrewItem(false);
                }
            }.runTaskLater(Sword.getInstance(), 2);
        }

        SoundUtil.playSound(thrower.entity(), SoundType.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 0.4f);

        thrower.setItemStackInHand(ItemStack.of(Material.AIR), true);
		InteractiveItemArbiter.put(this);
		xDisplayOffset = yDisplayOffset = zDisplayOffset = 0;
		determineOrientation();

		this.initialVelocity = initialVelocity;
		LivingEntity ex = thrower.entity();
		Location o = ex.getEyeLocation();
		List<Vector> basis = VectorUtil.getBasisWithoutPitch(o);
		double phi = Math.toRadians(-1 * o.getPitch());
		double cosPhi = Math.cos(phi);
		double sinPhi = Math.sin(phi);
		double forwardCoefficient = initialVelocity*cosPhi;
		double upwardCoefficient = initialVelocity*sinPhi;
		origin = o.add(basis.getFirst().multiply(0.5))
				.add(basis.get(1).multiply(0.1))
				.add(basis.getLast().multiply(-0.25));
		cur = origin.clone();
		prev = cur.clone();
		Vector flatDir = thrower.getFlatDir().rotateAroundY(Math.PI/85);
		velocity = flatDir.clone();
		Vector forwardVelocity = flatDir.clone().multiply(forwardCoefficient);
		Vector upwardVelocity = VectorUtil.UP.clone().multiply(upwardCoefficient);
		
		double gravDamper = 46;
		
		positionFunction = t -> flatDir.clone().multiply(forwardCoefficient*t)
				.add(VectorUtil.UP.clone().multiply((upwardCoefficient*t)-(initialVelocity*(1/gravDamper)*t*t)));
		
		velocityFunction = t -> forwardVelocity.clone()
				.add(upwardVelocity.clone().add(VectorUtil.UP.clone().multiply(-initialVelocity*(2/(gravDamper))*t)));
		
		new BukkitRunnable() {
			@Override
			public void run() {
				if (grounded || hit || caught || display.isDead()) {
					onEnd();
					cancel();
					return;
				}
				
				cur = origin.clone().add(positionFunction.apply(t));
				velocity = velocityFunction.apply(t);

                if (!prev.equals(cur) && cur.clone().subtract(prev).toVector().dot(velocity) > 0) {
                    DisplayUtil.smoothTeleport(display, 1);
                }

                String name = display.getItemStack().getType().toString();
                if (name.endsWith("_SWORD")) {
                    display.teleport(cur.setDirection(velocity));
                }
                else {
                    display.teleport(cur.setDir