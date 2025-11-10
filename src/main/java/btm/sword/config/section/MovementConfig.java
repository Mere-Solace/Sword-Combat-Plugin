package btm.sword.config.section;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import lombok.Getter;

/**
 * Type-safe accessor for movement action configuration values.
 * <p>
 * Uses flat field pattern (no nested classes) since all values are simple primitives.
 * Includes dash mechanics, toss actions, and related physics parameters.
 * </p>
 */
@Getter
public class MovementConfig {
    // === DASH CONFIGURATION ===

    // Dash mechanics
    private final double dashMaxDistance;
    private final int dashCastDuration;
    private final double dashBasePower;

    // Dash offsets and positioning
    private final double dashInitialOffsetY;
    private final double dashImpedanceCheckOffsetY;
    private final double dashForwardMultiplier;
    private final double dashUpwardMultiplier;
    private final double dashUpwardBoost;

    // Dash detection and hitboxes
    private final double dashRayHitboxRadius;
    private final double dashSecantRadius;
    private final double dashGrabDistanceSquared;
    private final double dashBlockCheckOffsetY;

    // Dash velocity and movement
    private final double dashVelocityDamping;

    // Dash timing
    private final long dashParticleTaskDelay;
    private final long dashParticleTaskPeriod;
    private final int dashParticleTimerIncrement;
    private final int dashParticleTimerThreshold;
    private final long dashGrabCheckDelay;
    private final long dashVelocityTaskDelay;
    private final long dashVelocityTaskPeriod;

    // Dash particle effects
    private final int dashParticleCount;
    private final double dashParticleSpreadX;
    private final double dashParticleSpreadY;
    private final double dashParticleSpreadZ;

    // Dash sound effects
    private final float dashFlapSoundVolume;
    private final float dashFlapSoundPitch;
    private final float dashSweepSoundVolume;
    private final float dashSweepSoundPitch;

    // === TOSS CONFIGURATION ===

    // Toss force and power
    private final double tossBaseForce;
    private final double tossMightMultiplierBase;
    private final double tossMightMultiplierIncrement;

    // Toss velocity phases
    private final int tossUpwardPhaseIterations;
    private final double tossUpwardVelocityY;
    private final int tossForwardPhaseIterations;
    private final int tossAnimationIterations;

    // Toss positioning and offsets
    private final double tossLocationOffsetMultiplier;
    private final double tossParticleHeightMultiplier;
    private final double tossRayTraceDistanceMultiplier;

    // Toss collision detection
    private final double tossEntityDetectionRadius;
    private final double tossKnockbackMultiplier;

    // Toss damage and impact
    private final float tossExplosionPower;
    private final int tossHitInvulnerabilityTicks;
    private final int tossHitShardDamage;
    private final float tossHitToughnessDamage;
    private final float tossHitSoulfireReduction;

