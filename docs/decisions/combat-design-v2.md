# Core Combat System — Design Document v2.0

## Design Philosophy

**"Master the Blade, Improvise with Everything Else"**

- The Umbral Blade is your primary weapon and skill expression tool
- Found weapons are situational tools, especially when Soulfire is depleted
- Every hit matters — discrete Hearts make damage feel significant
- Soulfire economy creates moment-to-moment tactical decisions
- Simple inputs, deep mastery through the execution tree

---

## Input System

### Available Inputs
Only 5 raw inputs exist. Complexity is achieved through an **execution tree** — combinations and sequences that expand the action space well beyond 5.

| Symbol | Input | Notes |
|--------|-------|-------|
| `L` | Left Click | Always detectable as a press |
| `R` | Right Click | Only detectable when holding an item; can detect hold |
| `F` | Swap | Always detectable as a press |
| `D` | Drop | Only detectable when holding an item |
| `S` | Crouch | Can detect hold |

### Action Mapping

| Input | Action | Notes |
|-------|--------|-------|
| `L` | Attack | Context-dependent (see Umbral Blade states) |
| `R` (hold) | Block | Hold to maintain; costs Soulfire/second |
| `F + F` | Dash Forward | In look direction. If targeting a lodged blade, dashes to and reclaims it |
| `S + S` | Dash Backward | Opposite of look direction. Same blade-reclaim logic |
| `S + L` | Grab | Grab nearby object or enemy |
| `S + L`, then `L` | Grab Bash | Melee with grabbed object |
| `S + L`, then `F` | Grab Throw | Throw grabbed object in look direction |
| `D + R` (hold → release) | Throw Weapon | Hold charges the throw, release launches |
| `F + L + [F/L/R/D]` | Umbral Skill | Four distinct Umbral Blade special moves (see below) |

> **Note:** `R` and `D` inputs only register while an item is held in the active slot. Design around this — players holding the Soul Link or a found weapon should always have access to block and throw.

### Aerial Variants
Basic attacks performed while airborne have modified shapes and altered cooldowns. No new inputs required — aerial state is detected automatically.

---

## Hotbar Layout

9 slots total. Layout is fixed:

| Slot | Purpose |
|------|---------|
| 1 | Umbral Blade / Soul Link (always here) |
| 2–3 | Active skill slots |
| 4–7 | Found weapons / picked-up tools |
| 8 | TBD |
| 9 | Menu |

**Soul Link** is the item in slot 1 when the Umbral Blade is deployed. It's the vessel that remains in hand, enabling Umbral Skill inputs (`F+L+[...]`) and blade-related actions even when the blade itself is lodged in an enemy across the room.

---

## Health Systems

### Player Health: Discrete Damage

Players have **Hearts** — discrete health units. Every hit taken removes exactly 1 Heart, regardless of the attack source. No damage scaling, no fractions.

- **Starting Hearts:** 10
- **Max Hearts (upgraded):** ~20

**Why discrete?** It makes damage feel consistent and legible. Players always know their margin — "I can take 4 more hits" is a cleaner mental model than watching a bar erode unpredictably. Every engagement becomes a countdown, every mistake costs something real.

### Enemy Health: Traditional Pools

Enemies use standard HP bars, allowing multi-hit combos and satisfying kill sequences. If enemies also had discrete health, combat would feel like a series of separate transactions rather than a flowing rhythm. Traditional HP lets the Umbral Blade combo system breathe.

---

## Soulfire Economy

Soulfire is your universal resource. It powers everything: movement, defense, offense, and healing. Managing it is the core loop.

- **Starting Capacity:** 100
- **Max Capacity (upgraded):** ~200

### Gaining Soulfire

| Source | Amount |
|--------|--------|
| Umbral Blade hit | +10–15 |
| Found weapon hit | +5 |
| Bare-hand parry | +25 |
| Killing an enemy | +30 |
| Blade impaled in enemy (per second) | +5 |

### Spending Soulfire

| Action | Cost |
|--------|------|
| Dash | 15 |
| High jump | 10 |
| Blocking (per second) | 5 |
| Healing (channel, per Heart) | 50 |
| Recalling distant blade | 10 |
| Heavy Umbral abilities | 20–40 |

### Losing Soulfire (from hits)

| Situation | Soulfire Lost | Hearts Lost |
|-----------|---------------|-------------|
| Hit while blocking | −20 | 0 |
| Hit unblocked | −10 | −1 |

**Design Principle:** Aggressive play refills Soulfire. Passive play starves you. Running away and turtling is mechanically punished — the resource you need for survival only comes from engaging.

---

## The Umbral Blade

### What It Is

A blade that hovers behind the player's shoulder when idle. It executes commands on input and can be sent out, lodged in enemies, or held directly. It is never lost — only repositioned.

### Blade States

| State | Description |
|-------|-------------|
| **Hovering** | Default. Rests behind player's shoulder. Ready for any command. Passive Soulfire regen (+2/sec) only when at full Hearts. |
| **Wielded** | Player physically holds the blade. Enables melee combos and healing channel. Disables passive regen. |
| **Attacking** | Executing a command. Cannot be interrupted. Returns to Hovering after completion. |
| **Impaled** | Lodged in an enemy. Drains enemy HP (5/sec), grants Soulfire (5/sec), and creates a dash anchor point. Player can use found weapons freely while blade is embedded. |

