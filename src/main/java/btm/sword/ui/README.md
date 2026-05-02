# Display System

Clean internal APIs for scoreboard and boss bar manipulation, built on Paper's Adventure API.
Both systems are lifecycle-managed — players are cleaned up automatically on disconnect, and
all resources are released on plugin disable.

---

## Scoreboard API

**Classes:** `SwordScoreboard`, `ScoreboardManager`

Per-player sidebar scoreboards with top-down line management (line 1 = top).
Each player gets their own isolated `Scoreboard` object, so there is no cross-player
interference.

### Quick start

```java
// Create and show a scoreboard
SwordScoreboard board = ScoreboardManager.create(player, Component.text("Combat HUD"));
board.setLine(1, Component.text("HP").color(NamedTextColor.RED).append(Component.text(": 100")));
board.setLine(2, Component.empty());  // blank spacer
board.setLine(3, Component.text("Soulfire: 50").color(NamedTextColor.AQUA));
board.show();

// Update a line
board.setLine(1, Component.text("HP: 75").color(NamedTextColor.RED));

// Hide/show toggle
board.hide();
board.show();

// Destroy when done
board.remove();
```

### API surface

| Method | Description |
|---|---|
| `ScoreboardManager.create(player, title)` | Create + register a scoreboard |
| `ScoreboardManager.get(player)` | Retrieve an existing scoreboard (Optional) |
| `ScoreboardManager.remove(player)` | Destroy the player's scoreboard |
| `board.setTitle(Component)` | Update the sidebar title |
| `board.setLine(int, Component)` | Set line 1–15 (1 = top) |
| `board.clearLine(int)` | Remove a single line |
| `board.clear()` | Remove all lines |
| `board.show()` / `board.hide()` | Toggle visibility |
| `board.remove()` | Destroy permanently |

### Limits

- **15 lines maximum** — Minecraft sidebar constraint.
- Lines use `Score.customName(Component)` (Paper 1.20.4+) for rich text without team hacks.

---

## Boss Bar API

**Classes:** `SwordBossBar`, `BossBarManager`

Multi-viewer boss bars with progress tracking, timed auto-removal, and full
Adventure API formatting support.

### Quick start

```java
// Persistent boss bar shown to all players in an encounter
SwordBossBar bar = BossBarManager.create(
    Component.text("Ancient Guardian").color(NamedTextColor.RED),
    1.0f,
    BossBar.Color.RED,
    BossBar.Overlay.NOTCHED_10
);
bar.addViewer(player1);
bar.addViewer(player2);

// Drain health over time
bar.setProgress(0.5f);
bar.setTitle(Component.text("Ancient Guardian [Enraged]").color(NamedTextColor.DARK_RED));

// Clean up when encounter ends
bar.remove();
```

```java
// Timed countdown bar (auto-removes after 10 seconds)
SwordBossBar countdown = BossBarManager.createTimed(
    Component.text("Round starts in 10s"),
    1.0f, BossBar.Color.YELLOW, 10, TimeUnit.SECONDS
);
countdown.addViewer(player);

// Timed bar with a completion callback
BossBarManager.create(Component.text("Capturing..."), 0f, BossBar.Color.GREEN)
    .removeAfter(5, TimeUnit.SECONDS, bar -> bar.setTitle(Component.text("Captured!")));
```

### API surface

| Method | Description |
|---|---|
| `BossBarManager.create(title, progress, color)` | Solid-progress bar |
| `BossBarManager.create(title, progress, color, overlay)` | Full control |
| `BossBarManager.createTimed(title, progress, color, duration, unit)` | Auto-removing bar |
| `bar.setTitle(Component)` | Update the title |
| `bar.setProgress(float)` | Set fill 0.0–1.0 (clamped) |
| `bar.setColor(BossBar.Color)` | Change bar color |
| `bar.setOverlay(BossBar.Overlay)` | Change segment style |
| `bar.addViewer(Player)` | Show to a player |
| `bar.removeViewer(Player)` | Hide from a player |
| `bar.clearViewers()` | Hide from all viewers |
| `bar.removeAfter(duration, unit)` | Schedule auto-removal |
| `bar.removeAfter(duration, unit, onComplete)` | Auto-removal with callback |
| `bar.remove()` | Destroy immediately |

### Colors and overlays

**Colors:** `PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`

**Overlays:**

| Overlay | Description |
|---|---|
| `PROGRESS` | Solid bar (default) |
| `NOTCHED_6` | 6 segments |
| `NOTCHED_10` | 10 segments |
| `NOTCHED_12` | 12 segments |
| `NOTCHED_20` | 20 segments |

### Limits and notes

- Minecraft clients can display up to ~20 boss bars simultaneously; 1–3 is the practical limit.
- A bar is shown to a player only while they remain online. Disconnecting auto-removes them.
- `remove()` is idempotent — calling it more than once is safe.

---

## Lifecycle hooks

Both systems hook into the server lifecycle automatically:

| Event | Action |
|---|---|
| Player joins | (no action; bars/boards are created on demand) |
| Player quits | `ScoreboardManager.onPlayerQuit` + `BossBarManager.onPlayerQuit` |
| Plugin disable | `ScoreboardManager.removeAll()` + `BossBarManager.removeAll()` |
