# `btm.sword.join` — Join-Sequence Lifecycle

This package owns the player join experience from the moment a player connects to the
moment they enter active gameplay. The system is built around an explicit per-player
lifecycle, single-owner state, and structurally-enforced invariants — every transition
goes through one named method and the input system cannot dispatch a single ability
while a player is locked in the waiting phase.

---

## Lifecycle

```
                    PlayerSpawnLocationEvent
                              │
                              ▼
                    JoinSessionArbiter.onSpawnLocation
                    sets event.setSpawnLocation(slot)
                              │
                              │ (50 ms — session construction deferral)
                              ▼
                       new JoinSession
                              │
                              ▼
                    ┌──────────────────┐
                    │     STAGING      │  gate.engage runs here:
                    │ (lockdown active)│  inventory stashed + replaced
                    └────────┬─────────┘  with placeholder panes,
                              │            ActivationContext.WAITING set,
                              │ (LOADING_WAIT_MS)
                              ▼
        ┌───────────► ┌──────────────────┐
        │             │     WAITING      │  router menu open,
        │             │                  │  waitingTicker zeroes velocity
        │             │                  │  + re-opens menu if closed
        │             └────────┬─────────┘
        │                      │ click destination
        │                      ▼
        │             ┌──────────────────┐
        │             │     ROUTING      │  countdown title task running,
        │             │                  │  waitingTicker still zeros velocity,
        └── sneak ────┤                  │  menu re-display disabled
                      └────────┬─────────┘
                               │ countdown completes
                               ▼
                      ┌──────────────────┐
                      │      ACTIVE      │  gate.disengage runs here:
                      │   (terminal)     │  inventory cleared,
                      └──────────────────┘  ActivationContext.NORMAL,
                                             survival + visible.
                                             Session evicted from arbiter map.
```

Termination is orthogonal to the four phases. Any phase can be terminated by a
player-quit event; `JoinSession.terminate()` cancels every running task, releases the
slot, evicts the session, and (if not yet `ACTIVE`) restores the stashed inventory back
onto the player.

---

## Components

| Component | Type | Owns |
|---|---|---|
| `JoinSessionArbiter` | Bukkit `Listener` | Map `UUID → JoinSession`. Spawn / quit / sneak event routing. The single eviction callback for sessions. |
| `JoinSession` | One per online player going through join | Phase, claimed slot, all running scheduler task handles, the chosen destination, the open InvUI window (indirectly via `WindowManager`). |
| `JoinPhase` | Enum | The closed lifecycle: `STAGING`, `WAITING`, `ROUTING`, `ACTIVE`. |
| `PlayerWaitingGate` | Stateless utility | The atomic enter / exit operations — `engage`, `disengage`, `abort`. |
| `Destination` | Enum | The closed set of destinations a player can pick — `HUB`, `QUICK_JOIN`, `ADVENTURE`, `ROGUELIKE`. Each resolves to its configured `Location`. |
| `WaitingPhaseGuard` | Stateless utility | One-line `isLocked(SwordPlayer)` check used by Bukkit listeners that side-step the `InputRouter`. |
| `MenuSlotGrid` | Static map | The grid of off-screen dark-room slots. Slot is acquired in `JoinSessionArbiter.onSpawnLocation`, released in `JoinSession.terminate` / `enterActive`. |
| `JoinRouterMenu` | InvUI menu | Pure intent surface — clicking a button raises a `Destination` through the `Consumer<Destination>` callback. The menu does no teleport or lifecycle work. |
| `stash.InventoryStashRepository` | Interface | Storage abstraction for inventory snapshots taken on `engage`. |
| `stash.InMemoryInventoryStashRepository` | Impl | `ConcurrentHashMap`-backed; lost on plugin restart. The only implementation today. |

### Dependency graph

```
JoinSessionArbiter ──► JoinSession ──► PlayerWaitingGate ──► InventoryStashRepository
                              │
                              ├─► MenuSlotGrid
                              ├─► JoinRouterMenu  (callback only, never reverse)
                              ├─► TimeArbiter
                              └─► JoinSequenceConfig
```

No edges flow back. The session never knows about the arbiter except through the
`EvictionCallback` functional interface. The menu never knows about the session except
through the `Consumer<Destination>` it raises intent on.

---

## Invariants

| # | Invariant | Enforcement |
|---|---|---|
| I1 | Exactly one `JoinSession` per online UUID. | `JoinSessionArbiter.sessions` is the single map; new sessions terminate any stale entry before insertion. |
| I2 | A player in `WAITING` triggers no input dispatch. | Hard early-return at `InputRouter.route` (first line). Hard early-return at `SwordPlayer.act` (first line). Mirrored guards in `PlayerListener` for `onItemPickup`, `inventoryDragEvent`, `inventoryInteractEvent`. |
| I3 | A player in `STAGING` / `WAITING` / `ROUTING` has `bladeEnabled == false` and the placeholder inventory. | Set in `PlayerWaitingGate.engage` on entry to `STAGING`, cleared only in `disengage` on the `ROUTING → ACTIVE` transition. |
| I4 | Phase transitions only via `JoinSession` methods. | `phase` field is `private`; no setter; transition methods are the only mutators. Invalid transitions throw `IllegalStateException`. |
| I5 | Repeated transition calls produce no duplicates and no leaks. | Every transition method begins with `if (phase == target || terminated) return;` and cancels prior task handles before scheduling new ones. |
| I6 | A `MenuSlotGrid` slot is held for exactly the duration of a session. | Acquired in `JoinSessionArbiter.onSpawnLocation`, released by `JoinSession.terminate` or by the arbiter directly only when no session was constructed (quit during the 50 ms deferral window). |
| I7 | Stashed inventory is owned by `InventoryStashRepository` for the lifetime of the session. | `stash` on `engage`; `clear` on `disengage`; `consume` on `abort`. No other writers. |
| I8 | A player in `ACTIVE` has no `JoinSession` in the map. | `enterActive` removes the session from the map after `disengage`. No other code reaches into the map. |
| I9 | Suppression of anchored-item upkeep matches gate engagement. | `engage` calls `setAllAnchoredItemUpkeep(false)` *before* clearing inventory; `disengage` and `abort` re-enable on exit. |

