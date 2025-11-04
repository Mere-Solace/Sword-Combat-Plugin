# Configuration File Lifecycle

This document explains the relationship between configuration files in the development environment, build artifacts, and server runtime.

## Configuration File Locations

### 1. Source Config (Development)
**Location**: `src/main/resources/config.yaml`

**Purpose**: The **master template** for default configuration values

**Usage**:
- Edit this file when adding new config values or changing defaults
- This is the source of truth for configuration structure
- Gets packaged into the plugin JAR during build

**When to Edit**:
- Adding new configuration values
- Changing default values
- Updating configuration structure
- Documenting config parameters

### 2. Packaged Config (Build Artifact)
**Location**: Inside `build/libs/Sword-1.0-SNAPSHOT.jar` (at `/config.yaml`)

**Purpose**: Embedded default config that ships with the plugin

**How it Gets There**:
```gradle
// build.gradle - Gradle automatically packages resources into the JAR
processResources {
    def props = [version: version]
    inputs.properties props
    filteringCharset 'UTF-8'
    filesMatching('paper-plugin.yml') {
        expand props
    }
}
```

The `processResources` task automatically copies everything from `src/main/resources/` into the JAR during `./gradlew build`.

**Verification**:
```bash
# View contents of the built JAR
jar tf build/libs/Sword-1.0-SNAPSHOT.jar | grep config.yaml
```

### 3. Server Runtime Config (Live Server)
**Location**: `run/plugins/Sword/config.yaml` (development server)
**Production Location**: `<server>/plugins/Sword/config.yaml`

**Purpose**: The **active configuration** that the plugin reads during runtime

