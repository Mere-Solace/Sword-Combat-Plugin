# UmbralBlade State Machine Implementation Status

> **Status**: 🚧 In Progress - Migration from manual state management to formal State Machine pattern
>
> **Related ADR**: [ADR 004: UmbralBlade State Machine Architecture](../decisions/004-umbral-blade-state-machine.md)
>
> **Tracking Issue**: [#98 - Umbral Blade State Machine](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/98)

## Overview

This document tracks the implementation progress of the UmbralBlade state machine architecture as defined in ADR 004. The codebase is currently in a **partially-refactored state** with compilation errors that need resolution before the migration can be completed.

---

## ✅ Completed Work

### Generic State Machine Framework

**Location**: `src/main/java/btm/sword/system/statemachine/`

| File | Purpose | Status |
|------|---------|--------|
| `StateMachine.java` | Core state machine with transition management and tick logic | ✅ Complete |
| `State.java` | Abstract base class with `onEnter()`, `onExit()`, `onTick()` lifecycle | ✅ Complete |
| `Transition.java` | Record defining state transitions with guards and actions | ✅ Complete |

### State Class Implementations

**Location**: `src/main/java/btm/sword/system/entity/umbral/statemachine/state/`

| State | File | Entry Actions | Exit Actions | Status |
|-------|------|--------------|--------------|--------|
| SHEATHED | `SheathedState.java` | Set transformation, stop idle, give link item | - | ✅ Complete |
| STANDBY | `StandbyState.java` | Set transformation, start hovering | Stop idle movement | ✅ Complete |
| WIELD | `WieldState.java` | Hide display, give blade item | Show display, give link item | ✅ Complete |
| RECALLING | `RecallingState.java` | Stop idle, begin return animation | - | ✅ Complete |
| RETURNING | `ReturningState.java` | Stop idle, auto-return to sheath | - | ✅ Complete |
| WAITING | `WaitingState.java` | Start idle, register as interactable | Stop idle, unregister (TODO) | ✅ Complete |
| ATTACKING_QUICK | `AttackingQuickState.java` | Stop idle, execute quick attack | - | ✅ Complete |
| ATTACKING_HEAVY | `AttackingHeavyState.java` | Stop idle, execute heavy attack | - | ✅ Complete |
| LUNGING | `LungingState.java` | Stop idle, lunge toward target (TODO) | Stop movement | ⚠️ Needs Implementation |
| FLYING | `FlyingState.java` | Stop idle, physics trajectory (TODO) | Stop physics | ⚠️ Needs Implementation |
| LODGED | `LodgedState.java` | Attach to target, impalement (TODO) | Detach from target (TODO) | ⚠️ Needs Implementation |

**Documentation**: All state classes include comprehensive Javadocs documenting:
- Purpose and behavior
- Entry/exit actions
- Valid transitions
- Implementation TODOs

---

## 🚧 Compilation Errors (BLOCKING)

The following errors prevent compilation and must be fixed before testing:

### Error 1: Missing `attackEndCallback` Field

**Location**: `UmbralBlade.java:364`

**Issue**: The `attackEndCallback` Runnable field was removed during refactoring but is still referenced in `loadBasicAttacks()`.

**Fix Required**:
```java
// Add field declaration after line 72:
private final Runnable attackEndCallback;

// Initialize in constructor BEFORE loadBasicAttacks() call:
this.attackEndCallback = () -> {
    setState(UmbralState.WAITING); // Transition to WAITING when attack completes
};
```

**Impact**: Blocks `loadBasicAttacks()` compilation

---

### Error 2: Missing `setState()` and `getState()` Wrapper Methods

**Locations**:
- `UmbralBlade.java:336` - `setState(UmbralState.SHEATHED)` call in callback
- `UmbralBladeAction.java:12, 22` - `getState()` calls in switch statements
- `UmbralBladeAction.java:13, 23, 24, 33` - `setState(UmbralState)` calls

**Issue**: These methods were removed when migrating to the state machine, but `UmbralBladeAction` still uses the enum-based API.

**Fix Required**:
```java
/**
 * Sets the state of the UmbralBlade by transitioning through the state machine.
 * <p>
 * This is a wrapper method that maps UmbralState enum values to State class instances
 * and delegates to the state machine for validation and execution.
 * </p>
 *
 * @param newState The target UmbralState enum value
 */
public void setState(UmbralState newState) {
    State<UmbralBlade> targetState = switch (newState) {
        case SHEATHED -> new SheathedState();
        case STANDBY -> new StandbyState();
        case WIELD -> new WieldState();
        case RECALLING -> new RecallingState();
        case RETURNING -> new ReturningState();
        case WAITING -> new WaitingState();
        case ATTACKING_QUICK -> new AttackingQuickState();
        case ATTACKING_HEAVY -> new AttackingHeavyState();
        case LUNGING -> new LungingState();
        case FLYING -> new FlyingState();
        case LODGED -> new LodgedState();
    };

    transitionTo(targetState);
}

/**
 * Gets the current state as an UmbralState enum value.
 * <p>
 * This wrapper method maintains backward compatibility with code that uses
 * the UmbralState enum instead of State class instances.
 * </p>
 *
 * @return The current UmbralState enum value
 */
public UmbralState getState() {
    String stateName = bladeStateMachine.getState().name();
    return UmbralState.valueOf(stateName);
}

/**
 * Internal method to transition to a new state using the state machine.
 * <p>
 * Searches registered transitions for a valid path from current state to target state.
 * If found, executes the transition. If not found, forces the transition (used during
 * initialization).
 * </p>
 *
 * @param targetState The target State instance
 */
private void transitionTo(State<UmbralBlade> targetState) {
    // The StateMachine.tick() method handles transition checking
    // For manual transitions, we need to find and execute the transition
    boolean transitioned = false;

    for (var entry : bladeStateMachine.getTransitions().entrySet()) {
        var transition = entry.getKey();
        var currentState = bladeStateMachine.getState();

        if (transition.from().getClass().equals(currentState.getClass())
            && transition.to().getClass().equals(targetState.getClass())
            && transition.condition().test(this)) {

            // Execute transition action
            transition.onTransition().accept(this);

            // Let state machine handle the state change
            // This will call onExit() on current state and onEnter() on new state
            bladeStateMachine.setState(targetState);

            transitioned = true;
            break;
        }
    }

    if (!transitioned) {
        // No explicit transition found - force it (used during initialization)
        bladeStateMachine.setState(targetState);
    }
}
```

**Note**: The `StateMachine` class needs a public `setState()` method or the above `transitionTo()` needs adjustment to access package-private methods.

**Impact**: Blocks `UmbralBlade` and `UmbralBladeAction` compilation

---

### Error 3: Invalid `onWield()` Method Reference

**Location**: `UmbralBlade.java:114`

**Issue**: The lambda calls `((UmbralBlade) blade).onWield()` but this method doesn't exist. The wield logic is implemented in `WieldState.onEnter()`.

**Current Code**:
```java
bladeStateMachine.addTransition(new Transition<>(
    new StandbyState(),
    new WieldState(),
    blade -> true,
    blade -> ((UmbralBlade) blade).onWield() // ❌ Method doesn't exist
));
```

**Fix Required**:
```java
bladeStateMachine.addTransition(new Transition<>(
    new StandbyState(),
    new WieldState(),
    blade -> true,
    blade -> {} // ✅ Empty - logic handled by WieldState.onEnter()
));
```

**Impact**: Blocks `UmbralBlade` compilation

---

## 📋 Remaining Implementation Tasks

### 1. Complete Transition Definitions

**Status**: ⚠️ Only 3 transitions registered, need ~20 total

**Location**: `UmbralBlade.initStateMachine()`

**Required Transitions**:
```java
// SHEATHED transitions
SHEATHED → STANDBY (Q+F pressed, isActive)
SHEATHED → WIELD (F+Left Click)

// STANDBY transitions
STANDBY → SHEATHED (Q+F pressed)
STANDBY → WIELD (F+Left Click)
STANDBY → ATTACKING_QUICK (Left Click)
STANDBY → ATTACKING_HEAVY (Hold Left Click)

// WIELD transitions
WIELD → STANDBY (Q+F pressed)
WIELD → SHEATHED (Q+F pressed)

// ATTACKING transitions
ATTACKING_QUICK → WAITING (attack completes)
ATTACKING_HEAVY → WAITING (attack completes)

// WAITING transitions
WAITING → STANDBY (player picks up)
WAITING → RETURNING (distance > 20 blocks OR idle > 30 seconds)

// RECALLING/RETURNING transitions
RECALLING → SHEATHED (recall completes)
RETURNING → SHEATHED (return completes)

// FLYING transitions
FLYING → LODGED (collision with entity/block)
FLYING → WAITING (lands on ground)
FLYING → RECALLING (manual recall)

// LODGED transitions
LODGED → RECALLING (manual recall)
LODGED → WAITING (target destroyed)

// LUNGING transitions
LUNGING → LODGED (hits target)
LUNGING → WAITING (lunge misses)
```

**Implementation Template**:
```java
bladeStateMachine.addTransition(new Transition<>(
    new SourceState(),           // From state
    new TargetState(),           // To state
    blade -> guardCondition(),   // Guard condition (or blade -> true)
    blade -> transitionAction()  // Transition action (or blade -> {})
));
```

---

### 2. Implement Guard Condition Helper Methods

**Status**: ⚠️ Not yet implemented

**Location**: `UmbralBlade.java` (add as private methods)

**Required Methods**:
```java
/**
 * Checks if the blade is too far from the wielder or has been idle too long.
 * Used by WAITING → RETURNING transition.
 *
 * @return true if blade should auto-return
 */
private boolean isTooFarOrIdleTooLong() {
    double distance = thrower.entity().getLocation().distance(display.getLocation());
    long timeSinceLastAction = System.currentTimeMillis() - lastActionTime;
    return distance > 20.0 || timeSinceLastAction > 30000; // 20 blocks or 30 seconds
}

/**
 * Checks if the blade has collided with a target entity.
 * Used by FLYING → LODGED and LUNGING → LODGED transitions.
 *
 * @return true if blade hit a valid target
 */
private boolean hasHitTarget() {
    return hitEntity != null && hitEntity.isValid();
}

/**
 * Checks if the blade has landed on the ground.
 * Used by FLYING → WAITING transition.
 *
 * @return true if blade is grounded
 */
private boolean hasLanded() {
    return grounded;
}

/**
 * Checks if the lodged target entity has been destroyed.
 * Used by LODGED → WAITING transition.
 *
 * @return true if target is dead/destroyed
 */
private boolean isTargetDestroyed() {
    return hitEntity == null || !hitEntity.isValid();
}

/**
 * Checks if a lunge attack missed its target.
 * Used by LUNGING → WAITING transition.
 *
 * @return true if lunge completed without hitting
 */
private boolean lungeMissed() {
    return !hasHitTarget() && !inFlight;
}
```

---

### 3. Implement TODO Features in State Classes

**Status**: ⚠️ Multiple TODOs flagged for game design decisions

#### WaitingState (Line 57)
```java
// TODO: Unregister from InteractiveItemArbiter when leaving this state
```
**Fix**: Add to `onExit()`:
```java
public void onExit(UmbralBlade blade) {
    blade.endIdleMovement();
    InteractiveItemArbiter.remove(blade);
}
```

#### LungingState (Line 51)
```java
// TODO: Implement lungeToTarget logic when target system is ready
```
**Status**: Requires game design - lunge distance, speed, particle effects

#### LodgedState (Lines 51-52, 57-58)
```java
// TODO: Attach blade display to target entity/block
// TODO: Apply impalement effects (bleeding, damage over time)
// TODO: Detach from target
// TODO: Remove impalement effects
```
**Status**: Requires game design - damage values, visual effects, attachment mechanics

#### FlyingState
- Complete physics trajectory simulation
- Collision detection with entities and blocks
- Particle trail effects

---

### 4. Integrate State Machine Tick

**Status**: ⚠️ Not yet integrated with game loop

**Location**: `UmbralBlade.onTick()`

**Current Code**:
```java
public void onTick() {
    if (!active) {
        thrower.message("Umbral Blade Not active.");
        return;
    }

    if (!thrower.isValid()) {
        thrower.message("Ending Umbral Blade");
        dispose();
    }

    if (display != null && active && inState(UmbralState.SHEATHED)) {
        updateSheathedPosition();
    }
}
```

**Required Change**:
```java
public void onTick() {
    if (!active) {
        thrower.message("Umbral Blade Not active.");
        return;
    }

    if (!thrower.isValid()) {
        thrower.message("Ending Umbral Blade");
        dispose();
    }

    // Tick the state machine - this will:
    // 1. Call current state's onTick() method
    // 2. Check all registered transitions for valid guard conditions
    // 3. Execute first valid transition (if any)
    bladeStateMachine.tick();
}
```

**Impact**: Enables automatic state transitions based on guard conditions

---

## 🧪 Testing Checklist

Once compilation errors are fixed, test these state transitions:

- [ ] SHEATHED → STANDBY (Q+F press)
- [ ] STANDBY → SHEATHED (Q+F press)
- [ ] STANDBY → ATTACKING_QUICK (Left Click)
- [ ] ATTACKING_QUICK → WAITING (attack completes via callback)
- [ ] WAITING → RETURNING (move 20+ blocks away)
- [ ] RETURNING → SHEATHED (auto-return completes)
- [ ] SHEATHED → WIELD (F+Left Click)
- [ ] WIELD → STANDBY (Q+F press)
- [ ] Invalid transitions are rejected (e.g., WIELD → ATTACKING)

**Test Method**:
```java
@Test
public void testBasicStateTransitions() {
    UmbralBlade blade = new UmbralBlade(player, weapon);

    // Test SHEATHED → STANDBY
    assertEquals(UmbralState.SHEATHED, blade.getState());
    blade.setState(UmbralState.STANDBY);
    assertEquals(UmbralState.STANDBY, blade.getState());

    // Test STANDBY → ATTACKING_QUICK
    blade.setState(UmbralState.ATTACKING_QUICK);
    assertEquals(UmbralState.ATTACKING_QUICK, blade.getState());

    // Verify state machine history
    assertTrue(blade.getStateMachine().getHistory().size() >= 2);
}
```

---

## 📊 Progress Summary

| Category | Progress | Status |
|----------|----------|--------|
| State Classes | 11/11 (100%) | ✅ Complete |
| Javadoc Documentation | 11/11 (100%) | ✅ Complete |
| Compilation Errors | 0/3 (0%) | ❌ Blocking |
| Transition Definitions | 3/20 (15%) | ⚠️ In Progress |
| Guard Condition Methods | 0/5 (0%) | ⚠️ Pending |
| TODO Features | 0/4 (0%) | ⚠️ Pending |
| State Machine Tick | 0/1 (0%) | ⚠️ Pending |
| Unit Tests | 0/8 (0%) | ⚠️ Pending |

**Overall**: 🚧 30% Complete - Blocked by compilation errors

---

## Next Steps

1. **Fix Compilation Errors** (PRIORITY):
   - Add `attackEndCallback` field and initialization
   - Add `setState()`/`getState()` wrapper methods
   - Fix `onWield()` method reference

2. **Complete Transition Definitions**:
   - Register all 20 transitions in `initStateMachine()`
   - Implement 5 guard condition helper methods

3. **Integrate State Machine Tick**:
   - Update `onTick()` to call `bladeStateMachine.tick()`

4. **Testing**:
   - Compile and test basic state transitions
   - Verify guard conditions work correctly
   - Test invalid transition rejection

5. **Feature Implementation**:
   - Implement TODO features based on game design decisions
   - Add comprehensive unit tests

---

## References

- **ADR**: [ADR 004: UmbralBlade State Machine Architecture](../decisions/004-umbral-blade-state-machine.md)
- **Issue**: [#98 - Umbral Blade State Machine](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/98)
- **Source Code**:
  - [StateMachine.java](../../src/main/java/btm/sword/system/statemachine/StateMachine.java)
  - [State.java](../../src/main/java/btm/sword/system/statemachine/State.java)
  - [UmbralBlade.java](../../src/main/java/btm/sword/system/entity/umbral/UmbralBlade.java)
  - [State implementations](../../src/main/java/btm/sword/system/entity/umbral/statemachine/state/)

---

**Last Updated**: 2025-11-12
