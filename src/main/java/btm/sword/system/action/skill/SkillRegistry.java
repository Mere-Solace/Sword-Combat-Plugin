package btm.sword.system.action.skill;

import java.util.HashMap;

import btm.sword.system.action.skill.type.impl.active.KnifeThrowAbility;
import btm.sword.system.action.skill.type.impl.active.TestAlphaAbility;
import btm.sword.system.action.skill.type.impl.active.TestBetaAbility;
import btm.sword.system.action.skill.type.impl.active.TestGammaAbility;
import btm.sword.system.action.skill.type.impl.umbral.ShadowSlashSkill;
import btm.sword.system.action.skill.type.impl.umbral.VoidLungeSkill;

public class SkillRegistry {
    public static final HashMap<SkillId, Skill> skillMapping = new HashMap<>();

    // No Bukkit/Paper API features are used here, so we can use a static call instead of a call in on Enable
    static {
        register(new VoidLungeSkill());
        register(new ShadowSlashSkill());
        register(new KnifeThrowAbility());
        // TODO: remove once real found abilities replace test stubs
        register(new TestAlphaAbility());
        register(new TestBetaAbility());
        register(new TestGammaAbility());
    }

    private static void register(Skill skill) {
        skillMapping.put(skill.id(), skill);
    }

    public static Skill get(SkillId id) {
        return skillMapping.get(id);
    }
}
