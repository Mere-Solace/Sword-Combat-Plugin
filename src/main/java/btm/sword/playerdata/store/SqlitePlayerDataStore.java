package btm.sword.playerdata.store;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import btm.sword.action.skill.container.PlayerSkillContainer;
import btm.sword.action.skill.container.SkillSlot;
import btm.sword.action.skill.container.SkillSlotState;
import btm.sword.entity.aspect.AspectType;
import btm.sword.entity.aspect.value.AspectValue;
import btm.sword.entity.base.CombatProfile;
import btm.sword.playerdata.PlayerData;
import btm.sword.playerdata.PlayerStorage;
import btm.sword.playerdata.store.repository.AspectRepository;
import btm.sword.playerdata.store.repository.ProfileRepository;
import btm.sword.playerdata.store.repository.SkillRepository;
import btm.sword.playerdata.store.repository.SkillStateRepository;
import btm.sword.playerdata.store.repository.StorageRepository;

/**
 * SQLite-backed implementation of {@link PlayerDataStore}.
 * <p>
 * Uses a single JDBC connection with WAL journal mode for safe async writes.
 * All operations are synchronous on the calling thread — callers are responsible
 * for invoking save methods off the main server thread.
 * </p>
 *
 * <p>Schema is created (and migrated) automatically on {@link #open()}.</p>
 */
public class SqlitePlayerDataStore implements PlayerDataStore {

    private static final int CURRENT_SCHEMA_VERSION = 2;

    private final File dbFile;
    private final Logger logger;

    private Connection conn;
    private ProfileRepository profileRepo;
    private AspectRepository aspectRepo;
    private SkillRepository skillRepo;
    private SkillStateRepository skillStateRepo;
    private StorageRepository storageRepo;

    /**
     * @param dbFile the SQLite database file (created if absent)
     * @param logger the plugin logger for error reporting
     */
    public SqlitePlayerDataStore(File dbFile, Logger logger) {
        this.dbFile = dbFile;
        this.logger = logger;
    }

    /**
     * Opens the database connection, enables WAL mode, creates tables, and runs migrations.
     * Must be called before any read/write operations.
     *
     * @throws SQLException if the connection or schema setup fails
     */
    public void open() throws SQLException {
        dbFile.getParentFile().mkdirs();
        conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        // Enable WAL mode for safe concurrent reads and atomic writes
        try (PreparedStatement stmt = conn.prepareStatement("PRAGMA journal_mode=WAL")) {
            stmt.execute();
        }
        // Disable auto-commit — we manage transactions explicitly per save
        conn.setAutoCommit(true);

        profileRepo = new ProfileRepository(conn);
        aspectRepo = new AspectRepository(conn);
        skillRepo = new SkillRepository(conn);
        skillStateRepo = new SkillStateRepository(conn);
        storageRepo = new StorageRepository(conn);

        profileRepo.createTable();
        aspectRepo.createTable();
        skillRepo.createTable();
        skillStateRepo.createTable();
        storageRepo.createTable();
        createSchemaVersionTable();

        runMigrations();
    }

    @Override
    public PlayerData load(UUID uuid) {
        try {
            ProfileRepository.ProfileRow profile = profileRepo.load(uuid);
            if (profile == null) return null; // first-time player

            Map<AspectType, AspectValue> aspects = aspectRepo.load(uuid);
            SkillRepository.SkillData skillData = skillRepo.load(uuid);
            PlayerStorage storage = storageRepo.load(uuid);

            CombatProfile combatProfile = new CombatProfile();
            // Override config defaults with persisted values (unknown types keep config defaults)
            aspects.forEach(combatProfile::setStat);
            combatProfile.setSwordClass(profile.swordClass());
            combatProfile.setMaxAirDodges(profile.maxAirDodges());

            PlayerSkillContainer skillContainer;
            if (skillData.equipped().isEmpty() && skillData.available().isEmpty()) {
                // No skill data saved yet — use default container
                skillContainer = new PlayerSkillContainer();
            } else {
                skillContainer = new PlayerSkillContainer(skillData.available(), skillData.equipped());
            }
            // available map now carries SkillAvailability states — constructor above handles it
            Map<SkillSlot, SkillSlotState> slotStates = skillStateRepo.load(uuid);
            slotStates.forEach(skillContainer::setSlotState);
            combatProfile.setPlayerSkillContainer(skillContainer);

            return new PlayerData(uuid, profile.firstLogin(), profile.joinSequenceCompleted(),
                combatProfile, storage);

        } catch (SQLException e) {
            logger.severe("[PlayerData] Failed to load data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    @Override
    public void save(UUID uuid, PlayerData data) {
        try {
            conn.setAutoCommit(false);
            try {
                CombatProfile profile = data.getCombatProfile();
                profileRepo.save(uuid, profile.getSwordClass(), data.isJoinSequenceCompleted(),
                    data.getDateOfFirstLogin().getTime(), profile.getMaxAirDodges());
                aspectRepo.save(uuid, profile.getStats());
                PlayerSkillContainer skills = profile.getPlayerSkillContainer();
                skillRepo.save(uuid, skills.equippedView(), skills.allDiscoveredSkillAvailabilities());
                skillStateRepo.save(uuid, skills.allSlotStates());
                storageRepo.save(uuid, data.getPlayerStorage());
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.severe("[PlayerData] Failed to save data for " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void saveAll(Collection<UUID> uuids, Map<UUID, PlayerData> cache) {
        for (UUID uuid : uuids) {
            PlayerData data = cache.get(uuid);
            if (data != null) save(uuid, data);
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.severe("[PlayerData] Failed to close database connection: " + e.getMessage());
        }
    }

    // =========================================================================
    // Schema versioning
    // =========================================================================

    private void createSchemaVersionTable() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS schema_version (version INTEGER NOT NULL)")) {
            stmt.execute();
        }
    }

    private int getSchemaVersion() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT version FROM schema_version LIMIT 1");
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt("version") : 0;
        }
    }

    private void setSchemaVersion(int version) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM schema_version");
             PreparedStatement ins = conn.prepareStatement(
                "INSERT INTO schema_version (version) VALUES (?)")) {
            del.execute();
            ins.setInt(1, version);
            ins.execute();
        }
    }

    private void runMigrations() throws SQLException {
        int version = getSchemaVersion();
        if (version < CURRENT_SCHEMA_VERSION) {
            logger.info("[PlayerData] Schema at v" + version + ", migrating to v" + CURRENT_SCHEMA_VERSION);
            // V0 → V1: initial schema (tables already created above — nothing more to migrate)
            if (version < 2) {
                // V1 → V2: add availability column to player_available_skills
                skillRepo.migrateV1ToV2();
            }
            setSchemaVersion(CURRENT_SCHEMA_VERSION);
            logger.info("[PlayerData] Schema migration complete.");
        }
    }
}
