package btm.sword.system.attack.def;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import btm.sword.system.attack.HitValuePacket;
import btm.sword.system.attack.simulation.ControlMode;
import btm.sword.system.attack.simulation.ControlPoint;
import btm.sword.system.attack.simulation.ControlPointTrajectory;
import btm.sword.system.attack.simulation.KeyframeEffect;
import btm.sword.system.attack.simulation.KeyframeType;
import btm.sword.system.attack.simulation.KeyframedTrajectory;
import btm.sword.system.attack.simulation.ParticleEffect;
import btm.sword.system.attack.simulation.SoundCue;
import btm.sword.system.attack.simulation.SweepCurve;
import btm.sword.system.attack.simulation.SweepTrajectory;
import btm.sword.system.attack.simulation.VolumeKeyframe;
import btm.sword.system.attack.simulation.VolumeShape;
import btm.sword.system.attack.visuals.CircleDisplay;
import btm.sword.system.attack.visuals.LineDisplay;
import btm.sword.system.attack.visuals.OriginAnchor;
import btm.sword.system.attack.visuals.ParticleDisplay;
import btm.sword.system.attack.visuals.PointDisplay;
import btm.sword.system.attack.visuals.SphereDisplay;
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
        VolumeType type = VolumeType.valueOf(typeStr.toUpperCase());

        int duration = section.getInt("duration");
        HitValuePacket hitValue = loadHitValue(section.getConfigurationSection("hit-value"), id);

        boolean orientWithPitch = section.getBoolean("orient-with-pitch", false);
        boolean lockOriginOnFire = section.getBoolean("lock-origin-on-fire", true);

        AttackDef.Builder builder = new AttackDef.Builder(id)
            .duration(duration)
            .onHit(hitValue)
            .orientWithPitch(orientWithPitch)
            .lockOriginOnFire(lockOriginOnFire);

        switch (type) {
            case SWEEP -> builder.sweep(loadSweepCurve(section.getConfigurationSection("curve"), id));
            case VOLUME -> builder.keyframes(loadKeyframes(section, id));
            case CTRL_POINT -> builder.controlPoints(loadControlPoints(section, id), loadControlMode(section, id));
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
            VolumeShape shape = readShape(map.get("shape"));
            KeyframeEffect effect = null;
            if (map.get("effect") instanceof Map<?, ?> effectMap) {
                effect = loadKeyframeEffect(effectMap);
            }
            boolean jump = Boolean.TRUE.equals(map.get("jump"));
            boolean linearToNext = Boolean.TRUE.equals(map.get("linear-to-next"));
            KeyframeType keyframeType = KeyframeType.STANDARD;
            if (map.get("keyframe-type") instanceof String ktStr) {
                try {
                    keyframeType = KeyframeType.valueOf(ktStr.toUpperCase());
                } catch (IllegalArgumentException ignored) { }
            }
            float originRayOffset = keyframeType == KeyframeType.ORIGIN_RAY
                && map.get("origin-ray-offset") instanceof Number n ? n.floatValue() : 0f;
            Vector3f localRayOrigin = null;
            if (keyframeType == KeyframeType.RAYCAST && map.get("ray-origin") instanceof Map<?, ?> roMap) {
                localRayOrigin = readVector3f(roMap);
            } else if (keyframeType == KeyframeType.ORIGIN_RAY && originRayOffset > 0f) {
                localRayOrigin = new Vector3f(position).normalize().mul(originRayOffset);
            }
            keyframes.add(new VolumeKeyframe(t, position, halfExtents, rotation, shape, effect, jump, linearToNext, keyframeType, originRayOffset, localRayOrigin));
        }
        return keyframes;
    }

    private static List<ControlPoint> loadControlPoints(ConfigurationSection s, String id) {
        List<Map<?, ?>> maps = s.getMapList("ctrl-points");
        if (maps.isEmpty()) throw new IllegalArgumentException("Attack '" + id + "' has no ctrl-points");
        List<ControlPoint> points = new ArrayList<>(maps.size());
        for (Map<?, ?> map : maps) {
            Vector3f position = readVector3f((Map<?, ?>) map.get("position"));
            Vector3f halfExtents = readVector3f((Map<?, ?>) map.get("half-extents"));
            points.add(new ControlPoint(position, halfExtents));
        }
        return points;
    }

    private static ControlMode loadControlMode(ConfigurationSection s, String id) {
        String modeStr = s.getString("control-mode");
        if (modeStr == null) throw new IllegalArgumentException("Attack '" + id + "' missing 'control-mode'");
        return ControlMode.valueOf(modeStr.toUpperCase());
    }

    private static KeyframeEffect loadKeyframeEffect(Map<?, ?> map) {
        List<ParticleDisplay> displays = new ArrayList<>();
        if (map.get("displays") instanceof List<?> dList) {
            for (Object item : dList) {
                if (item instanceof Map<?, ?> dMap) {
                    ParticleDisplay d = loadDisplay(dMap);
                    if (d != null) displays.add(d);
                }
            }
        } else if (map.get("particles") instanceof List<?> flatList) {
            // Pre-hierarchy YAML stored particles flat with an inline `offset`
            // that was always the display's origin offset — lift it into the
            // wrapping PointDisplay's originOffset.
            for (Object item : flatList) {
                if (item instanceof Map<?, ?> pMap) {
                    Vector3f originOffset = pMap.get("offset") instanceof Map<?, ?> offMap
                        ? readVector3f(offMap) : null;
                    displays.add(loadParticleEffect(pMap).toPointDisplay(originOffset));
                }
            }
        }
        SoundCue sound = null;
        if (map.get("sound") instanceof Map<?, ?> soundMap) {
            sound = loadSoundCue(soundMap);
        }
        return new KeyframeEffect(displays, sound);
    }

    private static ParticleDisplay loadDisplay(Map<?, ?> map) {
        Object shapeRaw = map.get("shape");
        String shape = (shapeRaw != null ? shapeRaw.toString() : "POINT").toUpperCase();
        OriginAnchor anchor = loadAnchor(map.get("anchor"));
        Vector3f originOffset = map.get("origin-offset") instanceof Map<?, ?> oMap
            ? readVector3f(oMap) : new Vector3f();
        Vector3f randomRange = map.get("random-offset-range") instanceof Map<?, ?> rMap
            ? readVector3f(rMap) : new Vector3f();
        int repeatCount = map.get("repeat-count") instanceof Number n ? n.intValue() : 1;
        int repeatPeriodTicks = map.get("repeat-period-ticks") instanceof Number n ? n.intValue() : 0;
        List<ParticleEffect> particles = new ArrayList<>();
        if (map.get("particles") instanceof List<?> pList) {
            for (Object item : pList) {
                if (item instanceof Map<?, ?> pMap) {
                    particles.add(loadParticleEffect(pMap));
                }
            }
        }
        return switch (shape) {
            case "LINE" -> {
                OriginAnchor endAnchor = loadAnchor(map.get("end-anchor"));
                Vector3f endOffset = map.get("end-offset") instanceof Map<?, ?> eo
                    ? readVector3f(eo) : new Vector3f();
                Vector3f endRandom = map.get("end-random-range") instanceof Map<?, ?> er
                    ? readVector3f(er) : new Vector3f();
                double spacing = map.get("spacing") instanceof Number n ? n.doubleValue() : 0.25;
                yield new LineDisplay(anchor, originOffset, randomRange,
                    repeatCount, repeatPeriodTicks, particles,
                    endAnchor, endOffset, endRandom, spacing);
            }
            case "SPHERE" -> {
                double radius = map.get("radius") instanceof Number n ? n.doubleValue() : 1.0;
                int density = map.get("density") instanceof Number n ? n.intValue() : 30;
                boolean filled = Boolean.TRUE.equals(map.get("filled"));
                yield new SphereDisplay(anchor, originOffset, randomRange,
                    repeatCount, repeatPeriodTicks, particles, radius, density, filled);
            }
            case "CIRCLE" -> {
                double outer = map.get("outer-radius") instanceof Number n ? n.doubleValue() : 1.0;
                double inner = map.get("inner-radius") instanceof Number n ? n.doubleValue() : 0.8;
                double sr = map.get("spacing-radial") instanceof Number n ? n.doubleValue() : 0.25;
                double sa = map.get("spacing-arc") instanceof Number n ? n.doubleValue() : Math.PI / 16;
                CircleDisplay.Normal normal;
                Object normalRaw = map.get("normal");
                String normalStr = (normalRaw != null ? normalRaw.toString() : "ATTACK_UP").toUpperCase();
                try {
                    normal = CircleDisplay.Normal.valueOf(normalStr);
                } catch (IllegalArgumentException e) {
                    normal = CircleDisplay.Normal.ATTACK_UP;
                }
                yield new CircleDisplay(anchor, originOffset, randomRange,
                    repeatCount, repeatPeriodTicks, particles,
                    outer, inner, sr, sa, normal);
            }
            default -> new PointDisplay(anchor, originOffset, randomRange,
                repeatCount, repeatPeriodTicks, particles);
        };
    }

    private static OriginAnchor loadAnchor(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) return OriginAnchor.owning();
        Object kindRaw = map.get("kind");
        String kind = (kindRaw != null ? kindRaw.toString() : "OWNING").toUpperCase();
        return switch (kind) {
            case "KEYFRAME" -> OriginAnchor.keyframe(
                map.get("index") instanceof Number n ? n.intValue() : 0);
            case "BODY" -> {
                Object pointRaw = map.get("point");
                String pointStr = (pointRaw != null ? pointRaw.toString() : "EYE").toUpperCase();
                OriginAnchor.BodyPoint bp;
                try {
                    bp = OriginAnchor.BodyPoint.valueOf(pointStr);
                } catch (IllegalArgumentException e) {
                    bp = OriginAnchor.BodyPoint.EYE;
                }
                yield OriginAnchor.body(bp);
            }
            case "LOCKED" -> OriginAnchor.fireLocked();
            case "RAYCAST_ORIGIN" -> OriginAnchor.raycastOrigin();
            default -> OriginAnchor.owning();
        };
    }

    private static ParticleEffect loadParticleEffect(Map<?, ?> map) {
        Particle type = Particle.valueOf(String.valueOf(map.get("type")).toUpperCase());
        int count = map.get("count") instanceof Number n ? n.intValue() : 1;
        Vector3f spreadOffset;
        if (map.get("spreadOffset") instanceof Map<?, ?> soMap) {
            spreadOffset = readVector3f(soMap);
        } else if (map.get("spread") instanceof Number sn) {
            float s = sn.floatValue();
            spreadOffset = new Vector3f(s, s, s);
        } else {
            spreadOffset = new Vector3f(0.1f, 0.1f, 0.1f);
        }
        double speed = map.get("speed") instanceof Number sn ? sn.doubleValue() : -1.0;
        Particle.DustOptions dust = null;
        if (map.get("dust") instanceof Map<?, ?> dustMap) {
            int r = dustMap.get("r") instanceof Number rn ? rn.intValue() : 255;
            int g = dustMap.get("g") instanceof Number gn ? gn.intValue() : 255;
            int b = dustMap.get("b") instanceof Number bn ? bn.intValue() : 255;
            float size = dustMap.get("size") instanceof Number sn2 ? sn2.floatValue() : 1.0f;
            dust = new Particle.DustOptions(Color.fromRGB(r, g, b), size);
        }
        return new ParticleEffect(type, count, spreadOffset, speed, dust);
    }

    private static SoundCue loadSoundCue(Map<?, ?> map) {
        String keyStr = String.valueOf(map.get("key"));
        // Support both "minecraft:entity.player.attack.sweep" and bare "entity.player.attack.sweep"
        NamespacedKey namespacedKey = keyStr.contains(":")
            ? NamespacedKey.fromString(keyStr)
            : NamespacedKey.minecraft(keyStr);
        if (namespacedKey == null) throw new IllegalArgumentException("Invalid sound key: " + keyStr);
        Sound sound = Registry.SOUNDS.get(namespacedKey);
        if (sound == null) throw new IllegalArgumentException("Unknown sound: " + keyStr);
        SoundCategory category = map.get("category") instanceof String s
            ? SoundCategory.valueOf(s.toUpperCase()) : SoundCategory.PLAYERS;
        float volume = map.get("volume") instanceof Number n ? n.floatValue() : 1.0f;
        float pitch = map.get("pitch") instanceof Number n ? n.floatValue() : 1.0f;
        return new SoundCue(sound, category, volume, pitch);
    }

    private static VolumeShape readShape(Object value) {
        if (value instanceof String s) {
            try {
                return VolumeShape.valueOf(s.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to default
            }
        }
        return VolumeShape.OBB;
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
        yaml.set(path + ".orient-with-pitch", attack.isOrientWithPitch());
        yaml.set(path + ".lock-origin-on-fire", attack.isLockOriginOnFire());

        saveHitValue(yaml, path + ".hit-value", attack.getHitValue());

        switch (attack.getType()) {
            case SWEEP -> saveSweepCurve(yaml, path, attack);
            case VOLUME -> saveKeyframes(yaml, path, attack);
            case CTRL_POINT -> saveControlPoints(yaml, path, attack);
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
            Map<String, Object> kfMap = new LinkedHashMap<>();
            kfMap.put("t", (double) kf.t());
            kfMap.put("position", vecMap(kf.localPosition()));
            kfMap.put("half-extents", vecMap(kf.halfExtents()));
            kfMap.put("rotation", quatMap(kf.rotation()));
            kfMap.put("shape", kf.shape().name());
            if (kf.keyframeType() != KeyframeType.STANDARD) {
                kfMap.put("keyframe-type", kf.keyframeType().name());
            }
            if (kf.keyframeType() == KeyframeType.ORIGIN_RAY) {
                kfMap.put("origin-ray-offset", (double) kf.originRayOffset());
            }
            if (kf.keyframeType() == KeyframeType.RAYCAST && kf.localRayOrigin() != null) {
                kfMap.put("ray-origin", vecMap(kf.localRayOrigin()));
            }
            if (kf.jump()) {
                kfMap.put("jump", true);
            }
            if (kf.linearToNext()) {
                kfMap.put("linear-to-next", true);
            }
            if (kf.effect() != null) {
                kfMap.put("effect", serializeEffect(kf.effect()));
            }
            kfMaps.add(kfMap);
        }
        yaml.set(path + ".keyframes", kfMaps);
    }

    private static void saveControlPoints(YamlConfiguration yaml, String path, AttackDef attack) {
        if (!(attack.getTrajectory() instanceof ControlPointTrajectory cpt)) {
            throw new UnsupportedOperationException(
                "Cannot serialize non-ControlPointTrajectory as CTRL_POINT for: " + attack.getId());
        }
        List<Map<String, Object>> ptMaps = new ArrayList<>();
        for (ControlPoint cp : cpt.getPoints()) {
            Map<String, Object> ptMap = new LinkedHashMap<>();
            ptMap.put("position", vecMap(cp.position()));
            ptMap.put("half-extents", vecMap(cp.halfExtents()));
            ptMaps.add(ptMap);
        }
        yaml.set(path + ".ctrl-points", ptMaps);
        yaml.set(path + ".control-mode", cpt.getMode().name());
    }

    private static Map<String, Object> serializeEffect(KeyframeEffect effect) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (effect.displays() != null && !effect.displays().isEmpty()) {
            List<Map<String, Object>> dList = new ArrayList<>();
            for (ParticleDisplay d : effect.displays()) {
                dList.add(serializeDisplay(d));
            }
            map.put("displays", dList);
        }
        if (effect.sound() != null) {
            map.put("sound", serializeSound(effect.sound()));
        }
        return map;
    }

    private static Map<String, Object> serializeDisplay(ParticleDisplay d) {
        Map<String, Object> map = new LinkedHashMap<>();
        String shape = switch (d) {
            case LineDisplay ignored -> "LINE";
            case SphereDisplay ignored -> "SPHERE";
            case CircleDisplay ignored -> "CIRCLE";
            default -> "POINT";
        };
        map.put("shape", shape);
        map.put("anchor", serializeAnchor(d.getAnchor()));
        map.put("origin-offset", vecMap(d.getOriginOffset()));
        map.put("random-offset-range", vecMap(d.getRandomOffsetRange()));
        map.put("repeat-count", d.getRepeatCount());
        map.put("repeat-period-ticks", d.getRepeatPeriodTicks());
        List<Map<String, Object>> pList = new ArrayList<>();
        for (ParticleEffect p : d.getParticles()) {
            pList.add(serializeParticle(p));
        }
        map.put("particles", pList);
        if (d instanceof LineDisplay ld) {
            map.put("end-anchor", serializeAnchor(ld.getEndAnchor()));
            map.put("end-offset", vecMap(ld.getEndOffset()));
            map.put("end-random-range", vecMap(ld.getEndRandomRange()));
            map.put("spacing", ld.getSpacing());
        } else if (d instanceof SphereDisplay sd) {
            map.put("radius", sd.getRadius());
            map.put("density", sd.getDensity());
            map.put("filled", sd.isFilled());
        } else if (d instanceof CircleDisplay cd) {
            map.put("outer-radius", cd.getOuterRadius());
            map.put("inner-radius", cd.getInnerRadius());
            map.put("spacing-radial", cd.getSpacingRadial());
            map.put("spacing-arc", cd.getSpacingArc());
            map.put("normal", cd.getNormal().name());
        }
        return map;
    }

    private static Map<String, Object> serializeAnchor(OriginAnchor anchor) {
        Map<String, Object> m = new LinkedHashMap<>();
        switch (anchor) {
            case OriginAnchor.OwningKeyframe ignored -> m.put("kind", "OWNING");
            case OriginAnchor.KeyframeIndex ki -> {
                m.put("kind", "KEYFRAME");
                m.put("index", ki.index());
            }
            case OriginAnchor.EntityBodyPoint ebp -> {
                m.put("kind", "BODY");
                m.put("point", ebp.point().name());
            }
            case OriginAnchor.FireLockedOrigin ignored -> m.put("kind", "LOCKED");
            case OriginAnchor.RaycastOrigin ignored -> m.put("kind", "RAYCAST_ORIGIN");
            default -> m.put("kind", "OWNING");
        }
        return m;
    }

    private static Map<String, Object> serializeParticle(ParticleEffect p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", p.type().name());
        map.put("count", p.count());
        map.put("spreadOffset", vecMap(p.spreadOffset()));
        map.put("speed", p.speed());
        if (p.dustOptions() != null) {
            Particle.DustOptions d = p.dustOptions();
            map.put("dust", Map.of(
                "r", d.getColor().getRed(),
                "g", d.getColor().getGreen(),
                "b", d.getColor().getBlue(),
                "size", (double) d.getSize()
            ));
        }
        return map;
    }

    private static Map<String, Object> serializeSound(SoundCue sc) {
        return Map.of(
            "key", Registry.SOUNDS.getKey(sc.sound()).asString(),
            "category", sc.category().name(),
            "volume", (double) sc.volume(),
            "pitch", (double) sc.pitch()
        );
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
