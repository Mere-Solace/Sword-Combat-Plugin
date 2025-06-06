package btm.sword.combat.attack;

import btm.sword.Sword;
import btm.sword.effect.EffectManager;
import btm.sword.system.entity.Combatant;

import org.bukkit.entity.LivingEntity;


import java.util.HashSet;

public abstract class Attack implements Runnable {
	private final AttackManager attackManager;
	private final EffectManager effectManager;
	private int delayTicks = 0;
	private int periodTicks = 20;
	private int iterations = 1;

	private boolean done = false;
	private boolean running = false;

	protected Combatant executor;

	protected HashSet<LivingEntity> targets = null;

	public Attack(AttackManager attackManager) {
		this.attackManager = attackManager;
		effectManager = attackManager.getEffectManager();
	}

	public Attack(AttackManager attackManager, int delayTicks) {
		this(attackManager);
		this.delayTicks = delayTicks;
	}

	public Attack(AttackManager attackManager, int delayTicks, int periodTicks) {
		this(attackManager, delayTicks);
		this.periodTicks = periodTicks;
	}

	public Attack(AttackManager attackManager, int delayTicks, int periodTicks, int iterations) {
		this(attackManager, delayTicks, periodTicks);
		this.iterations = iterations;
	}

	public int getDelayTicks() {
		return delayTicks;
	}

	public int getPeriodTicks() {
		return periodTicks;
	}

	public abstract void onRun();

	@Override
	public void run() {
		Sword.getInstance().getLogger().info("Attack Run Method");
		Sword.getInstance().getLogger().info("done = " + done);
		if (done) {
			Sword.getInstance().getLogger().info("Attack done");
			attackManager.removeAttack(this);
			return;
		}
		Sword.getInstance().getLogger().info("done = " + done);
		running = true;

		onRun();

		iterations--;
		if (iterations < 1) done();
	}

	public void done() {
		done = true;
		running = false;
		attackManager.done(this);
		onDone();
	}

	public void onDone() {

	}
}
