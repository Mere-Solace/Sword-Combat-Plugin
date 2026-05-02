package btm.sword.scene.fake;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.destroystokyo.paper.profile.ProfileProperty;

import btm.sword.Sword;
import btm.sword.entity.player.SwordPlayer;
import btm.sword.runtime.scheduler.SwordScheduler;
import btm.sword.scene.animation.FakeAnimation;

/**
 * Manages packet-based fake player NPCs for the static menu scene preview.
 * <p>
 * Each real player can have at most one active fake NPC at a time.
 * The NPC is purely packet-based — it exists in no Bukkit entity registry
 * and is visible only to its owning player.
 * The NPC renders with the real player's current skin and equipped armor.
 * </p>
 *
 * <h2>Packet strategy</h2>
 * <ul>
 *   <li><b>NMS reflection</b> — {@code ClientboundPlayerInfoUpdatePacket} and
 *       {@code ClientboundPlayerInfoRemovePacket}. ProtocolLib's wrapper cannot correctly
 *       encode these packets in Paper 1.21.8 (its {@code PlayerInfoData} does not convert
 *       to the NMS {@code Entry} record). Paper 1.21.8 ships with Mojang-mapped NMS class
 *       names, so they are accessible via {@link Class#forName} at runtime without
 *       Paperweight or any extra compilation dependency.</li>
 *   <li><b>ProtocolLib</b> — {@code SPAWN_ENTITY}, {@code ENTITY_HEAD_ROTATION},
 *       {@code ENTITY_EQUIPMENT}, {@code ENTITY_DESTROY}. These packets work correctly
 *       through ProtocolLib's standard accessors.</li>
 * </ul>
 *
 * <h2>Spawn packet sequence</h2>
 * <ol>
 *   <li>NMS {@code ClientboundPlayerInfoUpdatePacket} (ADD_PLAYER + UPDATE_LISTED) — registers
 *       the GameProfile with the client so the player entity renders with the correct skin.</li>
 *   <li>ProtocolLib {@code SPAWN_ENTITY} — places the entity in the world.</li>
 *   <li>ProtocolLib {@code ENTITY_HEAD_ROTATION} — aligns head yaw.</li>
 *   <li>ProtocolLib {@code ENTITY_EQUIPMENT} — sends all six equipment slots.</li>
 *   <li>Deferred NMS {@code ClientboundPlayerInfoRemovePacket} — removes the NPC from the tab list.</li>
 * </ol>
 */
public final class FakePlayerManager {

    private record FakePlayerData(int entityId, UUID fakeUuid, Player player) {}

    private static final Map<UUID, FakePlayerData> ACTIVE = new HashMap<>();

    /** Negative ID counter; decrements per spawn to avoid colliding with real server entity IDs. */
    private static int nextEntityId = -1000;

    // NMS class name constants (Mojang-mapped, available at runtime on Paper 1.20.5+)
    private static final String NMS_PLAYER_INFO_UPDATE =
        "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket";
    private static final String NMS_PLAYER_INFO_REMOVE =
        "net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket";
    private static final String NMS_GAME_TYPE = "net.minecraft.world.level.GameType";

    private FakePlayerManager() {}

    /**
     * Spawns a packet-based fake player NPC at the given location, visible only to the viewer.
     * <p>
     * If the player already has an active NPC, it is despawned first.
     * Equipment is snapshotted from the player's current inventory at call time.
     * </p>
     *
     * @param viewer   the player who will see the NPC
     * @param location the world position to spawn the NPC at
     */
    public static void spawnFakePlayer(SwordPlayer viewer, Location location) {
        Player player = viewer.player();
        UUID realUuid = player.getUniqueId();

        if (ACTIVE.containsKey(realUuid)) {
            despawnFakePlayer(viewer);
        }

        int entityId = nextEntityId--;
        UUID fakeUuid = new UUID(
            realUuid.getMostSignificantBits() ^ 0xDEADBEEFL,
            realUuid.getLeastSignificantBits()
        );

        FakePlayerData data = new FakePlayerData(entityId, fakeUuid, player);
        ACTIVE.put(realUuid, data);

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();

        sendPlayerInfoAdd(player, fakeUuid);
        sendSpawnEntity(manager, player, entityId, fakeUuid, location);
        sendHeadRotation(manager, player, entityId, location.getYaw());
        sendEquipment(manager, player, entityId, player.getEquipment());
        sendSkinParts(player, entityId);

        // Deferred: remove from tab list so the player does not see a duplicate
        SwordScheduler.runBukkitTaskLater(
            () -> sendPlayerInfoRemove(player, fakeUuid),
            100, TimeUnit.MILLISECONDS
        );
    }

