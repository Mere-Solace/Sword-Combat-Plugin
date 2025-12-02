package btm.sword.system.entity.base;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.util.Vector;

import btm.sword.system.control.PredicateRunnablePair;
import btm.sword.system.control.TimeArbiter;
import btm.sword.system.entity.types.Combatant;
import btm.sword.utility.Prefab;
import btm.sword.utility.display.ParticleWrapper;

public class SoulfireManager {
    public static void transferSoulfire(Combatant receiver, SwordEntity hit, float totalAmount) {
        float remainder = totalAmount;
        List<Float> packets = new ArrayList<>();

        // Split into increments up to 5, preferring larger chunks first
        while (remainder > 0) {
            if (remainder >= 5.0f) {
                packets.add(5.0f);
                remainder -= 5.0f;
            } else if (remainder >= 1.0f) {
                packets.add(1.0f);
                remainder -= 1.0f;
            } else if (remainder >= 0.5f) {
                packets.add(0.5f);
                remainder -= 0.5f;
            } else {
                // residual less than 0.5
                packets.add(remainder);
                remainder = 0;
            }
        }

        for (float packetAmount : packets) {
            spawnSoulfirePacket(receiver, hit, packetAmount);
        }
    }

    private static void spawnSoulfirePacket(Combatant receiver, SwordEntity hit, float packetAmount) {
        Location startLoc = hit.getChestLocation();
        Location[] currentLoc = new Location[]{startLoc.clone()};

        double speed = 0.25;
        int period = 25; // milliseconds
        double endDistance = 0.75;

        // Scale particle size dynamically: twice the packetAmount
        final float scaleFactor = packetAmount * packetAmount / 2.0f;

        // Random normalized vector for initial random direction (arc start)
        Vector initialDirection = new Vector(
            (Math.random() - 0.5) * 2,
            (Math.random() - 0.5) * 2,
            (Math.random()- 0.5) * 2
        ).normalize();

        int maxIterations = 300;
        int lerpIterationsBeforeFullFollow = 7;

        final int[] iterationsElapsed = {0};
        AtomicReference<Vector> toPlayer = new AtomicReference<>();
        TimeArbiter.runTimeAffectedTaskOnTimer(
            () -> {}, // no pre-check actions required.
            () -> {
                toPlayer.set(receiver.getChestLocation().toVector().subtract(currentLoc[0].toVector()));
                if (iterationsElapsed[0] <= lerpIterationsBeforeFullFollow) {
                    // Calculate blend factor 0 -> 1 over lifetime for path lerp
                    double t = (double) iterationsElapsed[0] / lerpIterationsBeforeFullFollow;

                    // Interpolate direction from initial random arc direction to direct player direction
                    Vector blendedDirection = initialDirection.clone().multiply(1 - t)
                        .add(toPlayer.get().clone().normalize().multiply(t)).normalize();

                    // Move current location step along blended direction
                    currentLoc[0].add(blendedDirection.multiply(speed));
                } else {
                    currentLoc[0].add(toPlayer.get().clone().normalize().multiply(speed));
                }

                // Display scaled particles with SOUL_FIRE_FLAME & SMOKE combination
                new ParticleWrapper(Particle.SMOKE, (int) scaleFactor,
                    0.025, 0.025, 0.025, 0.0001)
                    .display(currentLoc[0]);
                Prefab.Particles.UMBRAL_FLAME.display(currentLoc[0]);

                iterationsElapsed[0]++;
            },
            () -> {
                new ParticleWrapper(Particle.SMOKE, (int) scaleFactor,
                    0.025, 0.025, 0.025, 0.0001)
                    .display(currentLoc[0]);
                Prefab.Particles.UMBRAL_FLAME.display(currentLoc[0]);
            },

            period,

            new PredicateRunnablePair(
                receiver::isDead,
                () -> Prefab.Particles.SMOKE.display(currentLoc[0])),

            new PredicateRunnablePair(
                () -> iterationsElapsed[0] > 0 && toPlayer.get().lengthSquared() <= endDistance * endDistance,
                () -> {
                    Prefab.Particles.SOULFIRE_POOF.display(receiver.getChestLocation());
                    if (!receiver.isDead()) {
                        deliverSoulfire(receiver, packetAmount);
                    }
                }),

            new PredicateRunnablePair(
                () -> iterationsElapsed[0] > maxIterations,
                () -> {}
            )
        );
    }

    private static void deliverSoulfire(Combatant receiver, float amount) {
        receiver.aspects.soulfire().add(amount);
        Prefab.Sounds.SOULFIRE_GAIN_BACKGROUND.playForAllInRadius(receiver.self());
    }
}
