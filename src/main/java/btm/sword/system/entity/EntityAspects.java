package btm.sword.system.entity;

import btm.sword.system.entity.aspect.Aspect;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;
import btm.sword.system.playerdata.CombatProfile;

/**
 * Manages all aspects (stats and resources) for a combat entity.
 * <p>
 * This class serves as a container and facade for accessing an entity's
 * RPG-style attributes. It maintains four regenerating resources and eight
 * static aspects, providing convenient accessor methods for each.
 * </p>
 *
 * <h3>Resources (regenerating):</h3>
 * <ul>
 *   <li><b>Shards</b> - Primary resource for abilities</li>
 *   <li><b>Toughness</b> - Damage mitigation resource</li>
 *   <li><b>Soulfire</b> - Special ability resource</li>
 *   <li><b>Form</b> - Stance/form resource</li>
 * </ul>
 *
 * <h3>Aspects (static stats):</h3>
 * <ul>
 *   <li><b>Might</b> - Physical power</li>
 *   <li><b>Resolve</b> - Mental fortitude</li>
 *   <li><b>Finesse</b> - Precision and dexterity</li>
 *   <li><b>Prowess</b> - Combat skill</li>
 *   <li><b>Armor</b> - Damage reduction</li>
 *   <li><b>Fortitude</b> - Physical resilience</li>
 *   <li><b>Celerity</b> - Speed and agility</li>
 *   <li><b>Willpower</b> - Mental strength</li>
 * </ul>
 *
 * @see Aspect
 * @see Resource
 * @see CombatProfile
 */
public class EntityAspects {
    /** Array containing all 12 aspects (4 resources + 8 aspects). */
    private final Aspect[] stats = new Aspect[12];

    // Resources
    private final Resource shards;
    private final Resource toughness;
    private final Resource soulfire;
    private final Resource form;

    // Aspects
    private final Aspect might;
    private final Aspect resolve;
    private final Aspect finesse;
    private final Aspect prowess;
    private final Aspect armor;
    private final Aspect fortitude;
    private final Aspect celerity;
    private final Aspect willpower;

    /**
     * Constructs EntityAspects from a combat profile.
     * <p>
     * Initializes all resources and aspects with values from the profile,
     * and starts automatic regeneration tasks for resources.
     * </p>
     *
     * @param profile the combat profile containing initial values
     */
    public EntityAspects(CombatProfile profile) {
        AspectValue shardVals = profile.getStat(AspectType.SHARDS);
        shards = new Resource(
                AspectType.SHARDS,
                shardVals.getValue(),
                ((ResourceValue) shardVals).getRegenPeriod(),
                ((ResourceValue) shardVals).getRegenAmount());
        shards.startRegenTask();
        stats[0] = shards;

        AspectValue toughnessVals = profile.getStat(AspectType.TOUGHNESS);
        toughness = new Resource(
                AspectType.TOUGHNESS,
                toughnessVals.getValue(),
                ((ResourceValue) toughnessVals).getRegenPeriod(),
                ((ResourceValue) toughnessVals).getRegenAmount());
        toughness.startRegenTask();
        stats[1] = toughness;

        AspectValue soulfireVals = profile.getStat(AspectType.SOULFIRE);
        soulfire = new Resource(
                AspectType.SOULFIRE,
                soulfireVals.getValue(),
                ((ResourceValue) soulfireVals).getRegenPeriod(),
                ((ResourceValue) soulfireVals).getRegenAmount());
        soulfire.startRegenTask();
        stats[2] = soulfire;

        AspectValue formVals = profile.getStat(AspectType.FORM);
        form = new Resource(
                AspectType.FORM,
                formVals.getValue(),
                ((ResourceValue) formVals).getRegenPeriod(),
                ((ResourceValue) formVals).getRegenAmount());
        form.startRegenTask();
        stats[3] = form;

        might = new Aspect(AspectType.MIGHT, profile.getStat(AspectType.MIGHT).getValue());
        stats[4] = might;
        resolve = new Aspect(AspectType.RESOLVE, profile.getStat(AspectType.RESOLVE).getValue());
        stats[5] = resolve;
        finesse = new Aspect(AspectType.FINESSE, profile.getStat(AspectType.FINESSE).getValue());
        stats[6] = finesse;
        prowess = new Aspect(AspectType.PROWESS, profile.getStat(AspectType.PROWESS).getValue());
        stats[7] = prowess;
        armor = new Aspect(AspectType.ARMOR, profile.getStat(AspectType.ARMOR).getValue());
        stats[8] = armor;
        fortitude = new Aspect(AspectType.FORTITUDE, profile.getStat(AspectType.FORTITUDE).getValue());
        stats[9] = fortitude;
        celerity = new Aspect(AspectType.CELERITY, profile.getStat(AspectType.CELERITY).getValue());
        stats[10] = celerity;
        willpower = new Aspect(AspectType.WILLPOWER, profile.getStat(AspectType.WILLPOWER).getValue());
        stats[11] = willpower;
    }

