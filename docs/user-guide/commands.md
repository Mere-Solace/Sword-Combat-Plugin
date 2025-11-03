# Command Reference

This guide covers all available commands in Sword: Combat Evolved.

## Command Overview

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/sword` | `/sce`, `/swordce` | Shows plugin information | `sword.use` (default: true) |
| `/sword reload` | - | Hot-reloads configuration | `sword.reload` (default: op) |

## Command Details

### `/sword`

**Usage:** `/sword`
**Aliases:** `/sce`, `/swordce`
**Permission:** `sword.use` (granted to all players by default)

Displays basic plugin information and available commands.

**Example:**
```
/sword
> Sword: Combat Evolved
> Usage: /sword reload
```

---

### `/sword reload`

**Usage:** `/sword reload`
**Permission:** `sword.reload` (granted to operators only)

Hot-reloads the plugin configuration from `config.yaml` without requiring a server restart. This is useful for:
- Testing configuration changes during development
- Tuning combat values on a live server
- Adjusting physics parameters without downtime

**Success Output:**
```
/sword reload
> Reloading Sword: Combat Evolved configuration...
> ✓ Configuration reloaded successfully!
>   All values have been updated from config.yaml
```

**Failure Output:**
```
/sword reload
> Reloading Sword: Combat Evolved configuration...
> ✗ Configuration reload failed!
>   Check console for error details. Using previous values.
```

**Permission Denied:**
```
/sword reload
> You don't have permission to reload the config.
```

**What Gets Reloaded:**

The reload command updates all values from [`config.yaml`](../../src/main/resources/config.yaml), including:

- **Physics**: Gravity, trajectory, rotation speeds, velocity dampening
- **Combat**: Damage values, range multipliers, attack timings
- **Display**: Particle effects, animation settings, visual parameters
- **Detection**: Ground check distances, hitbox sizes
- **Timing**: Cooldowns, grace periods, update intervals
- **Audio**: Sound volumes and pitches
- **Entity Behavior**: Movement modifiers, slowness effects
- **World Interaction**: Explosion settings, marker placement

**What Does NOT Get Reloaded:**

- Active abilities or attacks in progress
- Player data or statistics
- Entity references (thrown items, displays)
- Event listener registrations

**Best Practices:**

1. **Test in Development:** Always test config changes on a development server first
2. **Backup Configs:** Keep a backup of `config.yaml` before making major changes
3. **Check Console:** Monitor server console for validation errors after reload
4. **Document Changes:** Note what values you changed and why
5. **Reload After Edits:** Always run `/sword reload` after editing `config.yaml`

**Technical Notes:**

- Commands are registered using Paper's Brigadier API with lifecycle events
- The reload system validates configuration structure before applying changes
- If validation fails, previous values remain in effect
- Configuration is thread-safe and can be reloaded during gameplay
- See [Configuration Guide](configuration.md) for detailed config documentation

## Permissions

### `sword.use`
- **Default:** `true` (all players)
- **Description:** Allows use of the base `/sword` command
- **Commands:** `/sword`, `/sce`, `/swordce`

### `sword.reload`
- **Default:** `op` (operators only)
- **Description:** Allows reloading plugin configuration
- **Commands:** `/sword reload`

## Command Registration

Sword: Combat Evolved uses **Paper's Brigadier command system** with lifecycle events. Commands are:
- Registered during plugin initialization via `LifecycleEvents.COMMANDS`
- Automatically available after server startup
- Do not require entries in `paper-plugin.yml`
- Support tab completion (future feature)
- Handle permissions at the command node level

For technical details, see:
- [`SwordCommands.java`](../../src/main/java/btm/sword/commands/SwordCommands.java) - Command implementation
- [`Sword.java`](../../src/main/java/btm/sword/Sword.java#L36-39) - Command registration
- [Issue #74](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/74) - Brigadier migration details

## Troubleshooting

### Commands don't work after server start

**Symptom:** `/sword` command not found

**Possible Causes:**
1. Plugin failed to load (check console for errors)
2. Plugin using wrong command registration system
3. Permission issues

**Solution:**
```bash
# Check if plugin is loaded
/plugins

# Check console for errors during startup
# Look for: "Sword: Combat Evolved has been enabled"

# Verify plugin version matches paper-plugin.yml
# Should show: sword-1.0-SNAPSHOT
```

### Reload command not working

**Symptom:** `/sword reload` shows permission denied

**Possible Causes:**
1. Not an operator
2. Custom permission plugin overriding defaults

**Solution:**
```bash
# Grant op status
/op <player>

# Or grant specific permission
/lp user <player> permission set sword.reload true
```

### Configuration changes not applying

**Symptom:** Changed values in config.yaml but seeing old behavior

**Possible Causes:**
1. Forgot to run `/sword reload`
2. Configuration syntax errors (YAML indentation)
3. File not saved properly

**Solution:**
```bash
# Save config.yaml
# Then reload
/sword reload

# Check console for validation errors
# If errors occur, config keeps previous values
```

## See Also

- [Configuration Guide](configuration.md) - Detailed config.yaml documentation
- [Contributing Guide](../../CONTRIBUTING.md) - How to contribute to the project
- [Paper Command API Documentation](https://docs.papermc.io/paper/dev/command-api/) - Brigadier command system
