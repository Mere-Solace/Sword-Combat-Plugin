package btm.sword.system.action.skill;

import java.util.List;

public final class SkillIds {

    private SkillIds() {}

    // Remember to add new skills to the ALL list

    /* =========================
       Helpful Skill Classifications
       ========================= */
    public static final SkillId NONE =
        SkillId.of("sword", "none");

    public static final SkillId LOCKED =
        SkillId.of("sword", "locked");


    /* =========================
       Umbral Blade Skills
       ========================= */

    public static final SkillId SHADOW_SLASH =
        SkillId.of("sword", "umbral_blade.shadow_slash");

    public static final SkillId VOID_LUNGE =
        SkillId.of("sword", "umbral_blade.void_lunge");


    /* =========================
       Active Skills
       ========================= */

    public static final SkillId KNIFE_THROW =
        SkillId.of("sword", "active.knife_throw");


    /* =========================
       Passive Skills
       ========================= */

    public static final SkillId BLEED_MASTERY =
        SkillId.of("sword", "passive.bleed_mastery");


    // Remember to add new skills to this list!
    private static final List<SkillId> ALL = List.of(
        NONE,
        LOCKED,
        SHADOW_SLASH,
        VOID_LUNGE,
        KNIFE_THROW,
        BLEED_MASTERY
    );

    public static List<SkillId> getAll() {
        return ALL;
    }
}
