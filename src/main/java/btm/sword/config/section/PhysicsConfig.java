package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

/**
 * Type-safe accessor for physics-related configuration values.
 * <p>
 * Handles projectile motion, gravity, velocity, and rotation settings.
 * Uses hybrid pattern: Simple coordinate triples and 2-value configs flattened,
 * complex rotation speeds kept nested.
 * </p>
 */
@Getter
public class PhysicsConfig {
    private final ThrownItemsConfig thrownItems;
    private final AttackVelocityConfig attackVelocity;

    public PhysicsConfig(FileConfiguration config) {
        ConfigurationSection physics = config.getConfigurationSection("physics");
        if (physics != null) {
            this.thrownItems = new ThrownItemsConfig(physics.getConfigurationSection("thrown_items"));
            this.attackVelocity = new AttackVelocityConfig(physics.getConfigurationSection("attack_velocity"));
        } else {
            this.thrownItems = new ThrownItemsConfig(null);
            this.attackVelocity = new AttackVelocityConfig(null);
        }
    }

    @Getter
    public static class ThrownItemsConfig {
        private final double gravityDamper;
        private final double trajectoryRotation;

        // Flattened display offset (3 coordinate values - no wrapper class needed)
        private final float displayOffsetX;
        private final float displayOffsetY;
        private final float displayOffsetZ;

        // Flattened origin offset (3 coordinate values - no wrapper class needed)
        private final double originOffsetForward;
        private final double originOffsetUp;
        private final double originOffsetBack;

        // Keep nested for complex rotation speeds (7 values - justified)
        private final RotationSpeedConfig rotationSpeed;

        public ThrownItemsConfig(ConfigurationSection section) {
            if (section != null) {
                this.gravityDamper = section.getDouble("gravity_damper", 46.0);
                this.trajectoryRotation = section.getDouble("trajectory_rotation", 0.03696);

                // Load display offset values directly
                ConfigurationSection displayOffset = section.getConfigurationSection("display_offset");
                if (displayOffset != null) {
                    this.displayOffsetX = (float) displayOffset.getDouble("x", -0.5);
                    this.displayOffsetY = (float) displayOffset.getDouble("y", 0.1);
                    this.displayOffsetZ = (float) displayOffset.getDouble("z", 0.5);
                } else {
                    this.displayOffsetX = -0.5f;
                    this.displayOffsetY = 0.1f;
                    this.displayOffsetZ = 0.5f;
                }

                // Load origin offset values directly
                ConfigurationSection originOffset = section.getConfigurationSection("origin_offset");
                if (originOffset != null) {
                    this.originOffsetForward = originOffset.getDouble("forward", 0.5);
                    this.originOffsetUp = originOffset.getDouble("up", 0.1);
                    this.originOffsetBack = originOffset.getDouble("back", -0.25);
                } else {
                    this.originOffsetForward = 0.5;
                    this.originOffsetUp = 0.1;
                    this.originOffsetBack = -0.25;
                }

                this.rotationSpeed = new RotationSpeedConfig(section.getConfigurationSection("rotation_speed"));
            } else {
                // Defaults if section missing
                this.gravityDamper = 46.0;
                this.trajectoryRotation = 0.03696;
                this.displayOffsetX = -0.5f;
                this.displayOffsetY = 0.1f;
                this.displayOffsetZ = 0.5f;
                this.originOffsetForward = 0.5;
                this.originOffsetUp = 0.1;
                this.originOffsetBack = -0.25;
                this.rotationSpeed = new RotationSpeedConfig(null);
            }
        }
    }

    @Getter
    public static class RotationSpeedConfig {
        private final double sword;
        private final double axe;
        private final double hoe;
        private final double pickaxe;
        private final double shovel;
        private final double shield;
        private final double defaultSpeed;

        public RotationSpeedConfig(ConfigurationSection section) {
            if (section != null) {
                this.sword = section.getDouble("sword", 0.0);
                this.axe = section.getDouble("axe", -Math.PI / 8);
                this.hoe = section.getDouble("hoe", -Math.PI / 8);
                this.pickaxe = section.getDouble("pickaxe", -Math.PI / 8);
                this.shovel = section.getDouble("shovel", -Math.PI / 8);
                this.shield = section.getDouble("shield", -Math.PI / 8);
                this.defaultSpeed = section.getDouble("default", Math.PI / 32);
            } else {
                this.sword = 0.0;
                this.axe = -Math.PI / 8;
                this.hoe = -Math.PI / 8;
                this.pickaxe = -Math.PI / 8;
                this.shovel = -Math.PI / 8;
                this.shield = -Math.PI / 8;
                this.defaultSpeed = Math.PI / 32;
            }
        }
    }

    @Getter
    public static class AttackVelocityConfig {
        // Flattened grounded damping (2 simple values - no wrapper class needed)
        private final double groundedDampingHorizontal;
        private final double groundedDampingVertical;

        // Flattened knockback (3 simple values - no wrapper class needed)
        private final double knockbackVerticalBase;
        private final double knockbackHorizontalModifier;
        private final double knockbackNormalMultiplier;

        public AttackVelocityConfig(ConfigurationSection section) {
            if (section != null) {
                // Load grounded damping values directly
                ConfigurationSection groundedDamping = section.getConfigurationSection("grounded_damping");
                if (groundedDamping != null) {
                    this.groundedDampingHorizontal = groundedDamping.getDouble("horizontal", 0.3);
                    this.groundedDampingVertical = groundedDamping.getDouble("vertical", 0.4);
                } else {
                    this.groundedDampingHorizontal = 0.3;
                    this.groundedDampingVertical = 0.4;
                }

                // Load knockback values directly
                ConfigurationSection knockback = section.getConfigurationSection("knockback");
                if (knockback != null) {
                    this.knockbackVerticalBase = knockback.getDouble("vertical_base", 0.25);
                    this.knockbackHorizontalModifier = knockback.getDouble("horizontal_modifier", 0.1);
                    this.knockbackNormalMultiplier = knockback.getDouble("normal_multiplier", 0.7);
                } else {
                    this.knockbackVerticalBase = 0.25;
                    this.knockbackHorizontalModifier = 0.1;
                    this.knockbackNormalMultiplier = 0.7;
                }
            } else {
                this.groundedDampingHorizontal = 0.3;
                this.groundedDampingVertical = 0.4;
                this.knockbackVerticalBase = 0.25;
                this.knockbackHorizontalModifier = 0.1;
                this.knockbackNormalMultiplier = 0.7;
            }
        }
    }
}