    public MovementConfig(FileConfiguration config) {
        ConfigurationSection movement = config.getConfigurationSection("movement");

        if (movement != null) {
            // Dash configuration
            ConfigurationSection dash = movement.getConfigurationSection("dash");
            if (dash != null) {
                this.dashMaxDistance = dash.getDouble("max_distance", 10.0);
                this.dashCastDuration = dash.getInt("cast_duration", 5);
                this.dashBasePower = dash.getDouble("base_power", 0.7);

                this.dashInitialOffsetY = dash.getDouble("initial_offset_y", 0.3);
                this.dashImpedanceCheckOffsetY = dash.getDouble("impedance_check_offset_y", 0.5);
                this.dashForwardMultiplier = dash.getDouble("forward_multiplier", 0.5);
                this.dashUpwardMultiplier = dash.getDouble("upward_multiplier", 0.15);
                this.dashUpwardBoost = dash.getDouble("upward_boost", 0.05);

                this.dashRayHitboxRadius = dash.getDouble("ray_hitbox_radius", 0.7);
                this.dashSecantRadius = dash.getDouble("secant_radius", 0.3);
                this.dashGrabDistanceSquared = dash.getDouble("grab_distance_squared", 8.5);
                this.dashBlockCheckOffsetY = dash.getDouble("block_check_offset_y", -0.75);

                this.dashVelocityDamping = dash.getDouble("velocity_damping", 0.6);

                this.dashParticleTaskDelay = dash.getLong("particle_task_delay", 0L);
                this.dashParticleTaskPeriod = dash.getLong("particle_task_period", 2L);
                this.dashParticleTimerIncrement = dash.getInt("particle_timer_increment", 2);
                this.dashParticleTimerThreshold = dash.getInt("particle_timer_threshold", 4);
                this.dashGrabCheckDelay = dash.getLong("grab_check_delay", 4L);
                this.dashVelocityTaskDelay = dash.getLong("velocity_task_delay", 0L);
                this.dashVelocityTaskPeriod = dash.getLong("velocity_task_period", 1L);

                this.dashParticleCount = dash.getInt("particle_count", 100);
                this.dashParticleSpreadX = dash.getDouble("particle_spread_x", 1.25);
                this.dashParticleSpreadY = dash.getDouble("particle_spread_y", 1.25);
                this.dashParticleSpreadZ = dash.getDouble("particle_spread_z", 1.25);

                this.dashFlapSoundVolume = (float) dash.getDouble("flap_sound_volume", 0.6);
                this.dashFlapSoundPitch = (float) dash.getDouble("flap_sound_pitch", 1.0);
                this.dashSweepSoundVolume = (float) dash.getDouble("sweep_sound_volume", 0.3);
                this.dashSweepSoundPitch = (float) dash.getDouble("sweep_sound_pitch", 0.6);
            } else {
                // Dash defaults
                this.dashMaxDistance = 10.0;
                this.dashCastDuration = 5;
                this.dashBasePower = 0.7;
                this.dashInitialOffsetY = 0.3;
                this.dashImpedanceCheckOffsetY = 0.5;
                this.dashForwardMultiplier = 0.5;
                this.dashUpwardMultiplier = 0.15;
                this.dashUpwardBoost = 0.05;
                this.dashRayHitboxRadius = 0.7;
                this.dashSecantRadius = 0.3;
                this.dashGrabDistanceSquared = 8.5;
                this.dashBlockCheckOffsetY = -0.75;
                this.dashVelocityDamping = 0.6;
                this.dashParticleTaskDelay = 0L;
                this.dashParticleTaskPeriod = 2L;
                this.dashParticleTimerIncrement = 2;
                this.dashParticleTimerThreshold = 4;
                this.dashGrabCheckDelay = 4L;
                this.dashVelocityTaskDelay = 0L;
                this.dashVelocityTaskPeriod = 1L;
                this.dashParticleCount = 100;
                this.dashParticleSpreadX = 1.25;
                this.dashParticleSpreadY = 1.25;
                this.dashParticleSpreadZ = 1.25;
                this.dashFlapSoundVolume = 0.6f;
                this.dashFlapSoundPitch = 1.0f;
                this.dashSweepSoundVolume = 0.3f;
                this.dashSweepSoundPitch = 0.6f;
            }

            // Toss configuration
            ConfigurationSection toss = movement.getConfigurationSection("toss");
            if (toss != null) {
                this.tossBaseForce = toss.getDouble("base_force", 1.5);
                this.tossMightMultiplierBase = toss.getDouble("might_multiplier_base", 2.5);
                this.tossMightMultiplierIncrement = toss.getDouble("might_multiplier_increment", 0.1);

                this.tossUpwardPhaseIterations = toss.getInt("upward_phase_iterations", 2);
                this.tossUpwardVelocityY = toss.getDouble("upward_velocity_y", 0.25);
                this.tossForwardPhaseIterations = toss.getInt("forward_phase_iterations", 3);
                this.tossAnimationIterations = toss.getInt("animation_iterations", 15);

                this.tossLocationOffsetMultiplier = toss.getDouble("location_offset_multiplier", 0.3);
                this.tossParticleHeightMultiplier = toss.getDouble("particle_height_multiplier", 0.5);
                this.tossRayTraceDistanceMultiplier = toss.getDouble("ray_trace_distance_multiplier", 0.6);

                this.tossEntityDetectionRadius = toss.getDouble("entity_detection_radius", 0.4);
                this.tossKnockbackMultiplier = toss.getDouble("knockback_multiplier", 0.3);

                this.tossExplosionPower = (float) toss.getDouble("explosion_power", 2.0);
                this.tossHitInvulnerabilityTicks = toss.getInt("hit_invulnerability_ticks", 3);
                this.tossHitShardDamage = toss.getInt("hit_shard_damage", 2);
                this.tossHitToughnessDamage = (float) toss.getDouble("hit_toughness_damage", 30.0);
                this.tossHitSoulfireReduction = (float) toss.getDouble("hit_soulfire_reduction", 5.0);
            } else {
                // Toss defaults
                this.tossBaseForce = 1.5;
                this.tossMightMultiplierBase = 2.5;
                this.tossMightMultiplierIncrement = 0.1;
                this.tossUpwardPhaseIterations = 2;
                this.tossUpwardVelocityY = 0.25;
                this.tossForwardPhaseIterations = 3;
                this.tossAnimationIterations = 15;
                this.tossLocationOffsetMultiplier = 0.3;
                this.tossParticleHeightMultiplier = 0.5;
                this.tossRayTraceDistanceMultiplier = 0.6;
                this.tossEntityDetectionRadius = 0.4;
                this.tossKnockbackMultiplier = 0.3;
                this.tossExplosionPower = 2.0f;
                this.tossHitInvulnerabilityTicks = 3;
                this.tossHitShardDamage = 2;
                this.tossHitToughnessDamage = 30.0f;
                this.tossHitSoulfireReduction = 5.0f;
            }
        } else {
            // Movement section missing - use all defaults
            this.dashMaxDistance = 10.0;
            this.dashCastDuration = 5;
            this.dashBasePower = 0.7;
            this.dashInitialOffsetY = 0.3;
            this.dashImpedanceCheckOffsetY = 0.5;
            this.dashForwardMultiplier = 0.5;
            this.dashUpwardMultiplier = 0.15;
            this.dashUpwardBoost = 0.05;
            this.dashRayHitboxRadius = 0.7;
            this.dashSecantRadius = 0.3;
            this.dashGrabDistanceSquared = 8.5;
            this.dashBlockCheckOffsetY = -0.75;
            this.dashVelocityDamping = 0.6;
            this.dashParticleTaskDelay = 0L;
            this.dashParticleTaskPeriod = 2L;
            this.dashParticleTimerIncrement = 2;
            this.dashParticleTimerThreshold = 4;
            this.dashGrabCheckDelay = 4L;
            this.dashVelocityTaskDelay = 0L;
            this.dashVelocityTaskPeriod = 1L;
            this.dashParticleCount = 100;
            this.dashParticleSpreadX = 1.25;
            this.dashParticleSpreadY = 1.25;
            this.dashParticleSpreadZ = 1.25;
            this.dashFlapSoundVolume = 0.6f;
            this.dashFlapSoundPitch = 1.0f;
            this.dashSweepSoundVolume = 0.3f;
            this.dashSweepSoundPitch = 0.6f;

            this.tossBaseForce = 1.5;
            this.tossMightMultiplierBase = 2.5;
            this.tossMightMultiplierIncrement = 0.1;
            this.tossUpwardPhaseIterations = 2;
            this.tossUpwardVelocityY = 0.25;
            this.tossForwardPhaseIterations = 3;
            this.tossAnimationIterations = 15;
            this.tossLocationOffsetMultiplier = 0.3;
            this.tossParticleHeightMultiplier = 0.5;
            this.tossRayTraceDistanceMultiplier = 0.6;
            this.tossEntityDetectionRadius = 0.4;
            this.tossKnockbackMultiplier = 0.3;
            this.tossExplosionPower = 2.0f;
            this.tossHitInvulnerabilityTicks = 3;
            this.tossHitShardDamage = 2;
            this.tossHitToughnessDamage = 30.0f;
            this.tossHitSoulfireReduction = 5.0f;
        }
    }
}
