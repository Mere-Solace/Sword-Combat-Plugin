# PMD Static Analysis Guide

PMD is a source code analyzer that detects common programming flaws like unused variables, empty
catch blocks, and potential bugs.

## Philosophy: Developer-Friendly Analysis

This project uses PMD with a **non-blocking, informational approach**:

- PMD violations **never fail builds**
- Reports are **informational only**
- Developers can ignore warnings while actively developing
- Encourages improvement without forcing it

### Why This Approach?

**Development Reality:**

```java
// Developer writes this FIRST (planning ahead):
Player player = event.getPlayer();
Location loc = player.getLocation();  // "Unused" right now
World world = loc.getWorld();         // "Unused" right now

// Then implements logic LATER:
// TODO: add world border check here
```

If we blocked on unused variables, developers would need to:

- Write code in perfect order (can't scaffold)
- Remove/re-add variables multiple times
- Lose flow state constantly

**Our approach:** PMD reports "Hey, you have unused variables" but lets you keep working.

## What PMD Detects

### Priority Levels

| Priority | Severity | Action Required                  |
| -------- | -------- | -------------------------------- |
| 1        | Critical | Fix ASAP - likely causes bugs    |
| 2        | High     | Should fix - potential issues    |
| 3        | Medium   | Review and fix if appropriate    |
| 4        | Low      | FYI only - fix if convenient     |
| 5        | Info     | Suggestions, ignore if preferred |

### Rules Configured

#### Unused Code (Low Priority)

- **UnusedLocalVariable** (P4) - Local variables declared but never used
- **UnusedPrivateField** (P3) - Private fields that are never read
- **UnusedPrivateMethod** (P3) - Private methods never called
- **UnusedFormalParameter** (P4) - Method parameters not used (often required by interfaces)

#### Potential Bugs (High Priority)

- **EmptyCatchBlock** (P2) - Catch blocks with no error handling
- **OverrideBothEqualsAndHashcode** (P1) - Override one but not the other
- **AvoidBranchingStatementAsLastInLoop** (P3) - Break/continue at end of loop

#### Performance (Low Priority)

- **UseStringBufferForStringAppends** (P4) - Use StringBuilder in loops

#### Documentation (Low Priority)

- **UncommentedEmptyMethodBody** (P4) - Empty methods should explain why

## Usage

### Run PMD Analysis

```bash
./gradlew pmdMain
```

### View Report

Open `build/reports/pmd/main.html` in a browser.

### Sample Output

```
A:\Projects\Sword-Combat-Plugin\src\main\java\btm\sword\system\entity\SwordPlayer.java:322:
    UnusedLocalVariable: Avoid unused local variables such as 'type'.

A:\Projects\Sword-Combat-Plugin\src\main\java\btm\sword\system\action\skill\SkillNode.java:11:
    UnusedPrivateField: Avoid unused private fields such as 'castDuration'.
```

## Suppressing False Positives

### Use TODO/FIXME Comments

PMD automatically ignores variables in code with TODO/FIXME:

```java
public void handleInventoryInput(InventoryClickEvent e) {
    // TODO: Implement inventory validation
    Inventory inv = e.getInventory();  // Won't be flagged
    ClickType clickType = e.getClick();  // Won't be flagged

    // ... rest of implementation coming later
}
```

### PMD Suppression Annotation

For intentional unused fields:

```java
@SuppressWarnings("PMD.UnusedPrivateField")
private String reservedForFutureFeature;
```

### Inline Suppression

```java
private int value;  // NOPMD - Reserved for serialization
```

## Common Scenarios

### Scenario 1: Work In Progress

**Situation:** You're scaffolding a method and haven't used all variables yet.

**Solution:** Add a TODO comment or ignore PMD warnings until implementation is complete.

```java
public void processAttack() {
    // TODO: Implement combo system
    Player attacker = getAttacker();  // PMD won't flag this
    int comboCount = getComboCount();  // PMD won't flag this

    // Will implement logic here...
}
```

### Scenario 2: Required by Interface

**Situation:** Interface forces you to accept parameters you don't need.

**Solution:** PMD priority 4 (Low) for unused parameters - just informational.

```java
@Override
public void onEvent(CustomEvent event) {
    // event parameter required by interface but not used yet
    doSomething();
}
```

### Scenario 3: Future Feature Placeholder

**Situation:** Fields for planned features not yet implemented.

**Solution:** Add TODO comment or suppression annotation.

```java
// TODO: Implement skill damage calculation system
@SuppressWarnings("PMD.UnusedPrivateField")
private double mainDamageVal;
private double rangeMultiplier;
```

### Scenario 4: Debugging Variables

**Situation:** Variables used only during development/debugging.

**Solution:** Either remove before commit or suppress if intentionally keeping for future debugging.

```java
// Keeping for debugging purposes
@SuppressWarnings("PMD.UnusedLocalVariable")
String debugInfo = generateDebugInfo();
```

## Integration with GitHub Actions

PMD runs automatically on every push and PR:

```yaml
- name: Run PMD analysis
  run: ./gradlew pmdMain --no-daemon
  continue-on-error: true # Never blocks
```

Reports are uploaded as artifacts for review.

## Current Findings

Initial PMD run found **34 violations**:

- 10 unused local variables (SwordPlayer.java methods scaffolding)
- 16 unused private fields (SkillNode.java - likely planned features)
- 8 uncommented empty method bodies

All are informational - none block development.

## Best Practices

### Do:

- Review PMD reports periodically
- Fix obvious issues (truly unused variables)
- Add TODO comments for planned code
- Use suppression for intentional cases

### Don't:

- Obsess over every violation
- Remove variables you're actively working with
- Block PRs based on PMD warnings
- Remove fields that are planned features

## Configuration

Edit rules in `config/pmd/pmd-rules.xml`:

```xml
<rule ref="category/java/bestpractices.xml/UnusedLocalVariable">
    <priority>4</priority>  <!-- Change priority 1-5 -->
    <properties>
        <property name="violationSuppressRegex" value=".*TODO.*|.*FIXME.*"/>
    </properties>
</rule>
```

## Related Documentation

- [automation-tools.md](automation-tools.md) - Overview of all tools
- [github-actions-guide.md](github-actions-guide.md) - CI/CD workflow
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Contribution workflow

## FAQ

**Q: Will PMD block my commits?** A: No, PMD is configured with `ignoreFailures = true` and never
blocks builds.

**Q: Should I fix all PMD violations before committing?** A: No, fix what makes sense for your
changes. Violations are informational.

**Q: What if PMD flags code I'm actively working on?** A: Add a `// TODO:` comment or ignore it
until your implementation is complete.

**Q: Can I disable PMD?** A: Yes, but it provides valuable feedback. Consider reviewing reports even
if you don't fix everything.

**Q: How is PMD different from Checkstyle?** A: Checkstyle checks style (formatting, naming). PMD
checks logic (unused code, bugs). Both are complementary.

---

Last Updated: 2025-10-30 PMD Version: 7.0.0
