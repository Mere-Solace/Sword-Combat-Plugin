package btm.sword.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Type-safe accessor for physics-related configuration values.
 * <p>
 * Handles projectile motion, gravity, velocity, and rotation settings.
 * Values are cached on construction for performance.
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
        private final DisplayOffsetConfig displayOffset;
        private final OriginOffsetConfig originOffset;
        private final RotationSpeedConfig rotationSpeed;

        public ThrownItemsConfig(ConfigurationSection section) {
            if (section != null) {
                this.gravityDamper = section.getDouble("gravity_damper", 46.0);
                this.trajectoryRotation = section.getDouble("trajectory_rotation", 0.03696);
                this.displayOffset = new DisplayOffsetConfig(section.getConfigurationSection("display_offset"));
                this.originOffset = new OriginOffsetConfig(section.getConfigurationSection("origin_offset"));
                this.rotationSpeed = new RotationSpeedConfig(section.getConfigurationSection("rotation_speed"));
            } else {
                // Defaults if section missing
                this.gravityDamper = 46.0;
                this.trajectoryRotation = 0.03696;
                this.displayOffset = new DisplayOffsetConfig(null);
                this.originOffset = new OriginOffsetConfig(null);
                this.rotationSpeed = new RotationSpeedConfig(null);
            }
        }
    }

    @Getter
    public static class DisplayOffsetConfig {
        private final float x;
        private final float y;
        private final float z;

        public DisplayOffsetConfig(ConfigurationSection section) {
            if (section != null) {
                this.x = (float) section.getDouble("x", -0.5);
                this.y = (float) section.getDouble("y", 0.1);
                this.z = (float) section.getDouble("z", 0.5);
            } else {
                this.x = -0.5f;
                this.y = 0.1f;
                this.z = 0.5f;
            }
        }
    }

    @Getter
    public static class OriginOffsetConfig {
        private final double forward;
        private final double up;
        private final double back;

        public OriginOffsetConfig(ConfigurationSection section) {
            if (section != null) {
                this.forward = section.getDouble("forward", 0.5);
                this.up = section.getDouble("up", 0.1);
                this.back = section.getDouble("back", -0.25);
            } else {
                this.forward = 0.5;
                this.up = 0.1;
                this.back = -0.25;
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
        private final GroundedDampingConfig groundedDamping;
        private final KnockbackConfig knockback;

        public AttackVelocityConfig(ConfigurationSection section) {
            if (section != null) {
                this.groundedDamping = new GroundedDampingConfig(section.getConfigurationSection("grounded_damping"));
                this.knockback = new KnockbackConfig(section.getConfigurationSection("knockback"));
            } else {
                this.groundedDamping = new GroundedDampingConfig(null);
                this.knockback = new KnockbackConfig(null);
            }
        }
    }

    @Getter
    public static class GroundedDampingConfig {
        private final double horizontal;
        private final double vertical;

        public GroundedDampingConfig(ConfigurationSection section) {
            if (section != null) {
                this.horizontal = section.getDouble("horizontal", 0.3);
                this.vertical = section.getDouble("vertical", 0.4);
            } else {
                this.horizontal = 0.3;
                this.vertical = 0.4;
            }
        }
    }

    @Getter
    public static class KnockbackConfig {
        private final double verticalBase;
        private final double horizontalModifier;
        private final double normalMultiplier;

        public KnockbackConfig(ConfigurationSection section) {
            if (section != null) {
                this.verticalBase = section.getDouble("vertical_base", 0.25);
                this.horizontalModifier = section.getDouble("horizontal_modifier", 0.1);
                this.normalMultiplier = section.getDouble("normal_multiplier", 0.7);
            } else {
                this.verticalBase = 0.25;
                this.horizontalModifier = 0.1;
                this.normalMultiplier = 0.7;
            }
        }
    }
}
