package btm.sword.action.skill;

import java.util.List;

/**
 * Catalogue of all known {@link SkillId} constants.
 *
 * <p>When adding a new skill, define its {@link SkillId} here and include it in
 * {@link #getAll()} so it is discoverable by the player-skill system and menus.</p>
 */
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

    public static final SkillId ICE_SPELL =
        SkillId.of("sword", "active.ice_spell");


    /* =========================
       Passive Skills
       ========================= */

    public static final SkillId BLEED_MASTERY =
        SkillId.of("sword", "passive.bleed_mastery");


    /* =========================
       Test Stubs (TODO: remove once real found abilities replace these)
       ========================= */

    public static final SkillId TEST_ALPHA =
        SkillId.of("sword", "active.test_alpha");

    public static final SkillId TEST_BETA =
        SkillId.of("sword", "active.test_beta");

    public static final SkillId TEST_GAMMA =
        SkillId.of("sword", "active.test_gamma");


    // Remember to add new skills to this list!
    private static final List<SkillId> ALL = List.of(
        NONE,
        LOCKED,
        SHADOW_SLASH,
        VOID_LUNGE,
        KNIFE_THROW,
        ICE_SPELL,
        BLEED_MASTERY,
        TEST_ALPHA,
        TEST_BETA,
        TEST_GAMMA
    );

    /**
     * Returns all registered skill IDs, used to populate fresh player skill containers.
     *
     * @return immutable list of all known {@link SkillId}s
     */
    public static List<SkillId> getAll() {
        return ALL;
    }
}
