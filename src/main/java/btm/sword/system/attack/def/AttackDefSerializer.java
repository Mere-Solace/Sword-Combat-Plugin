package btm.sword.system.attack.def;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.attack.simulation.SweepCurve;
import btm.sword.system.attack.simulation.SweepTrajectory;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.utility.Debug;

/**
 * Serializes and deserializes {@link AttackDef} entries to/from Bukkit {@link YamlConfiguration}.
 *
 * <p>SWEEP format uses a {@code curve} section with {@code control-points} and
 * {@code radius-profile} lists. VOLUME format uses a {@code keyframes} list.
 * Both share a {@code hit-value} section and top-level {@code type} / {@code duration} fields.</p>
 *
 * <p>{@code knockbackFunction} is not serialized — it cannot be represented in YAML.
 * Loaded attacks use the default zero-vector knockback.</p>
 */
public final class AttackDefSerializer {

    private AttackDefSerializer() {}

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Deserializes a single {@link AttackDef} from a {@link ConfigurationSection}.
     *
     * @param section the YAML section for this attack (child of {@code attacks:})
     * @param id      the attack identifier (key in the parent section)
     * @return the deserialized attack definition
     * @throws IllegalArgumentException if required fields are missing or malformed
     */
    public static AttackDef load(ConfigurationSection section, String id) {
        String typeStr = section.getString("type");
        if (typeStr == null) throw new IllegalArgumentException("Attack '" + id + "' missing 'type'");
        AttackPrimitive type = AttackPrimitive.valueOf(typeStr.toUpperCase());

        int duration = section.getInt("duration");
        HitValuePacket hitValue = loadHitValue(section.getConfigurationSection("hit-value"), id);

        AttackDef.Builder builder = new AttackDef.Builder(id)
            .duration(duration)
            .onHit(hitValue);

        switch (type) {
            case SWEEP -> builder.sweep(loadSweepCurve(section.getConfigurationSection("curve"), id));
            case VOLUME -> builder.keyframes(loadKeyframes(section, id));
        }

        return builder.build();
    }

    private static HitValuePacket loadHitValue(ConfigurationSection s, String id) {
        if (s == null) throw new IllegalArgumentException("Attack '" + id + "' missing 'hit-value'");
        int shards = s.getInt("shard-damage");
        float toughness = (float) s.getDouble("toughness-damage");
        float soulfire = (float) s.getDouble("soulfire-loss");
        float reaped = (float) s.getDouble("reaped-soulfire", 0.0);
        int invulnTicks = s.getInt("invulnerable-ticks", 10);
        return new HitValuePacket(() -> reaped, () -> invulnTicks, () -> shards, () -> toughness, () -> soulfire);
    }

    private static SweepCurve loadSweepCurve(ConfigurationSection s, String id) {
        if (s == null) throw new IllegalArgumentException("Attack '" + id + "' missing 'curve'");

        List<Map<?, ?>> pointMaps = s.getMapList("control-points");
        List<Vector3f> controlPoints = new ArrayList<>(pointMaps.size());
        for (Map<?, ?> map : pointMaps) {
            controlPoints.add(readVector3f(map));
        }

        List<Map<?, ?>> radiusMaps = s.getMapList("radius-profile");
        List<SweepCurve.RadiusPoint> radiusProfile = new ArrayList<>(radiusMaps.size());
        for (Map<?, ?> map : radiusMaps) {
            float t = toFloat(map.get("t"));
            float radius = toFloat(map.get("radius"));
            radiusProfile.add(new SweepCurve.RadiusPoint(t, radius));
        }

        return new SweepCurve(controlPoints, radiusProfile);
    }