    /**
     * Despawns the fake player NPC for the given viewer, removing it client-side.
     * Does nothing if no NPC is active for this player.
     *
     * @param viewer the player whose NPC to remove
     */
    public static void despawnFakePlayer(SwordPlayer viewer) {
        FakePlayerData data = ACTIVE.remove(viewer.player().getUniqueId());
        if (data == null) return;

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        Player player = viewer.player();

        sendEntityDestroy(manager, player, data.entityId());
        sendPlayerInfoRemove(player, data.fakeUuid());
    }

    /**
     * Resends the equipment packets for the active NPC using the player's current inventory.
     * Call this if the player's equipment changes while the scene is active.
     *
     * @param viewer the player whose NPC to refresh
     */
    public static void updateEquipment(SwordPlayer viewer) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return;

        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        Player player = viewer.player();
        sendEquipment(manager, player, data.entityId(), player.getEquipment());
    }

    /**
     * Returns whether an active fake player NPC exists for the given viewer.
     *
     * @param viewer the player to check
     * @return {@code true} if a fake player is currently spawned for this viewer
     */
    public static boolean isActive(SwordPlayer viewer) {
        return ACTIVE.containsKey(viewer.player().getUniqueId());
    }

    /**
     * Moves the fake player NPC by the given relative delta.
     * Maximum supported delta per axis is approximately ±8 blocks
     * (short fixed-point limit at 4096 units/block).
     * For larger displacements use {@link #teleportFake} instead.
     *
     * @param viewer    the player whose NPC to move
     * @param dx        X delta in blocks
     * @param dy        Y delta in blocks
     * @param dz        Z delta in blocks
     * @param onGround  whether the entity is touching the ground
     * @return {@code true} if the packet was sent without error
     */
    public static boolean moveRelative(SwordPlayer viewer, double dx, double dy, double dz, boolean onGround) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        if (Math.abs(dx) > 7.9 || Math.abs(dy) > 7.9 || Math.abs(dz) > 7.9) {
            Sword.getInstance().getLogger().warning(
                "[FakePlayerManager] moveRelative delta exceeds ±8 block limit; use teleportFake for large displacements");
        }
        try {
            sendMoveRelative(ProtocolLibrary.getProtocolManager(), data.player(), data.entityId(), dx, dy, dz, onGround);
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send REL_ENTITY_MOVE for " + viewer.player().getName(), e);
            return false;
        }
    }

    /**
     * Teleports the fake player NPC to an absolute world position.
     * Unlike {@link #moveRelative}, there is no distance limit;
     * the client snaps immediately with no lerp interpolation.
     *
     * @param viewer   the player whose NPC to teleport
     * @param location the destination (yaw/pitch are applied)
     * @return {@code true} if the packet was sent without error
     */
    public static boolean teleportFake(SwordPlayer viewer, Location location) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        try {
            sendTeleport(ProtocolLibrary.getProtocolManager(), data.player(), data.entityId(), location);
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send ENTITY_TELEPORT for " + viewer.player().getName(), e);
            return false;
        }
    }

    /**
     * Rotates the fake player NPC to face the given yaw/pitch.
     * Sends both {@code ENTITY_LOOK} (body rotation) and {@code ENTITY_HEAD_ROTATION}
     * (head yaw) so the head and body stay aligned.
     *
     * @param viewer   the player whose NPC to rotate
     * @param yaw      yaw angle in degrees (0 = south, 90 = west, 180/−180 = north, −90 = east)
     * @param pitch    pitch angle in degrees (−90 = looking straight up, 90 = straight down)
     * @param onGround whether the entity is touching the ground
     * @return {@code true} if both packets were sent without error
     */
    public static boolean rotateFake(SwordPlayer viewer, float yaw, float pitch, boolean onGround) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        try {
            sendRotate(ProtocolLibrary.getProtocolManager(), data.player(), data.entityId(), yaw, pitch, onGround);
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send ENTITY_LOOK/ENTITY_HEAD_ROTATION for " + viewer.player().getName(), e);
            return false;
        }
    }

    /**
     * Plays a one-shot animation on the fake player NPC via {@code ClientboundAnimatePacket}.
     *
     * @param viewer    the player whose NPC to animate
     * @param animation the animation to play
     * @return {@code true} if the packet was sent without error
     */
    public static boolean animateFake(SwordPlayer viewer, FakeAnimation animation) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        try {
            sendAnimation(ProtocolLibrary.getProtocolManager(), data.player(), data.entityId(), animation.getId());
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send ANIMATION (" + animation.name() + ") for " + viewer.player().getName(), e);
            return false;
        }
    }

    /**
     * Sends a velocity impulse to the fake player NPC.
     * The client receives this as a visual impulse only; the server does not simulate
     * ongoing physics. Follow up with position packets (e.g. {@link #moveRelative})
     * to drive the actual trajectory each tick.
     *
     * <p>Units: 1/8000 of a block per tick. A value of {@code 1.0} represents 8000 units
     * (the maximum short range). Typical jump velocity is approximately {@code 0.42}.</p>
     *
     * @param viewer the player whose NPC to impulse
     * @param vx     X velocity in blocks/tick
     * @param vy     Y velocity in blocks/tick (positive = up)
     * @param vz     Z velocity in blocks/tick
     * @return {@code true} if the packet was sent without error
     */
    public static boolean velocityFake(SwordPlayer viewer, double vx, double vy, double vz) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        try {
            sendVelocity(ProtocolLibrary.getProtocolManager(), data.player(), data.entityId(), vx, vy, vz);
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send ENTITY_VELOCITY for " + viewer.player().getName(), e);
            return false;
        }
    }

    // =========================================================================
    // ProtocolLib helpers — movement, rotation, animation, velocity
    // (private; throw on failure so public wrappers own the catch/log)
    // =========================================================================

    private static void sendMoveRelative(ProtocolManager manager, Player player, int entityId,
            double dx, double dy, double dz, boolean onGround) throws Exception {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.REL_ENTITY_MOVE);
        packet.getIntegers().write(0, entityId);
        packet.getShorts().write(0, (short) Math.round(dx * 4096));
        packet.getShorts().write(1, (short) Math.round(dy * 4096));
        packet.getShorts().write(2, (short) Math.round(dz * 4096));
        packet.getBooleans().write(0, onGround);
        manager.sendServerPacket(player, packet);
    }

    /**
     * Builds {@code ClientboundTeleportEntityPacket} via NMS reflection.
     * <p>
     * In Paper 1.21.8, {@code ClientboundTeleportEntityPacket} stores position/rotation
     * inside a {@code PositionMoveRotation} record, so ProtocolLib's {@code getIntegers()}
     * and {@code getDoubles()} modifiers return empty — the same class-layout issue seen
     * with player-info and entity-data packets. We bypass the ProtocolLib modifier entirely.
     * </p>
     * <p>
     * NMS path: {@code ClientboundTeleportEntityPacket(int id, PositionMoveRotation change,
     * Set<Relative> relatives, boolean onGround)}.
     * An empty {@code relatives} set means all coordinates are absolute (not relative deltas).
     * </p>
     */
    private static void sendTeleport(ProtocolManager manager, Player player, int entityId,
            Location location) throws Exception {
        // Vec3(x, y, z) — position
        Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
        Constructor<?> vec3Ctor = vec3Class.getDeclaredConstructor(double.class, double.class, double.class);
        Object position = vec3Ctor.newInstance(location.getX(), location.getY(), location.getZ());
        Object zeroDelta = vec3Ctor.newInstance(0.0, 0.0, 0.0);

        // PositionMoveRotation(Vec3 position, Vec3 delta, float yRot, float xRot)
        // yRot = yaw, xRot = pitch (NMS naming convention)
        Class<?> pmrClass = Class.forName("net.minecraft.world.entity.PositionMoveRotation");
        Constructor<?> pmrCtor = pmrClass.getDeclaredConstructor(vec3Class, vec3Class, float.class, float.class);
        pmrCtor.setAccessible(true);
        Object pmr = pmrCtor.newInstance(position, zeroDelta, location.getYaw(), location.getPitch());

        // ClientboundTeleportEntityPacket(int id, PositionMoveRotation change, Set<Relative> relatives, boolean onGround)
        // Empty relatives set = all axes are absolute
        Class<?> packetClass = Class.forName(
            "net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket");
        Constructor<?> packetCtor = packetClass.getDeclaredConstructor(int.class, pmrClass, Set.class, boolean.class);
        packetCtor.setAccessible(true);
        Object nmsPacket = packetCtor.newInstance(entityId, pmr, Set.of(), false);

        manager.sendServerPacket(player, PacketContainer.fromPacket(nmsPacket));
    }

    private static void sendRotate(ProtocolManager manager, Player player, int entityId,
            float yaw, float pitch, boolean onGround) throws Exception {
        // Body rotation
        PacketContainer lookPacket = manager.createPacket(PacketType.Play.Server.ENTITY_LOOK);
        lookPacket.getIntegers().write(0, entityId);
        lookPacket.getBytes().write(0, angleToByte(yaw));
        lookPacket.getBytes().write(1, angleToByte(pitch));
        lookPacket.getBooleans().write(0, onGround);
        manager.sendServerPacket(player, lookPacket);
        // Head yaw — must match body yaw or the head twists independently of the body
        PacketContainer headPacket = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
        headPacket.getIntegers().write(0, entityId);
        headPacket.getBytes().write(0, angleToByte(yaw));
        manager.sendServerPacket(player, headPacket);
    }

    private static void sendAnimation(ProtocolManager manager, Player player, int entityId,
            int animationId) throws Exception {
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.ANIMATION);
        packet.getIntegers().write(0, entityId);
        packet.getIntegers().write(1, animationId);
        manager.sendServerPacket(player, packet);
    }

    /**
     * Builds {@code ClientboundSetEntityMotionPacket} via NMS reflection.
     * <p>
     * In Paper 1.21.8, {@code ClientboundSetEntityMotionPacket} stores velocity components
     * as fields derived from a {@code Vec3}, so ProtocolLib's {@code getIntegers()} and
     * {@code getShorts()} modifiers return empty. We bypass ProtocolLib's modifier entirely.
     * </p>
     * <p>
     * NMS path: {@code ClientboundSetEntityMotionPacket(int id, Vec3 velocity)}.
     * The constructor clamps each axis to ±3.9 blocks/tick and converts to the wire
     * format (multiply by 8000, cast to int).
     * </p>
     */
    private static void sendVelocity(ProtocolManager manager, Player player, int entityId,
            double vx, double vy, double vz) throws Exception {
        // Vec3(x, y, z) — velocity in blocks/tick
        Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
        Constructor<?> vec3Ctor = vec3Class.getDeclaredConstructor(double.class, double.class, double.class);
        Object velocityVec = vec3Ctor.newInstance(vx, vy, vz);

        // ClientboundSetEntityMotionPacket(int id, Vec3 velocity)
        Class<?> packetClass = Class.forName(
            "net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket");
        Constructor<?> packetCtor = packetClass.getDeclaredConstructor(int.class, vec3Class);
        packetCtor.setAccessible(true);
        Object nmsPacket = packetCtor.newInstance(entityId, velocityVec);

        manager.sendServerPacket(player, PacketContainer.fromPacket(nmsPacket));
    }

    /**
     * Sets the {@code LIVING_ENTITY_FLAGS} metadata byte (index 8) on the fake player,
     * controlling item-use pose, active hand selection, and riptide spin state.
     *
     * <p><b>Shield requirement for blocking:</b> setting {@code handActive = true} only renders
     * the blocking animation if the active hand's equipped item is a shield. Equip one first via
     * {@link #setEquipmentSlot(SwordPlayer, EnumWrappers.ItemSlot, ItemStack)} before calling
     * this method.</p>
     *
     * <p>Flag semantics:</p>
     * <ul>
     *   <li><b>handActive</b> ({@code 0x01}) — marks the entity as actively using an item.
     *       When the held item is a shield this produces the blocking stance; with other items
     *       it raises the selected hand into the use-item pose.</li>
     *   <li><b>offhand</b> ({@code 0x02}) — selects which hand the active flag applies to.
     *       {@code false} = main hand, {@code true} = offhand. Only meaningful when
     *       {@code handActive} is {@code true}.</li>
     *   <li><b>riptide</b> ({@code 0x04}) — plays the trident riptide arm-raise and spin pose.
     *       Independent of the other two flags; can be set alone without {@code handActive}.</li>
     * </ul>
     *
     * <p>Pass all {@code false} to clear any active pose and return the NPC to idle stance.</p>
     *
     * <p>Implemented via {@code ClientboundSetEntityDataPacket} with a single
     * {@code SynchedEntityData$DataValue} at index 8, using the NMS {@code BYTE} serializer —
     * the same reflection strategy used by {@link #spawnFakePlayer} for skin parts.</p>
     *
     * @param viewer     the player whose NPC to update
     * @param handActive {@code true} to enter item-use / blocking stance
     * @param offhand    {@code true} to apply the active flag to the offhand instead of the main hand
     * @param riptide    {@code true} to play the riptide trident raise/spin pose
     * @return {@code true} if the metadata packet was sent without error
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean setEntityFlags(SwordPlayer viewer, boolean handActive, boolean offhand, boolean riptide) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        byte flags = (byte) ((handActive ? 0x01 : 0) | (offhand ? 0x02 : 0) | (riptide ? 0x04 : 0));
        try {
            sendEntityFlags(data.player(), data.entityId(), flags);
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send ENTITY_DATA (living entity flags) for "
                    + viewer.player().getName(), e);
            return false;
        }
    }

    /**
     * Sends a single-slot {@code ENTITY_EQUIPMENT} packet to update one equipment slot on the
     * fake player NPC. Use this to equip items (e.g. a shield before calling
     * {@link #setEntityFlags}) without replacing the full equipment set.
     *
     * <p>Does nothing if no NPC is active for the viewer.</p>
     *
     * @param viewer the player whose NPC to update
     * @param slot   the equipment slot to update (MAINHAND, OFFHAND, HEAD, CHEST, LEGS, FEET)
     * @param item   the item to place in the slot; {@code null} is treated as air
     * @return {@code true} if the packet was sent without error
     */
    public static boolean setEquipmentSlot(SwordPlayer viewer, EnumWrappers.ItemSlot slot, ItemStack item) {
        FakePlayerData data = ACTIVE.get(viewer.player().getUniqueId());
        if (data == null) return false;
        try {
            ProtocolManager manager = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            packet.getIntegers().write(0, data.entityId());
            packet.getSlotStackPairLists().write(0,
                Collections.singletonList(new Pair<>(slot, orAir(item))));
            manager.sendServerPacket(data.player(), packet);
            return true;
        } catch (Exception e) {
            Sword.getInstance().getLogger().log(Level.SEVERE,
                "[FakePlayerManager] Failed to send ENTITY_EQUIPMENT (single slot) for "
                    + viewer.player().getName(), e);
            return false;
        }
    }

    /**
     * Builds and sends {@code ClientboundSetEntityDataPacket} with the {@code LIVING_ENTITY_FLAGS}
     * byte at metadata index 8. Throws on any reflection or send failure so the public wrapper
     * can log with a full stack trace.
     */
    private static void sendEntityFlags(Player player, int entityId, byte flags) throws Exception {
        Class<?> serializersClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializers");
        Class<?> serializerClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializer");
        Field byteField = serializersClass.getDeclaredField("BYTE");
        byteField.setAccessible(true);
        Object byteSerializer = byteField.get(null);

        // SynchedEntityData$DataValue(index, serializer, value)
        Class<?> dataValueClass = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue");
        Constructor<?> dataValueCtor = dataValueClass.getDeclaredConstructor(int.class, serializerClass, Object.class);
        dataValueCtor.setAccessible(true);
        Object dataValue = dataValueCtor.newInstance(8, byteSerializer, flags);

        Class<?> packetClass = Class.forName(
            "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
        Constructor<?> packetCtor = packetClass.getDeclaredConstructor(int.class, List.class);
        packetCtor.setAccessible(true);
        Object nmsPacket = packetCtor.newInstance(entityId, List.of(dataValue));

        ProtocolLibrary.getProtocolManager()
            .sendServerPacket(player, PacketContainer.fromPacket(nmsPacket));
    }

    /**
     * Despawns all active fake player NPCs. Called on plugin disable to prevent
     * lingering client-side entities.
     */
    public static void despawnAll() {
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        for (FakePlayerData data : ACTIVE.values()) {
            Player player = data.player();
            if (player != null && player.isOnline()) {
                sendEntityDestroy(manager, player, data.entityId());
                sendPlayerInfoRemove(player, data.fakeUuid());
            }
        }
        ACTIVE.clear();
    }

    // =========================================================================
    // NMS reflection helpers (player info packets)
    // =========================================================================

    /**
     * Sends {@code ClientboundPlayerInfoUpdatePacket} (ADD_PLAYER + UPDATE_LISTED) via NMS
     * reflection. ProtocolLib's {@code PlayerInfoData} wrapper cannot be cast to the NMS
     * {@code Entry} record in Paper 1.21.8; this method bypasses the converter entirely.
     * <p>
     * {@code com.mojang.authlib.GameProfile} is also created via reflection — authlib is not
     * exposed as a compile-time dependency through {@code paper-api}, but is always present
     * at runtime on a Paper server.
     * </p>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void sendPlayerInfoAdd(Player player, UUID fakeUuid) {
        try {
            Object profile = buildProfileNms(player, fakeUuid);

            // --- Entry record ---
            Class<?> entryClass = Class.forName(NMS_PLAYER_INFO_UPDATE + "$Entry");
            Class<?> gameTypeClass = Class.forName(NMS_GAME_TYPE);

            // ADVENTURE game mode: ordinal 2 (SURVIVAL=0, CREATIVE=1, ADVENTURE=2)
            // Enum.valueOf requires the exact obfuscated/mapped name; use ordinal as fallback
            Object gameMode = resolveGameTypeAdventure(gameTypeClass);

            // The Entry record's canonical constructor — discover it at runtime so this works
            // across minor patches that add/remove record components
            Constructor<?> entryCtor = findCanonicalCtor(entryClass);
            Object entry = invokeEntryCtor(entryCtor, fakeUuid, profile, gameMode);

            // --- Actions EnumSet ---
            Class<? extends Enum> actionClass =
                (Class<? extends Enum>) Class.forName(NMS_PLAYER_INFO_UPDATE + "$Action");
            Enum addPlayer    = Enum.valueOf(actionClass, "ADD_PLAYER");
            Enum updateListed = Enum.valueOf(actionClass, "UPDATE_LISTED");
            EnumSet actions   = EnumSet.of(addPlayer, updateListed);

            // --- Packet ---
            Class<?> packetClass = Class.forName(NMS_PLAYER_INFO_UPDATE);
            Constructor<?> packetCtor = packetClass.getDeclaredConstructor(EnumSet.class, List.class);
            packetCtor.setAccessible(true);
            Object nmsPacket = packetCtor.newInstance(actions, List.of(entry));

            ProtocolLibrary.getProtocolManager()
                .sendServerPacket(player, PacketContainer.fromPacket(nmsPacket));
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[FakePlayerManager] Failed to send PLAYER_INFO (ADD): " + e.getMessage());
        }
    }

    /**
     * Sends {@code ClientboundPlayerInfoRemovePacket} via NMS reflection.
     */
    private static void sendPlayerInfoRemove(Player player, UUID fakeUuid) {
        try {
            Class<?> removeClass = Class.forName(NMS_PLAYER_INFO_REMOVE);
            Constructor<?> ctor = removeClass.getDeclaredConstructor(List.class);
            ctor.setAccessible(true);
            Object nmsPacket = ctor.newInstance(List.of(fakeUuid));

            ProtocolLibrary.getProtocolManager()
                .sendServerPacket(player, PacketContainer.fromPacket(nmsPacket));
        } catch (Exception e) {
            Sword.getInstance().getLogger().warning(
                "[FakePlayerManager] Failed to send PLAYER_INFO_REMOVE: " + e.getMessage());
        }
    }

    /**
     * Resolves the {@code ADVENTURE} constant from the NMS {@code GameType} enum.
     * Tries by name first; falls back to ordinal 2 if the mapped name differs.
     */
    private static Object resolveGameTypeAdventure(Class<?> gameTypeClass) {
        for (Object constant : gameTypeClass.getEnumConstants()) {
            String name = ((Enum<?>) constant).name();
            if ("ADVENTURE".equalsIgnoreCase(name) || "adventure".equals(name)) {
                return constant;
            }
        }
        // Ordinal fallback: SURVIVAL=0, CREATIVE=1, ADVENTURE=2, SPECTATOR=3
        Object[] constants = gameTypeClass.getEnumConstants();
        return constants.length > 2 ? constants[2] : constants[0];
    }

    /**
     * Builds a {@code com.mojang.authlib.GameProfile} with the player's skin texture via
     * reflection. Authlib is always present at runtime on Paper but is not exposed as a
     * compile-time transitive dependency from {@code paper-api}.
     */
    private static Object buildProfileNms(Player player, UUID fakeUuid)
            throws ReflectiveOperationException {
        Class<?> profileClass = Class.forName("com.mojang.authlib.GameProfile");
        Constructor<?> profileCtor = profileClass.getDeclaredConstructor(UUID.class, String.class);
        Object profile = profileCtor.newInstance(fakeUuid, player.getName());

        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Constructor<?> propCtor = propertyClass.getDeclaredConstructor(
            String.class, String.class, String.class);

        Object propMap = profileClass.getMethod("getProperties").invoke(profile);
        Method putMethod = null;
        for (Method m : propMap.getClass().getMethods()) {
            if ("put".equals(m.getName()) && m.getParameterCount() == 2) {
                putMethod = m;
                break;
            }
        }
        if (putMethod == null) throw new RuntimeException("No put() on PropertyMap");

        for (ProfileProperty prop : player.getPlayerProfile().getProperties()) {
            if ("textures".equals(prop.getName())) {
                Object nmsProperty = propCtor.newInstance(
                    prop.getName(), prop.getValue(), prop.getSignature());
                putMethod.invoke(propMap, prop.getName(), nmsProperty);
            }
        }
        return profile;
    }

    /**
     * Finds the canonical constructor of the {@code Entry} record — the one with the most
     * parameters, which is always the record's canonical constructor in Java.
     */
    private static Constructor<?> findCanonicalCtor(Class<?> entryClass) {
        return Arrays.stream(entryClass.getDeclaredConstructors())
            .max((a, b) -> Integer.compare(a.getParameterCount(), b.getParameterCount()))
            .orElseThrow(() -> new RuntimeException("No constructors on Entry record"));
    }

    /**
     * Invokes the {@code Entry} record constructor with appropriate arguments.
     * <p>
     * Fills parameter positions as follows, matching the 1.21.x record layout:
     * {@code UUID, GameProfile, boolean(listed), int(latency), GameType,
     * Component(displayName), boolean(showHat), int(listOrder), RemoteChatSession.Data}.
     * Unknown types (everything past the five core fields) are filled with {@code null} or
     * {@code false}/{@code 0} according to their kind.
     * </p>
     */
    private static Object invokeEntryCtor(Constructor<?> ctor, UUID fakeUuid,
            Object profile, Object gameMode) throws ReflectiveOperationException {
        ctor.setAccessible(true);
        Class<?>[] params = ctor.getParameterTypes();
        Object[] args = new Object[params.length];

        // Positions are stable across 1.21.x patch releases:
        // 0 = UUID, 1 = GameProfile, 2 = boolean(listed), 3 = int(latency), 4 = GameType
        if (params.length > 0) args[0] = fakeUuid;
        if (params.length > 1) args[1] = profile;
        if (params.length > 2) args[2] = true;   // listed = true
        if (params.length > 3) args[3] = 0;       // latency = 0
        if (params.length > 4) args[4] = gameMode;
        // displayName(5), showHat(6), listOrder(7), chatSession(8) — all null/default
        for (int i = 5; i < params.length; i++) {
            if (params[i] == boolean.class) {
                args[i] = false;
            } else if (params[i] == int.class) {
                args[i] = 0;
            } else {
                args[i] = null;
            }
        }
        return ctor.newInstance(args);
    }

    // =========================================================================
    // ProtocolLib helpers (spawn, equipment, rotation, destroy)
    // =========================================================================

    private static void sendSpawnEntity(ProtocolManager manager, Player player,
            int entityId, UUID fakeUuid, Location location) {
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.SPAWN_ENTITY);

            packet.getIntegers().write(0, entityId);
            packet.getUUIDs().write(0, fakeUuid);
            packet.getEntityTypeModifier().write(0, EntityType.PLAYER);
            packet.getDoubles().write(0, location.getX());
            packet.getDoubles().write(1, location.getY());
            packet.getDoubles().write(2, location.getZ());
            // pitch (xRot), body yaw (yRot), head yaw — encoded as 0–255 over 0–360 degrees
            packet.getBytes().write(0, angleToByte(location.getPitch()));
            packet.getBytes().write(1, angleToByte(location.getYaw()));
            packet.getBytes().write(2, angleToByte(location.getYaw()));
            packet.getIntegers().write(1, 0); // data = 0

            manager.sendServerPacket(player, packet);
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[FakePlayerManager] Failed to send SPAWN_ENTITY: " + e.getMessage());
        }
    }

    private static void sendHeadRotation(ProtocolManager manager, Player player,
            int entityId, float yaw) {
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_HEAD_ROTATION);
            packet.getIntegers().write(0, entityId);
            packet.getBytes().write(0, angleToByte(yaw));
            manager.sendServerPacket(player, packet);
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[FakePlayerManager] Failed to send ENTITY_HEAD_ROTATION: " + e.getMessage());
        }
    }

    private static void sendEquipment(ProtocolManager manager, Player player,
            int entityId, EntityEquipment equipment) {
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            packet.getIntegers().write(0, entityId);

            List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = new ArrayList<>();
            slots.add(new Pair<>(EnumWrappers.ItemSlot.HEAD,     orAir(equipment.getHelmet())));
            slots.add(new Pair<>(EnumWrappers.ItemSlot.CHEST,    orAir(equipment.getChestplate())));
            slots.add(new Pair<>(EnumWrappers.ItemSlot.LEGS,     orAir(equipment.getLeggings())));
            slots.add(new Pair<>(EnumWrappers.ItemSlot.FEET,     orAir(equipment.getBoots())));
            slots.add(new Pair<>(EnumWrappers.ItemSlot.MAINHAND, orAir(equipment.getItemInMainHand())));
            slots.add(new Pair<>(EnumWrappers.ItemSlot.OFFHAND,  orAir(equipment.getItemInOffHand())));

            packet.getSlotStackPairLists().write(0, slots);
            manager.sendServerPacket(player, packet);
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[FakePlayerManager] Failed to send ENTITY_EQUIPMENT: " + e.getMessage());
        }
    }

    /**
     * Sends a {@code ClientboundSetEntityDataPacket} enabling all skin overlay layers (hat,
     * jacket, sleeves, pants) via NMS reflection.
     * <p>
     * ProtocolLib's {@code WrappedDataWatcher} produces {@code SynchedEntityData$DataItem}
     * objects, but the NMS encoder requires {@code SynchedEntityData$DataValue} records —
     * same class-cast failure pattern as the player-info packet. We bypass ProtocolLib's
     * wrapper entirely and build the NMS packet directly.
     * </p>
     * <p>
     * Metadata index 17 = Displayed Skin Parts (byte bitmask):<br>
     * {@code 0x01} cape, {@code 0x02} jacket, {@code 0x04} left sleeve, {@code 0x08} right sleeve,
     * {@code 0x10} left pants, {@code 0x20} right pants, {@code 0x40} hat → {@code 0x7F} = all.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static void sendSkinParts(Player player, int entityId) {
        try {
            // Resolve the BYTE serializer from EntityDataSerializers
            Class<?> serializersClass =
                Class.forName("net.minecraft.network.syncher.EntityDataSerializers");
            Class<?> serializerClass =
                Class.forName("net.minecraft.network.syncher.EntityDataSerializer");
            Field byteField = serializersClass.getDeclaredField("BYTE");
            byteField.setAccessible(true);
            Object byteSerializer = byteField.get(null);

            // Build SynchedEntityData$DataValue(17, BYTE, 0x7F)
            Class<?> dataValueClass =
                Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue");
            Constructor<?> dataValueCtor =
                dataValueClass.getDeclaredConstructor(int.class, serializerClass, Object.class);
            dataValueCtor.setAccessible(true);
            Object dataValue = dataValueCtor.newInstance(17, byteSerializer, (byte) 0x7F);

            // Build ClientboundSetEntityDataPacket(entityId, List.of(dataValue))
            Class<?> packetClass = Class.forName(
                "net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            Constructor<?> packetCtor =
                packetClass.getDeclaredConstructor(int.class, List.class);
            packetCtor.setAccessible(true);
            Object nmsPacket = packetCtor.newInstance(entityId, List.of(dataValue));

            ProtocolLibrary.getProtocolManager()
                .sendServerPacket(player, PacketContainer.fromPacket(nmsPacket));
        } catch (Exception e) {
            Sword.getInstance().getLogger().warning(
                "[FakePlayerManager] Failed to send ENTITY_METADATA (skin parts): " + e.getMessage());
        }
    }

    private static void sendEntityDestroy(ProtocolManager manager, Player player, int entityId) {
        try {
            PacketContainer packet = manager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            packet.getIntLists().write(0, Collections.singletonList(entityId));
            manager.sendServerPacket(player, packet);
        } catch (Exception e) {
            Sword.getInstance().getLogger().severe(
                "[FakePlayerManager] Failed to send ENTITY_DESTROY: " + e.getMessage());
        }
    }

    /** Encodes a degree angle (−180 to 180, or 0 to 360) as a protocol byte (0–255). */
    private static byte angleToByte(float angle) {
        return (byte) Math.round(angle / 360.0f * 256.0f);
    }

    private static ItemStack orAir(ItemStack item) {
        return item != null ? item : ItemStack.of(org.bukkit.Material.AIR);
    }
}
