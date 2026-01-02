package btm.sword.system.action.skill.type;

import btm.sword.system.action.skill.Skill;
import btm.sword.system.entity.impl.Combatant;

public abstract class ActiveSkill implements Skill {

    public abstract void execute(Combatant combatant);
    public abstract int calculateCooldown(Combatant combatant);
    public abstract boolean canPerform(Combatant combatant);
}
