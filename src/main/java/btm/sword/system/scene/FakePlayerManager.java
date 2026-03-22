package btm.sword.system.scene;

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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import btm.sword.system.control.SwordScheduler;
import btm.sword.system.entity.impl.SwordPlayer;

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
        return item != null ? item : new ItemStack(org.bukkit.Material.AIR);
    }
}
