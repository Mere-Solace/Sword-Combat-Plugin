package btm.sword.combat.attack;

import btm.sword.Sword;
import btm.sword.combat.appliedEffect.AppliedEffect;
import btm.sword.effect.Effect;
import btm.sword.effect.EffectManager;
import btm.sword.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public abstract class Attack implements Runnable, Cloneable {
	private final AttackManager attackManager;
	private final EffectManager effectManager;
	protected List<Effect> effects = new LinkedList<>();
	protected List<AppliedEffect> appliedEffects = new LinkedList<>();
	private int delayTicks = 0;
	private int periodTicks = 20;
	private int iterations = 1;
	public boolean requiresTargets = false;
	
	private boolean done = false;
	private boolean running = false;
	private boolean runNext = true;
	private Attack next = null;
	
	protected PlayerData playerData;
	protected Player executor;
	
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
	
	public Attack(AttackManager attackManager, int delayTicks, int periodTicks, int iterations, PlayerData playerData) {
		this(attackManager, delayTicks, periodTicks, iterations);
		this.playerData = playerData;
		executor = Bukkit.getPlayer(playerData.getUUID());
	}
	
	public void add(Attack nextAttack) {
		if (next == null) next = nextAttack;
		else this.next.add(nextAttack);
	}
	
	public void setPlayerData(PlayerData playerData) {
		this.playerData = playerData;
		executor = Bukkit.getPlayer(playerData.getUUID());
		if (next == null) return;
		next.setPlayerData(playerData);
	}
	
	public int getDelayTicks() {
		return delayTicks;
	}
	
	public int getPeriodTicks() {
		return periodTicks;
	}
	
	public void setTargets(HashSet<LivingEntity> targets) {
		this.targets = targets;
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
		if (next == null || !runNext)
			return;
		
		if (next.requiresTargets)
			next.setTargets(targets);
		
		next.setPlayerData(playerData);
		
		attackManager.start(next);
	}
}
