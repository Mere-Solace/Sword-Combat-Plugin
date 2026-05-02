package btm.sword.entity.mob;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.bukkit.entity.EntityType;

import btm.sword.entity.aspect.AspectType;
import btm.sword.entity.aspect.value.AspectValue;
import btm.sword.entity.aspect.value.ResourceValue;
import btm.sword.entity.base.CombatProfile;

/**
 * Immutable definition for a mob type loaded from {@code mob_types.yml}.
 * <p>
 * Contains the DEU display group tag, resolved {@link AnimationSlots}, optional per-stat
 * overrides applied on top of global {@link CombatProfile} defaults, and a list of ability IDs
 * reserved for future ability system integration.
 * </p>
 *
 * @param id            the mob type id (e.g. {@code "pillager"})
 * @param entityType    the vanilla entity type this definition applies to
 * @param displayGroup  the DEU group tag to spawn as the visual rig; {@code null} if none
 * @param animationSlots resolved animation tag names for each state
 * @param statOverrides per-stat overrides keyed by lowercase {@link AspectType} name
 * @param abilityIds    list of ability IDs (reserved for future use)
 */
public record MobTypeDefinition(
        String id,
        @Nullable EntityType entityType,
        @Nullable String displayGroup,
        AnimationSlots animationSlots,
        Map<String, Float> statOverrides,
        List<String> abilityIds) {

    /**
     * Applies this type's stat overrides onto the given {@link CombatProfile}.
     * For resource stats, only the base value is changed; regen period and amount are preserved.
     *
     * @param profile the profile to modify in place
     */
    public void applyTo(CombatProfile profile) {
        for (Map.Entry<String, Float> entry : statOverrides.entrySet()) {
            AspectType type;
            try {
                type = AspectType.valueOf(entry.getKey().toUpperCase());
            } catch (IllegalArgumentException e) {
                continue;
            }
            AspectValue existing = profile.getStat(type);
            if (existing instanceof ResourceValue rv) {
                profile.setStat(type, new ResourceValue(entry.getValue(), rv.getRegenPeriod(), rv.getRegenAmount()));
            } else {
                profile.setStat(type, new AspectValue(entry.getValue()));
            }
        }
    }
}
