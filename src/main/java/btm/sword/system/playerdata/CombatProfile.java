package btm.sword.system.playerdata;

import btm.sword.system.entity.aspect.AspectType;
import btm.sword.system.entity.aspect.value.AspectValue;
import btm.sword.system.entity.aspect.value.ResourceValue;
import java.util.HashMap;

/**
 * Stores the combat configuration and default stat values for a player or entity.
 * <p>
 * This class serves as a template containing base values for all aspects and resources,
 * the player's sword class/archetype, and combat settings like air dodge limits.
 * When an entity spawns, these values are used to initialize its {@link btm.sword.system.entity.EntityAspects}.
 * </p>
 *
 * <h3>Default Resource Values:</h3>
 * <ul>
 *   <li><b>Shards:</b> 5 max, regenerates 1 per 40 ticks</li>
 *   <li><b>Toughness:</b> 100 max, regenerates 0.5 per 20 ticks</li>
 *   <li><b>Soulfire:</b> 50 max, regenerates 0.2 per 2 ticks</li>
 *   <li><b>Form:</b> 10 max, regenerates 1 per 60 ticks</li>
 * </ul>
 *
 * @see AspectType
 * @see AspectValue
 * @see ResourceValue
 * @see SwordClassType
 * @see btm.sword.system.entity.EntityAspects
 */
public class CombatProfile {
    /** The sword class/archetype for this combat profile (default: LOSAH). */
    private SwordClassType swordClass;

    /** Map storing default values for all aspects and resources. */
    private final HashMap<AspectType, AspectValue> stats = new HashMap<>(); // max Stats

    /** Maximum number of air dodges the entity can perform before landing. */
    private int maxAirDodges = 1;

    /**
     * Constructs a new CombatProfile with default values.
     * <p>
     * Initializes all resources with their default max values and regeneration rates,
     * sets all non-resource aspects to 1, and sets the sword class to LOSAH.
     * </p>
     */
    public CombatProfile() {
        swordClass = SwordClassType.LOSAH;

        for (AspectType stat : AspectType.values()) {
            switch (stat) {
                case SHARDS -> stats.put(stat, new ResourceValue(5, 40, 1));
                case TOUGHNESS -> stats.put(stat, new ResourceValue(100, 20, 0.5f));
                case SOULFIRE -> stats.put(stat, new ResourceValue(50, 2, 0.2f));
                case FORM -> stats.put(stat, new ResourceValue(10, 60, 1));
                default -> stats.put(stat, new AspectValue(1));
            }
        }
        // replace item material type with a specific item metadata in the future
    }

    /**
     * Gets the sword class/archetype for this profile.
     *
     * @return the sword class type
     */
    public SwordClassType getSwordClass() {
        return swordClass;
    }

    /**
     * Sets the sword class/archetype for this profile.
     *
     * @param swordClass the new sword class type
     */
    public void setSwordClass(SwordClassType swordClass) {
        this.swordClass = swordClass;
    }

    /**
     * Sets the value for a specific aspect type.
     * <p>
     * This updates or adds the aspect value in the stats map.
     * </p>
     *
     * @param type the aspect type to set
     * @param values the new aspect value
     */
    public void setStat(AspectType type, AspectValue values) {
        stats.put(type, values);
    }

    /**
     * Gets the value for a specific aspect type.
     *
     * @param type the aspect type to retrieve
     * @return the aspect value, or null if not set
     */
    public AspectValue getStat(AspectType type) {
        return stats.get(type);
    }

    /**
     * Increases the maximum number of air dodges by 1.
     * <p>
     * This can be used for upgrades or special abilities.
     * </p>
     */
    public void increaseNumAirDodges() {
        maxAirDodges++;
    }

    /**
     * Gets the maximum number of air dodges.
     *
     * @return the max air dodge count
     */
    public int getMaxAirDodges() {
        return maxAirDodges;
    }
}