    /**
     * Gets an aspect by type using a type-safe enum lookup.
     *
     * @param type the aspect type to retrieve
     * @return the aspect of the specified type
     */
    public Aspect getAspect(AspectType type) {
        return switch (type) {
            case SHARDS -> shards;
            case TOUGHNESS -> toughness;
            case SOULFIRE -> soulfire;
            case FORM -> form;

            case MIGHT -> might;
            case RESOLVE -> resolve;
            case FINESSE -> finesse;
            case PROWESS -> prowess;
            case ARMOR -> armor;
            case FORTITUDE -> fortitude;
            case CELERITY -> celerity;
            case WILLPOWER -> willpower;
        };
    }

    /**
     * Returns all aspects as an array.
     *
     * @return array containing all 12 aspects
     */
    public Aspect[] aspectSet() {
        return stats;
    }

    /**
     * Gets the effective value of an aspect by type.
     *
     * @param type the aspect type
     * @return the effective value
     */
    public float getAspectVal(AspectType type) {
        return getAspect(type).effectiveValue();
    }

    // Resource Getters

    /** @return the shards resource */
    public Resource shards() { return shards; }

    /** @return the toughness resource */
    public Resource toughness() { return toughness; }

    /** @return the soulfire resource */
    public Resource soulfire() { return soulfire; }

    /** @return the form resource */
    public Resource form() { return form; }

    // Aspect Getters

    /** @return the might aspect */
    public Aspect might() { return might; }

    /** @return the resolve aspect */
    public Aspect resolve() { return resolve; }

    /** @return the finesse aspect */
    public Aspect finesse() { return finesse; }

    /** @return the prowess aspect */
    public Aspect prowess() { return prowess; }

    /** @return the armor aspect */
    public Aspect armor() { return armor; }

    /** @return the fortitude aspect */
    public Aspect fortitude() { return fortitude; }

    /** @return the celerity aspect */
    public Aspect celerity() { return celerity; }

    /** @return the willpower aspect */
    public Aspect willpower() { return willpower; }

    // Resource Effective Value Getters

    /** @return the effective maximum value of shards */
    public float shardsVal() { return shards.effectiveValue(); }

    /** @return the effective maximum value of toughness */
    public float toughnessVal() { return toughness.effectiveValue(); }

    /** @return the effective maximum value of soulfire */
    public float soulfireVal() { return soulfire.effectiveValue(); }

    /** @return the effective maximum value of form */
    public float formVal() { return form.effectiveValue(); }

    // Aspect Effective Value Getters

    /** @return the effective value of might */
    public float mightVal() { return might.effectiveValue(); }

    /** @return the effective value of resolve */
    public float resolveVal() { return resolve.effectiveValue(); }

    /** @return the effective value of finesse */
    public float finesseVal() { return finesse.effectiveValue(); }

    /** @return the effective value of prowess */
    public float prowessVal() { return prowess.effectiveValue(); }

    /** @return the effective value of armor */
    public float armorVal() { return armor.effectiveValue(); }

    /** @return the effective value of fortitude */
    public float fortitudeVal() { return fortitude.effectiveValue(); }

    /** @return the effective value of celerity */
    public float celerityVal() { return celerity.effectiveValue(); }

    /** @return the effective value of willpower */
    public float willpowerVal() { return willpower.effectiveValue(); }

    // Resource Current Value Getters

    /** @return the current value of shards */
    public float shardsCur() { return shards.cur(); }

    /** @return the current value of toughness */
    public float toughnessCur() { return toughness.cur(); }

    /** @return the current value of soulfire */
    public float soulfireCur() { return soulfire.cur(); }

    /** @return the current value of form */
    public float formCur() { return form.cur(); }

    /**
     * Returns a formatted string showing all current resource values.
     *
     * @return multi-line string with current resource values
     */
    public String curResources() {
        return "Shards: " + shardsCur() +
                "\nToughness: " + toughnessCur() +
                "\nSoulfire: " + soulfireCur() +
                "\nForm: " + formCur();
    }
}