---

## Extension points

### Adding a new destination

1. Add a new value to `Destination` whose `spawn()` returns a configured `Location`.
2. Add a matching `public static Location DESTINATION_X` field plus `register(...)` call
   in `JoinSequenceConfig`.
3. Add the YAML default block under `join_sequence.destination.x` in
   `src/main/resources/config.yaml`.
4. Wire a button in `JoinRouterMenu.open()` that raises the new value through its
   `onSelect` callback. Slot it into the `Gui.Builder` structure.

No changes to `JoinSession`, `JoinSessionArbiter`, or the gate are required.

### Swapping the stash repository backing

The default in-memory implementation is constructed at one site —
`Sword.onEnable()`:

```java
inventoryStashRepository = new InMemoryInventoryStashRepository();
```

To upgrade to a durable backing, implement `InventoryStashRepository` and replace that
single line:

```java
inventoryStashRepository = new JdbcInventoryStashRepository(dataSource);
```

No call site changes — `PlayerWaitingGate` consumes through the interface.

### Adding a new phase

Phases are intentionally minimal. Before adding a new phase consider whether the
behaviour can fit into one of the existing phases or as a sub-state of the gate. If a
new phase is genuinely needed:

1. Add the value to `JoinPhase`.
2. Decide every transition into and out of the new phase — phases without a clear
   transition story are an architectural smell.
3. Add the corresponding intent method on `JoinSession`. Match the existing pattern:
   guard precondition, cancel prior-phase task handles, set up new-phase resources, set
   `phase`.
4. Update the lifecycle diagram in this README.

---

## Threading model

All `JoinSession` transition methods must be called on the Bukkit main thread. The
session is not thread-safe internally — it relies on the main-thread guarantee.

The scheduler tasks registered via `TimeArbiter` dispatch their bodies on the main
thread, so callbacks from those tasks (including the routing countdown's last-iteration
callback that drives `enterActive`) inherit the same threading guarantee.

`InventoryStashRepository` implementations are required to be safe for concurrent calls
because `PlayerQuitEvent` can fire from any thread before being marshalled to the main
thread. The default `InMemoryInventoryStashRepository` uses `ConcurrentHashMap`.

---

## Design rationale

### Why `PlayerSpawnLocationEvent` and not `PlayerJoinEvent`

`PlayerJoinEvent` fires after the player has been placed in the world at their
saved-position. A delayed teleport from that handler races against Paper's chunk-load
+ saved-position restore on rejoin and frequently loses. `PlayerSpawnLocationEvent`
fires *before* the player is placed — calling `event.setSpawnLocation` is the
canonical Paper API for overriding the spawn position. There is no race because the
player materializes directly at the dark-room slot.

### Why a hard input gate at `InputRouter.route` and `SwordPlayer.act`

Per-node `visibleIf` gates are useful for fine-grained `NORMAL`-mode distinctions but
they default to "always visible" — every node added to the input tree is visible
unless someone remembers to gate it. That default-allow design leaked ability dispatch
into creative mode and the join phase. Two early-returns at the dispatch chokepoints
guarantee the invariant *"WAITING ⇒ no input dispatch"* structurally — no node can opt
out, no future contributor can forget the gate.

### Why `setAllAnchoredItemUpkeep` and not a new flag

`SwordPlayer` already exposes `setAllAnchoredItemUpkeep(boolean)` and the cutscene and
creative-dev modes already use it as the canonical "suppress automatic item
replacement" toggle. Reusing the existing toggle keeps the suppression mechanism
consistent across modes and avoids boolean-flag explosion on `SwordPlayer`.

### Why a repository interface for the stash with no DB yet

The user explicitly asked for a database stub. An interface plus an in-memory
implementation is the cleanest stub form — future durable storage swaps in via a
single constructor change in `Sword.onEnable`. A no-op TODO method has the same
forgetfulness risk as not stashing at all; the interface formalizes the contract now
so the inventory is correctly preserved across the `WAITING → terminate → abort`
restore path.

### Why a `Destination` enum and not raw `Location`s

The menu surfaces user intent as one of these enum values; it never deals in raw world
locations. The session resolves the chosen destination to a `Location` via
`JoinSequenceConfig` at the moment of teleport, so runtime config edits via the
in-game config menu take effect on the next click without re-instantiation. Adding a
new destination is one enum value plus one config entry plus one button — no menu
logic changes.

### Why `gate.engage` happens in `enterStaging` not `enterWaiting`

The 1.5 second `LOADING_WAIT_MS` delay exists to let the client receive the
spawn-location packet and load chunks before the GUI appears. That delay does *not*
need to apply to the lockdown — placing the player in `ActivationContext.WAITING` and
clearing their inventory is local server-side work that should happen as early as
possible. Engaging the gate on entry to `STAGING` rather than waiting for the `WAITING`
transition closes a 1.5 second window where a fresh-spawn player could trigger
abilities before the input gate engaged.
