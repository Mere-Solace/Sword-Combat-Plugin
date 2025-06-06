package btm.sword.combat.appliedEffect;

import btm.sword.system.entity.Combatant;
import btm.sword.system.entity.SwordEntity;

import java.util.HashSet;

public abstract class AppliedEffect {
	public abstract void applyEffect(Combatant executor, HashSet<SwordEntity> targets);
}
