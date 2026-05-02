package btm.sword.action.skill;

import java.util.HashMap;

import btm.sword.action.skill.type.impl.active.IceSpellAbility;
import btm.sword.action.skill.type.impl.active.KnifeThrowAbility;
import btm.sword.action.skill.type.impl.active.TestAlphaAbility;
import btm.sword.action.skill.type.impl.active.TestBetaAbility;
import btm.sword.action.skill.type.impl.active.TestGammaAbility;
import btm.sword.action.skill.type.impl.umbral.ShadowSlashSkill;
import btm.sword.action.skill.type.impl.umbral.VoidLungeSkill;

/**
 * Static registry mapping every known {@link SkillId} to its {@link Skill} implementation.
 *
 * <p>All skills are registered in the {@code static} initialiser so they are available as soon
 * as the class is loaded, without requiring an explicit {@code onEnable} hook. Pass a
 * {@link SkillId} to {@link #get(SkillId)} to retrieve the corresponding implementation.</p>
 */
public final class SkillRegistry {

    private SkillRegistry() {}

    /** The backing map; populated by the static initialiser. */
    public static final HashMap<SkillId, Skill> SKILL_MAPPING = new HashMap<>();

    // No Bukkit/Paper API features are used here, so we can use a static call instead of a call in on Enable
    static {
        register(new VoidLungeSkill());
        register(new ShadowSlashSkill());
        register(new KnifeThrowAbility());
        register(new IceSpellAbility());
        // TODO: remove once real found abilities replace test stubs
        register(new TestAlphaAbility());
        register(new TestBetaAbility());
        register(new TestGammaAbility());
    }

    private static void register(Skill skill) {
        SKILL_MAPPING.put(skill.id(), skill);
    }

    /**
     * Looks up the skill for the given ID.
     *
     * @param id the skill ID to look up
     * @return the registered {@link Skill}, or {@code null} if not found
     */
    public static Skill get(SkillId id) {
        return SKILL_MAPPING.get(id);
    }
}