**How it Gets Created**:
When the plugin starts for the first time, [ConfigManager.java:100-118](../../src/main/java/btm/sword/config/ConfigManager.java#L100-L118) checks if the file exists:

```java
private void setupConfig() {
    configFile = new File(plugin.getDataFolder(), "config.yaml");
    //                     ^-- This resolves to: run/plugins/Sword/config.yaml

    if (!configFile.exists()) {
        plugin.getDataFolder().mkdirs();
        try (InputStream defaultConfig = plugin.getResource("config.yaml")) {
            //                              ^-- Reads from JAR's embedded config.yaml
            if (defaultConfig != null) {
                Files.copy(defaultConfig, configFile.toPath());
                //         ^-- Copies from JAR -> server folder
                plugin.getLogger().info("Created default config.yaml");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create config.yaml", e);
        }
    }
}
```

**Key Behavior**:
- **Only created if it doesn't exist** - preserves user edits across plugin updates
- If you delete `run/plugins/Sword/config.yaml`, it will be recreated from the JAR on next server start

## Configuration Lifecycle Flowchart

```
┌─────────────────────────────────────────────────────────────────┐
│ 1. DEVELOPMENT                                                  │
├─────────────────────────────────────────────────────────────────┤
│ Developer edits:                                                │
│   src/main/resources/config.yaml                                │
│                                                                 │
│ - Add new config values                                        │
│ - Update defaults                                              │
│ - Document parameters                                          │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
         ┌───────────────────────────┐
         │  ./gradlew build          │
         │  (processResources task)  │
         └───────────┬───────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 2. BUILD ARTIFACT                                               │
├─────────────────────────────────────────────────────────────────┤
│ Created:                                                        │
│   build/libs/Sword-1.0-SNAPSHOT.jar                            │
│                                                                 │
│ Contains embedded resource:                                     │
│   /config.yaml (from src/main/resources/)                      │
│                                                                 │
│ - Packaged into JAR automatically                              │
│ - Ready for distribution                                       │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
         ┌───────────────────────────┐
         │ Copy JAR to server or     │
         │ ./gradlew runServer       │
         └───────────┬───────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────────┐
│ 3. SERVER RUNTIME (First Start)                                │
├─────────────────────────────────────────────────────────────────┤
│ Plugin checks:                                                  │
│   run/plugins/Sword/config.yaml exists?                        │
│                                                                 │
│ ┌─────────────────┐              ┌──────────────────┐         │
│ │ NO              │              │ YES              │         │
│ └────────┬────────┘              └────────┬─────────┘         │
│          │                                │                    │
│          ▼                                ▼                    │
│  Extract from JAR                Use existing file           │
│  Create new file                 (preserve user edits)       │
└─────────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│ 4. RUNTIME UPDATES                                              │
├─────────────────────────────────────────────────────────────────┤
│ Server admin edits:                                             │
│   run/plugins/Sword/config.yaml                                │
│                                                                 │
│ Then runs:                                                      │
│   /sword reload                                                 │
│                                                                 │
│ ConfigManager.reload() re-reads the file without restart       │
└─────────────────────────────────────────────────────────────────┘
```

## Common Workflows

### Workflow 1: Adding a New Config Value

1. **Edit source config**:
   ```bash
   # Edit: src/main/resources/config.yaml
   movement:
     dash:
       new_parameter: 42.0  # Add new value
   ```

2. **Update config class**:
   ```java
   // In MovementConfig.java
   @Getter
   public class MovementConfig {
       private final double newParameter;

       public MovementConfig(FileConfiguration config) {
           ConfigurationSection dash = movement.getConfigurationSection("dash");
           this.newParameter = dash.getDouble("new_parameter", 42.0);
       }
   }
   ```

3. **Build and test**:
   ```bash
   ./gradlew build
   ./gradlew runServer
   ```

4. **Verification**:
   - On first server start, `run/plugins/Sword/config.yaml` is created with your new value
   - Edit `run/plugins/Sword/config.yaml` to test different values
   - Use `/sword reload` to hot-reload changes

### Workflow 2: Testing Config Changes During Development

**Scenario**: You're tuning dash mechanics and want to test different values quickly.

**DO NOT** edit `src/main/resources/config.yaml` for testing - that's for defaults only!

**DO** edit the runtime config:

1. Start the server:
   ```bash
   ./gradlew runServer
   ```

2. Edit the **runtime config**:
   ```bash
   # Edit: run/plugins/Sword/config.yaml
   movement:
     dash:
       max_distance: 15.0  # Testing increased distance
   ```

3. Hot-reload in-game:
   ```
   /sword reload
   ```

4. Test the changes in-game

5. **If you like the changes**, update the default:
   ```bash
   # Edit: src/main/resources/config.yaml
   movement:
     dash:
       max_distance: 15.0  # Make this the new default
   ```

6. Rebuild and commit:
   ```bash
   ./gradlew build
   git add src/main/resources/config.yaml
   git commit -m "Update dash max distance default to 15.0"
   ```

### Workflow 3: Plugin Updates and Config Preservation

**Scenario**: You release a new version with additional config values.

**What Happens**:

```
Old config.yaml (v1.0)          New default config (v1.1)
─────────────────────────       ───────────────────────────
movement:                       movement:
  dash:                           dash:
    max_distance: 10.0              max_distance: 10.0
                                    new_parameter: 42.0  ← NEW!
```

**Server Behavior**:

1. User updates plugin JAR (places new JAR in `plugins/` folder)
2. Server restarts
3. `run/plugins/Sword/config.yaml` **still exists** from old version
4. ConfigManager sees file exists, **does not overwrite**
5. New parameter uses code default: `dash.getDouble("new_parameter", 42.0)`
   - The `42.0` fallback value is used since it's not in the old config

**User Must Manually Add New Values**:

Server admins need to either:
- Manually add new parameters from the default config
- Delete `config.yaml` and let it regenerate (loses custom values)
- Copy-paste new sections while preserving their edits

**Best Practice**: Document new config values in release notes.

## Hot Reload System

The `/sword reload` command uses [ConfigManager.reload()](../../src/main/java/btm/sword/config/ConfigManager.java#L160-L167):

```java
public boolean reload() {
    plugin.getLogger().info("Reloading configuration...");
    boolean success = loadConfig();
    if (success) {
        plugin.getLogger().info("Configuration reloaded successfully!");
    }
    return success;
}
```

**What Gets Reloaded**:
- All `ConfigSection` objects are recreated with new values from disk
- Changes take effect immediately (no server restart required)
- Active gameplay (entities, ongoing actions) uses new values on next execution

**What Does NOT Get Reloaded**:
- Already-spawned entities keep their old stat values (until respawned)
- Scheduled tasks continue with old parameters (until next execution)

## Build System Integration

### Gradle Tasks

```bash
# Build plugin JAR (includes config.yaml from resources)
./gradlew build

# Run development server (auto-copies JAR to run/plugins/)
./gradlew runServer

# Clean build artifacts (removes build/ directory)
./gradlew clean
```

### JAR Contents

```
Sword-1.0-SNAPSHOT.jar
├── btm/sword/           # Compiled .class files
├── config.yaml          # ← FROM src/main/resources/config.yaml
├── paper-plugin.yml     # Plugin metadata
└── META-INF/            # JAR metadata
```

### processResources Task

The `processResources` Gradle task automatically:
1. Copies all files from `src/main/resources/` to `build/resources/main/`
2. Expands template variables (e.g., `${version}` in `paper-plugin.yml`)
3. Packages the processed resources into the final JAR

**No manual configuration needed** - resources are included by default in Java projects.

## Troubleshooting

### Problem: Config changes not taking effect

**Solution**:
1. Check you edited the **runtime config** (`run/plugins/Sword/config.yaml`), not the source
2. Run `/sword reload` in-game or restart the server
3. Check server console for config loading errors

### Problem: New config values not appearing in server config

**Cause**: ConfigManager only creates the file if it doesn't exist (preserves user edits).

**Solution**:
1. Manually add new sections from `src/main/resources/config.yaml` to `run/plugins/Sword/config.yaml`
2. OR delete `run/plugins/Sword/config.yaml` and restart (regenerates from JAR, loses custom values)

### Problem: Build succeeds but config.yaml not in JAR

**Verification**:
```bash
jar tf build/libs/Sword-1.0-SNAPSHOT.jar | grep config.yaml
```

**Should output**:
```
config.yaml
```

**If missing**:
1. Verify file exists at `src/main/resources/config.yaml`
2. Run `./gradlew clean build` to force rebuild
3. Check build logs for resource processing errors

### Problem: Server creates empty config.yaml

**Cause**: `plugin.getResource("config.yaml")` returns null (file not found in JAR).

**Solution**:
1. Verify config.yaml is in JAR: `jar tf build/libs/Sword-1.0-SNAPSHOT.jar`
2. Check file is at correct path: `src/main/resources/config.yaml` (not in a subdirectory)
3. Rebuild: `./gradlew clean build`

## References

- [ConfigManager.java](../../src/main/java/btm/sword/config/ConfigManager.java) - Configuration loading system
- [build.gradle](../../build.gradle) - Gradle build configuration
- [Hybrid Configuration Pattern ADR](../decisions/003-hybrid-configuration-pattern.md) - Development methodology
- [Configuration Guide](../user-guide/configuration.md) - User documentation for server admins
