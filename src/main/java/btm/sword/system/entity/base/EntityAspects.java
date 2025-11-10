package btm.sword.system.entity.base;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import btm.sword.Sword;
import btm.sword.system.entity.aspect.Aspect;
import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.Resource;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;


/**
 * Manages and provides access to all {@link Aspect} and {@link Resource} values associated with an entity.
 * <p>
 * The {@code EntityAspects} class acts as a central container for an entity's full set of combat-related stats.
 * These stats are divided into two major groups:
 * <ul>
 *     <li><b>Resources</b> — dynamic pools that regenerate over time, such as {@link #shards}, {@link #toughness}, {@link #soulfire}, and {@link #form}.</li>
 *     <li><b>Aspects</b> — static or semi-static modifiers that describe the entity’s inherent traits and power scaling, such as {@link #might}, {@link #resolve}, {@link #armor}, etc.</li>
 * </ul>
 * <p>
 * Each {@link EntityAspects} instance is constructed from a {@link CombatProfile}, which defines the base values
 * for each {@link AspectType}. Upon construction, all {@link Resource} aspects automatically begin their regeneration
 * tasks (handled internally by their {@link Resource#startRegenTask()} methods).
 *
 * <h3>Usage Overview</h3>
 * <p>
 * This class is primarily accessed through the owning {@link btm.sword.system.entity.base.SwordEntity SwordEntity}.
 * You can retrieve the active {@link EntityAspects} instance via:
 * <pre>{@code
 * SwordEntity entity = ...;
 * EntityAspects aspects = entity.getAspects();
 * }</pre>
 *
 * <h3>Accessing Stats</h3>
 * Each aspect can be retrieved or queried by its {@link AspectType}:
 * <pre>{@code
 * float currentToughness = entity.getAspects().toughnessVal();
 * float currentSoulfire = entity.getAspects().soulfireCur();
 *
 * // Or dynamically by type:
 * float might = entity.getAspects().getAspectVal(AspectType.MIGHT);
 * }</pre>
 *
 * <h3>Modifying Values</h3>
 * Use the {@link Resource} API for additive, subtractive, or reset-based updates:
 * <pre>{@code
 * aspects.toughness().remove(10); // Removes 10 points of toughness
 * aspects.shards().reset();       // Restores shards to full
 * }</pre>
 *
 * <h3>Lifecycle & Integration</h3>
 * <ul>
 *     <li>This object is created once per {@link SwordEntity} (during its construction).</li>
 *     <li>All resource regeneration tasks begin immediately upon construction.</li>
 *     <li>Resources are reset during {@link SwordEntity#onSpawn()} or when a respawn/reset event occurs.</li>
 *     <li>Aspects and resources are primarily visualized in the player HUD or combat overlay, and can be queried
 *     for live updates via the appropriate getters (e.g., for health bars or mana-like gauges).</li>
 * </ul>
 *
 * <h3>Internal Layout</h3>
 * <p>
 * Internally, all aspects are stored in a single {@code Aspect[]} array named {@link #stats}, ordered as follows:
 * <pre>
 * [shards, toughness, soulfire, form, might, resolve, finesse, prowess, armor, fortitude, celerity, willpower]
 * </pre>
 * This layout is deterministic and consistent across entities for indexing and iteration purposes.
 *
 * @see Aspect
 * @see Resource
 * @see CombatProfile
 * @see btm.sword.system.entity.base.SwordEntity
 */
public class EntityAspects {
    /**
     * Ordered array of all aspects (resources first, then static attributes).
     * <p>
     * Index layout:
     * <pre>
     * [0] Shards
     * [1] Toughness
     * [2] Soulfire
     * [3] Form
     * [4] Might
     * [5] Resolve
     * [6] Finesse
     * [7] Prowess
     * [8] Armor
     * [9] Fortitude
     * [10] Celerity
     * [11] Willpower
     * </pre>
     */
    private final Aspect[] stats = new Aspect[12];

    private final Resource shards;
    private final Resource toughness;
    private final Resource soulfire;
    private final Resource form;