### Basic Attacks (`L` — context-dependent)

**Blade Hovering:**
- Tap → Blade lunges in aimed direction
- Hold → Blade charges, releases a powerful slash on release

**Blade Wielded:**
- Click → Standard 3-hit melee combo
- Hold → Heavy overhead slam

**Blade Impaled:**
- Click → Twist blade (damage spike, stagger)
- Hold → Explosive Extract (AoE damage, blade returns — costs 20 Soulfire)

### Umbral Skills (`F + L + [Direction]`)

Performed while holding the Soul Link (slot 1). These are your high-skill tools.

| Input | Skill | Effect | Cost |
|-------|-------|--------|------|
| `F+L+F` | Impale Lunge | Blade shoots forward, impales first target | 0 |
| `F+L+L` (or Back) | Return & Slash | Blade spins back, hitting enemies in its path | 10 |
| `F+L+R` | Orbital Slash | Wide horizontal sweep | 15 |
| `F+L+D` | Aerial Spike | Blade rises then slams down | 15 |

### Dash-to-Blade

When the blade is deployed (Hovering out of reach or Impaled), dashing **toward** it (`F+F`) automatically reclaims it with a bonus attack on arrival:

| Arrival Modifier | Reclaim Attack |
|-----------------|----------------|
| Neutral dash | Overhead slam |
| Dash + Jump | Aerial grab + downward spike |
| Dash + `L` | Grab mid-dash, spinning slash |

This is your primary repositioning tool. It's not just mobility — it's always an attack.

---

## Found Weapons

### Philosophy

Found weapons are **backup and supplemental tools**, not a parallel primary system. The Umbral Blade is always your main expression. Found weapons fill gaps — when the blade is impaled across the room, when Soulfire is low, when you want to stack pressure on a second target.

### Mechanics

- Pick up with `E` (or interact input) near a weapon
- Can hold multiple in slots 4–7
- Durability: 5–10 hits before breaking
- Breaking a weapon grants +10 Soulfire
- Left click attacks with found weapon when Soul Link is not actively wielded
- `D+R` (hold → release) throws the weapon

### Weapon Types

| Type | Character |
|------|-----------|
| Sword | Balanced — 3-hit combo |
| Axe | Slow, high damage per swing |
| Spear | Long reach, fast thrust |
| Mace | Breaks super-armor / forces stagger |

---

## Healing

### How to Heal

1. Wield the Umbral Blade (swap to wielded state)
2. Hold `R` (block input)
3. Enter healing channel — immobile, glowing
4. After 2 seconds: restore 1 Heart
5. Cost: 50 Soulfire per Heart

Taking damage during the channel cancels it. Soulfire is **not** refunded on interruption.

### When to Heal

✅ After clearing a room, before the next  
✅ When Soulfire is above 75 and Hearts are at 3 or below  
✅ Behind cover or after creating distance  

❌ During active combat  
❌ When Soulfire is below 50  
❌ When enemies are in aggro range  

**Why this design?** Healing requires commitment — you can't attack while channeling, and you're spending 50% of your base Soulfire on one Heart. This keeps healing as a deliberate strategic decision rather than a panic button.

---

## Blocking & Parry

### Block (`R` hold)

- Negates all frontal damage (90° cone)
- Costs 5 Soulfire/second while held
- A blocked hit also drains an additional 20 Soulfire instantly
- Does not restore Hearts, only prevents losing them

Blocking is Soulfire-negative even on success. Dashing costs Soulfire but regains it through the follow-up attack. Blocking doesn't. It is a last resort, not a primary strategy.

### Parry (Advanced)

Release block exactly as an attack lands → Parry  
- Staggers the enemy briefly  
- Grants +25 Soulfire  
- High risk, high reward

---

## Combat Scenarios

These scenarios exist to explore the design space and identify feel targets — not as tutorials.

---

### Scenario: The Anchor

*You spot a tight cluster of three enemies in a small room. You impale the central one and pocket its position as your anchor.*

1. `F+L+F` — Impale Lunge on center enemy (anchor set)
2. Pick up dropped spear with `E`
3. Spear-attack the left enemy while center drains
4. `F+F` toward anchor — reclaim blade with overhead slam (center enemy killed)
5. `F+L+R` — Orbital Slash catches both remaining enemies
6. Finish with Wielded melee combo

**Why it works:** The impaled enemy becomes terrain — a drain station and teleport destination. The found weapon isn't a replacement, it's a bridge between blade positions. You leave the room with more Soulfire than you entered.

---

### Scenario: The Desperate Trade

*You're at 3 Hearts, 28 Soulfire, two enemies left. One is a mace-wielder with super-armor.*