    private static List<VolumeKeyframe> loadKeyframes(ConfigurationSection s, String id) {
        List<Map<?, ?>> maps = s.getMapList("keyframes");
        if (maps.isEmpty()) throw new IllegalArgumentException("Attack '" + id + "' has no keyframes");
        List<VolumeKeyframe> keyframes = new ArrayList<>(maps.size());
        for (Map<?, ?> map : maps) {
            float t = toFloat(map.get("t"));
            Vector3f position = readVector3f((Map<?, ?>) map.get("position"));
            Vector3f halfExtents = readVector3f((Map<?, ?>) map.get("half-extents"));
            Quaternionf rotation = readQuaternionf((Map<?, ?>) map.get("rotation"));
            keyframes.add(new VolumeKeyframe(t, position, halfExtents, rotation));
        }
        return keyframes;
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Serializes a single {@link AttackDef} into {@code file}, merging with any
     * existing entries already present in the file.
     *
     * @param file   destination YAML file
     * @param attack the attack definition to write
     * @throws UnsupportedOperationException if the attack uses a custom lambda trajectory
     *                                        that cannot be serialized
     */
    public static void save(File file, AttackDef attack) {
        YamlConfiguration yaml = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();
        String path = "attacks." + attack.getId();

        yaml.set(path + ".type", attack.getType().name());
        yaml.set(path + ".duration", attack.getDurationMs());

        saveHitValue(yaml, path + ".hit-value", attack.getHitValue());

        switch (attack.getType()) {
            case SWEEP -> saveSweepCurve(yaml, path, attack);
            case VOLUME -> saveKeyframes(yaml, path, attack);
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            Debug.system("AttackDefSerializer: failed to save '" + attack.getId() + "': " + e.getMessage());
        }
    }

    private static void saveHitValue(YamlConfiguration yaml, String path, HitValuePacket hv) {
        yaml.set(path + ".shard-damage", hv.shardDamage());
        yaml.set(path + ".toughness-damage", (double) hv.toughnessDamage());
        yaml.set(path + ".soulfire-loss", (double) hv.soulfireLoss());
        yaml.set(path + ".reaped-soulfire", (double) hv.reapedSoulfire());
        yaml.set(path + ".invulnerable-ticks", hv.invulnerableTicks());
    }

    private static void saveSweepCurve(YamlConfiguration yaml, String path, AttackDef attack) {
        if (!(attack.getTrajectory() instanceof SweepTrajectory sweep)) {
            throw new UnsupportedOperationException("Cannot serialize non-SweepTrajectory as SWEEP for: " + attack.getId());
        }
        SweepCurve curve = sweep.getCurve();
        List<Map<String, Double>> pointMaps = new ArrayList<>();
        for (Vector3f p : curve.controlPoints()) {
            pointMaps.add(Map.of("x", (double) p.x, "y", (double) p.y, "z", (double) p.z));
        }
        yaml.set(path + ".curve.control-points", pointMaps);

        List<Map<String, Double>> radiusMaps = new ArrayList<>();
        for (SweepCurve.RadiusPoint rp : curve.radiusProfile()) {
            radiusMaps.add(Map.of("t", (double) rp.t(), "radius", (double) rp.radius()));
        }
        yaml.set(path + ".curve.radius-profile", radiusMaps);
    }

    private static void saveKeyframes(YamlConfiguration yaml, String path, AttackDef attack) {
        if (!(attack.getTrajectory() instanceof KeyframedTrajectory kt)) {
            throw new UnsupportedOperationException("Cannot serialize non-KeyframedTrajectory as VOLUME for: " + attack.getId());
        }
        List<Map<String, Object>> kfMaps = new ArrayList<>();
        for (VolumeKeyframe kf : kt.getKeyframes()) {
            kfMaps.add(Map.of(
                "t", (double) kf.t(),
                "position", vecMap(kf.localPosition()),
                "half-extents", vecMap(kf.halfExtents()),
                "rotation", quatMap(kf.rotation())
            ));
        }
        yaml.set(path + ".keyframes", kfMaps);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Vector3f readVector3f(Map<?, ?> map) {
        return new Vector3f(toFloat(map.get("x")), toFloat(map.get("y")), toFloat(map.get("z")));
    }

    private static Quaternionf readQuaternionf(Map<?, ?> map) {
        return new Quaternionf(toFloat(map.get("x")), toFloat(map.get("y")), toFloat(map.get("z")), toFloat(map.get("w")));
    }

    private static Map<String, Double> vecMap(Vector3f v) {
        return Map.of("x", (double) v.x, "y", (double) v.y, "z", (double) v.z);
    }

    private static Map<String, Double> quatMap(Quaternionf q) {
        return Map.of("x", (double) q.x, "y", (double) q.y, "z", (double) q.z, "w", (double) q.w);
    }

    private static float toFloat(Object value) {
        if (value instanceof Number n) return n.floatValue();
        throw new IllegalArgumentException("Expected a number, got: " + value);
    }
}