    private final Aspect might;
    private final Aspect resolve;
    private final Aspect finesse;
    private final Aspect prowess;
    private final Aspect armor;
    private final Aspect fortitude;
    private final Aspect celerity;
    private final Aspect willpower;

    private BukkitTask restartTask;

    /**
     * Constructs all resource and aspect objects for a given {@link CombatProfile}.
     * <p>
     * Each resource is configured with its base value, regeneration rate, and regeneration period,
     * and then has its regeneration task automatically started.
     *
     * @param profile the {@link CombatProfile} providing base stat and resource values
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
     * Retrieves a specific {@link Aspect} or {@link Resource} based on its {@link AspectType}.
     *
     * @param type the aspect type to retrieve
     * @return the corresponding {@link Aspect} or {@link Resource} instance
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
     * Returns the full ordered array of all aspects, including both resources and static attributes.
     * <p>
     * This is typically used for iteration or serialization purposes.
     *
     * @return an array containing all {@link Aspect}s in canonical order
     */
    public Aspect[] aspectSet() {
        return stats;
    }

    /**
     * Retrieves the current effective value of a given {@link AspectType}.
     * <p>
     * The "effective value" represents the stat’s current real-time magnitude,
     * incorporating modifiers, buffs, or debuffs.
     *
     * @param type the aspect type
     * @return the effective stat value
     */
    public float getAspectVal(AspectType type) {
        return getAspect(type).effectiveValue();
    }

    public Resource shards() { return shards; }
    public Resource toughness() { return toughness; }
    public Resource soulfire() { return soulfire; }
    public Resource form() { return form; }

    public Aspect might() { return might; }
    public Aspect resolve() { return resolve; }
    public Aspect finesse() { return finesse; }
    public Aspect prowess() { return prowess; }
    public Aspect armor() { return armor; }
    public Aspect fortitude() { return fortitude; }
    public Aspect celerity() { return celerity; }
    public Aspect willpower() { return willpower; }

    public float shardsVal() { return shards.effectiveValue(); }
    public float toughnessVal() { return toughness.effectiveValue(); }
    public float soulfireVal() { return soulfire.effectiveValue(); }
    public float formVal() { return form.effectiveValue(); }

    public float mightVal() { return might.effectiveValue(); }
    public float resolveVal() { return resolve.effectiveValue(); }
    public float finesseVal() { return finesse.effectiveValue(); }
    public float prowessVal() { return prowess.effectiveValue(); }
    public float armorVal() { return armor.effectiveValue(); }
    public float fortitudeVal() { return fortitude.effectiveValue(); }
    public float celerityVal() { return celerity.effectiveValue(); }
    public float willpowerVal() { return willpower.effectiveValue(); }

    public float shardsCur() { return shards.cur(); }
    public float toughnessCur() { return toughness.cur(); }
    public float soulfireCur() { return soulfire.cur(); }
    public float formCur() { return form.cur(); }

    public String curResources() {
        return "Shards: " + shardsCur() +
                "\nToughness: " + toughnessCur() +
                "\nSoulfire: " + soulfireCur() +
                "\nForm: " + formCur();
    }

    public void stopAllResourceTasks() {
        if (restartTask != null && !restartTask.isCancelled()) restartTask.cancel();
        shards().stopRegenTask();
        toughness().stopRegenTask();
        soulfire().stopRegenTask();
        form().stopRegenTask();
    }

    public void restartResourceProcessAfterDelay(AspectType type) {
        if (restartTask != null && !restartTask.isCancelled()) return;

        Resource r = null;
        switch (type) {
            case SHARDS -> r = shards;
            case TOUGHNESS -> r = toughness;
            case SOULFIRE -> r = soulfire;
            case FORM ->  r = form;
            default -> { }  // Non-resource aspects don't have regen tasks
        }
        if (r == null) return;
        final Resource R = r;
        restartTask = new BukkitRunnable() {
            @Override
            public void run() {
                R.restartRegenTask();
            }
        }.runTaskLater(Sword.getInstance(), r.getBaseRegenPeriod());
    }
}