1. Resist the dash — 15 Soulfire is too much to spend right now
2. Wield blade, enter careful 3-hit melee on the fodder enemy
3. On its death: +30 Soulfire (now at ~43)
4. Impale Lunge on the mace-wielder
5. Let it drain while staying close — land a bare-hand parry if it swings (+25)
6. At ~70 Soulfire, reclaim blade and finish
7. After room clears: wield, channel-heal back to 4 Hearts

**Why it works:** This is the "danger zone" scenario. With sub-30 Soulfire you cannot afford to dash or use abilities — you play conservatively and use the kill bonus to claw back resources. The mace-wielder's super-armor means the Mace found weapon type would have been valuable here; note this for item placement design.

---

### Scenario: The Juggle

*Wide arena. Four enemies, two ranged. You want to spend as little time standing still as possible.*

1. Impale Lunge on the nearest ranged enemy — it's now pinned and draining
2. Orbital Slash to catch the adjacent melee enemy mid-approach
3. Grab + Throw a barrel at the second ranged enemy (deny their attack window)
4. `F+F` dash back to the impaled ranged enemy — aerial grab reclaim
5. Return & Slash on the way back through the group
6. Finish with wielded melee on the last standing enemy

**Why it works:** Ranged enemies are pressure creators that force movement. Impaling one early removes it from the threat pool while generating passive Soulfire. The grab-throw into the second ranged buys a moment without spending Soulfire. Everything flows around the blade's position as a hub.

---

### Scenario: The Interrupted Heal

*You just cleared a room at 2 Hearts but missed an enemy in the corner. You're mid-channel.*

1. Healing channel is active — you're immobile
2. Enemy strikes: channel cancels, 50 Soulfire is gone, no Heart restored
3. You're now at 2 Hearts, ~25 Soulfire
4. This is the death spiral if you panic — don't

Decision point:  
- Impale Lunge immediately — start the drain clock, buy Soulfire  
- If the enemy has super-armor, wield blade and play for the parry window  
- Do not attempt to re-channel until Soulfire is above 60 and the room is confirmed clear

**Why this matters for design:** The interrupted heal scenario will frustrate players the first time it happens. It needs to feel *fair* — the enemy's presence must be telegraphed and the player's decision to channel should feel like a mistake they made, not a system that punished them arbitrarily. Room clear confirmation cues matter here.

---

## Design Pillars

### 1. The Blade is Central
Every mechanic reinforces Umbral Blade mastery. Found weapons and skills are extensions, not alternatives.

### 2. Soulfire Creates Decisions
Every action has a cost and a context. There are no free moves — only good trades and bad ones.

### 3. Discrete Health = Tension
10 Hearts means every mistake is a countdown. The number is legible. The stakes are clear.

### 4. Aggressive Play is Rewarded
Sitting back and blocking drains resources. Attacking builds them. The system has a direction.

### 5. Simple Inputs, Deep Mastery
Five inputs. An execution tree. Combinations create depth without requiring extra buttons.

---

## Playtesting Questions

### Core Feel (Early)
- Does landing an Umbral Blade hit feel satisfying?
- Is the Hovering state clearly readable — does the player know the blade is ready?
- Can you tell at a glance when Soulfire is critically low?
- Does the execution tree feel discoverable, or does it feel like arbitrary combos?

### Loop Integrity (Mid)
- Does the Impale → Drain → Dash-to-Reclaim loop feel rewarding end-to-end?
- Is healing too risky in practice? Not risky enough?
- Do players understand *when* to use found weapons vs the Umbral Blade?
- Is the 50 Soulfire heal cost making players avoid healing, or making them strategic about it?

### Depth & Mastery (Late)
- Are Umbral Skill commands (`F+L+[...]`) intuitive after an hour of play?
- Can a player manage 3+ enemies simultaneously and feel in control?
- Is the Soulfire economy forcing interesting decisions, or does it feel like a tax?
- Is there visible skill growth between session 1 and session 5?

### Open Design Questions
1. Is 2 seconds the right heal channel time?
2. Should blocking drain Soulfire faster than 5/sec?
3. Is 50 Soulfire per Heart too expensive at base capacity (50% of starting pool)?
4. How many found weapons should spawn per arena? (Current thinking: 2–3)
5. Should enemies drop Soulfire orbs on death, or only the on-kill bonus?
6. What belongs in hotbar slot 8?

---

## Glossary

**Hearts** — Player's discrete health units. Start with 10.

**Soulfire** — Universal resource for mobility, defense, healing, and blade abilities.

**Umbral Blade** — Floating signature weapon. States: Hovering, Wielded, Attacking, Impaled.

**Soul Link** — The item in slot 1 when the Umbral Blade is deployed. Enables Umbral Skill inputs.

**Impale** — Blade lodged in an enemy. Drains its HP, generates Soulfire, creates a dash anchor.

**Dash-to-Blade** — Dashing toward the deployed blade reclaims it with a bonus attack.

**Channel Healing** — Hold block while wielded to restore 1 Heart over 2 seconds. Costs 50 Soulfire.

**Parry** — Release block on impact. Staggers enemy, grants 25 Soulfire.

**Execution Tree** — The input combination system that expands 5 raw inputs into a full action set.

---

*Version 2.0 — February 2026*
