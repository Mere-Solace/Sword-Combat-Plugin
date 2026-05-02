package btm.sword.combat.def;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.joml.Vector3f;

import btm.sword.combat.simulation.ParticleEffect;
import btm.sword.combat.visuals.CircleDisplay;
import btm.sword.combat.visuals.LineDisplay;
import btm.sword.combat.visuals.OriginAnchor;
import btm.sword.combat.visuals.ParticleDisplay;
import btm.sword.combat.visuals.PointDisplay;
import btm.sword.combat.visuals.SphereDisplay;

/**
 * Public serialization utilities for {@link ParticleDisplay} and its components.
 *
 * <p>Extracted from {@link AttackDefSerializer} so that other systems (e.g.
 * {@link ParticleDisplayLibrary}) can read and write display data without going through
 * the full attack serialization path.</p>
 */
public final class ParticleDisplaySerializer {

    private ParticleDisplaySerializer() {}

    /**
     * Deserializes a {@link ParticleDisplay} from a raw YAML map.
     *
     * @param map the raw map from YAML
     * @return the deserialized display, or a default {@link PointDisplay} if the map is malformed
     */
    public static ParticleDisplay load(Map<?, ?> map) {
        if (map == null) return new PointDisplay(OriginAnchor.owning(), new Vector3f(), new Vector3f(), 1, 0, new ArrayList<>());
        Object shapeRaw = map.get("shape");
        String shape = (shapeRaw != null ? shapeRaw.toString() : "POINT").toUpperCase();
        OriginAnchor anchor = loadAnchor(map.get("anchor"));
        Vector3f originOffset = map.get("origin-offset") instanceof Map<?, ?> oMap
            ? readVec(oMap) : new Vector3f();
        Vector3f randomRange = map.get("random-offset-range") instanceof Map<?, ?> rMap
            ? readVec(rMap) : new Vector3f();
        int repeatCount = map.get("repeat-count") instanceof Number n ? n.intValue() : 1;
        int repeatPeriodTicks = map.get("repeat-period-ticks") instanceof Number n ? n.intValue() : 0;
        int betweenKfRepeat = map.get("between-kf-repeat") instanceof Number n ? n.intValue() : 0;
        List<ParticleEffect> particles = new ArrayList<>();
        if (map.get("particles") instanceof List<?> pList) {
            for (Object item : pList) {
                if (item instanceof Map<?, ?> pMap) {
                    particles.add(loadParticleEffect(pMap));
                }
            }
        }
        ParticleDisplay display = switch (shape) {
            case "LINE" -> {
                OriginAnchor endAnchor = loadAnchor(map.get("end-anchor"));
                Vector3f endOffset = map.get("end-offset") instanceof Map<?, ?> eo
                    ? readVec(eo) : new Vector3f();
                Vector3f endRandom = map.get("end-random-range") instanceof Map<?, ?> er
                    ? readVec(er) : new Vector3f();
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
        display.setBetweenKfRepeat(betweenKfRepeat);
        return display;
    }

    /**
     * Serializes a {@link ParticleDisplay} to a raw YAML-compatible map.
     *
     * @param display the display to serialize
     * @return a map suitable for writing with {@link org.bukkit.configuration.file.YamlConfiguration}
     */
    public static Map<String, Object> serialize(ParticleDisplay display) {
        Map<String, Object> map = new LinkedHashMap<>();
        String shape = switch (display) {
            case LineDisplay ignored -> "LINE";
            case SphereDisplay ignored -> "SPHERE";
            case CircleDisplay ignored -> "CIRCLE";
            default -> "POINT";
        };
        map.put("shape", shape);
        map.put("anchor", serializeAnchor(display.getAnchor()));
        map.put("origin-offset", vecMap(display.getOriginOffset()));
        map.put("random-offset-range", vecMap(display.getRandomOffsetRange()));
        map.put("repeat-count", display.getRepeatCount());
        map.put("repeat-period-ticks", display.getRepeatPeriodTicks());
        if (display.getBetweenKfRepeat() > 0) map.put("between-kf-repeat", display.getBetweenKfRepeat());
        List<Map<String, Object>> pList = new ArrayList<>();
        for (ParticleEffect p : display.getParticles()) {
            pList.add(serializeParticle(p));
        }
        map.put("particles", pList);
        switch (display) {
            case LineDisplay ld -> {
                map.put("end-anchor", serializeAnchor(ld.getEndAnchor()));
                map.put("end-offset", vecMap(ld.getEndOffset()));
                map.put("end-random-range", vecMap(ld.getEndRandomRange()));
                map.put("spacing", ld.getSpacing());
            }
            case SphereDisplay sd -> {
                map.put("radius", sd.getRadius());
                map.put("density", sd.getDensity());
                map.put("filled", sd.isFilled());
            }
            case CircleDisplay cd -> {
                map.put("outer-radius", cd.getOuterRadius());
                map.put("inner-radius", cd.getInnerRadius());
                map.put("spacing-radial", cd.getSpacingRadial());
                map.put("spacing-arc", cd.getSpacingArc());
                map.put("normal", cd.getNormal().name());
            }
            default -> {
            }
        }
        return map;
    }

    /**
     * Deserializes an {@link OriginAnchor} from a raw YAML value.
     *
     * @param raw the raw object from the YAML map under the {@code anchor} key
     * @return the resolved anchor, defaulting to {@link OriginAnchor#owning()}
     */
    public static OriginAnchor loadAnchor(Object raw) {
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

    /**
     * Serializes an {@link OriginAnchor} to a YAML-compatible map.
     *
     * @param anchor the anchor to serialize
     * @return a map with a {@code kind} key and any additional fields
     */
    public static Map<String, Object> serializeAnchor(OriginAnchor anchor) {
        Map<String, Object> m = new LinkedHashMap<>();
        switch (anchor) {
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

    /**
     * Deserializes a {@link ParticleEffect} from a raw YAML map.
     *
     * @param map the raw map containing particle fields
     * @return the deserialized effect
     */
    public static ParticleEffect loadParticleEffect(Map<?, ?> map) {
        Particle type = Particle.valueOf(String.valueOf(map.get("type")).toUpperCase());
        int count = map.get("count") instanceof Number n ? n.intValue() : 1;
        Vector3f spreadOffset;
        if (map.get("spreadOffset") instanceof Map<?, ?> soMap) {
            spreadOffset = readVec(soMap);
        } else {
            spreadOffset = new Vector3f();
        }
        double speed = map.get("speed") instanceof Number n ? n.doubleValue() : -1.0;
        Particle.DustOptions dustOptions = null;
        if (map.get("dust") instanceof Map<?, ?> dustMap) {
            int r = dustMap.get("r") instanceof Number n ? n.intValue() : 255;
            int g = dustMap.get("g") instanceof Number n ? n.intValue() : 255;
            int b = dustMap.get("b") instanceof Number n ? n.intValue() : 255;
            float size = dustMap.get("size") instanceof Number n ? n.floatValue() : 1.0f;
            if (dustMap.containsKey("to-r")) {
                int tr = dustMap.get("to-r") instanceof Number n ? n.intValue() : 255;
                int tg = dustMap.get("to-g") instanceof Number n ? n.intValue() : 255;
                int tb = dustMap.get("to-b") instanceof Number n ? n.intValue() : 255;
                dustOptions = new Particle.DustTransition(Color.fromRGB(r, g, b), Color.fromRGB(tr, tg, tb), size);
            } else {
                dustOptions = new Particle.DustOptions(Color.fromRGB(r, g, b), size);
            }
        }
        Color entityColor = null;
        if (map.get("entityColor") instanceof Map<?, ?> ecMap) {
            int r = ecMap.get("r") instanceof Number n ? n.intValue() : 255;
            int g = ecMap.get("g") instanceof Number n ? n.intValue() : 255;
            int b = ecMap.get("b") instanceof Number n ? n.intValue() : 255;
            entityColor = Color.fromRGB(r, g, b);
        }
        return new ParticleEffect(type, count, spreadOffset, speed, dustOptions, entityColor);
    }

    /**
     * Serializes a {@link ParticleEffect} to a YAML-compatible map.
     *
     * @param p the effect to serialize
     * @return a map containing all particle fields
     */
    public static Map<String, Object> serializeParticle(ParticleEffect p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", p.type().name());
        map.put("count", p.count());
        map.put("spreadOffset", vecMap(p.spreadOffset()));
        map.put("speed", p.speed());
        if (p.dustOptions() instanceof Particle.DustTransition dt) {
            map.put("dust", Map.of(
                "r", dt.getColor().getRed(),
                "g", dt.getColor().getGreen(),
                "b", dt.getColor().getBlue(),
                "to-r", dt.getToColor().getRed(),
                "to-g", dt.getToColor().getGreen(),
                "to-b", dt.getToColor().getBlue(),
                "size", (double) dt.getSize()
            ));
        } else if (p.dustOptions() != null) {
            Particle.DustOptions d = p.dustOptions();
            map.put("dust", Map.of(
                "r", d.getColor().getRed(),
                "g", d.getColor().getGreen(),
                "b", d.getColor().getBlue(),
                "size", (double) d.getSize()
            ));
        }
        if (p.entityColor() != null) {
            map.put("entityColor", Map.of(
                "r", p.entityColor().getRed(),
                "g", p.entityColor().getGreen(),
                "b", p.entityColor().getBlue()
            ));
        }
        return map;
    }

    static Vector3f readVec(Map<?, ?> m) {
        float x = m.get("x") instanceof Number n ? n.floatValue() : 0f;
        float y = m.get("y") instanceof Number n ? n.floatValue() : 0f;
        float z = m.get("z") instanceof Number n ? n.floatValue() : 0f;
        return new Vector3f(x, y, z);
    }

    static Map<String, Object> vecMap(Vector3f v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("x", (double) v.x);
        m.put("y", (double) v.y);
        m.put("z", (double) v.z);
        return m;
    }
}
