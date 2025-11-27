# Documentation Standards

## Code Documentation Standards

This project follows **Microsoft Java Documentation Standards** for inline code documentation.

## Javadoc Requirements

### General Principles

1. **Be Rich and Comprehensive**: Include all information needed to use the API correctly
2. **Link Liberally**: Use `{@link}` tags to connect related classes and methods
3. **Specify Units**: Always document units for numeric parameters (blocks, ticks, radians, etc.)
4. **Document Preconditions**: State requirements that must be met before calling
5. **Document Postconditions**: Describe the state after method execution
6. **Provide Examples**: Show typical usage patterns for complex APIs
7. **Warn About Pitfalls**: Use `@apiNote` or `@implNote` for important caveats

### Classes

All public classes must include:

```java
/**
 * Brief description of the class purpose.
 *
 * <p>More detailed description explaining the class's role in the system,
 * typical usage patterns, and any important considerations.</p>
 *
 * <p><b>Thread Safety:</b> This class is [thread-safe|not thread-safe].</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * ExampleClass example = new ExampleClass(parameter);
 * example.doSomething();
 * }</pre>
 *
 * @see RelatedClass
 * @see AnotherRelatedClass
 * @author Author Name
 * @since 1.0
 */
public class ExampleClass {
}
```

### Methods

All public methods must include:

```java
/**
 * Brief description of what the method does.
 *
 * <p>More detailed explanation of behavior, including edge cases,
 * performance characteristics, and side effects.</p>
 *
 * <p><b>Preconditions:</b></p>
 * <ul>
 *   <li>Parameter must not be null</li>
 *   <li>Object must be in valid state (e.g., initialized)</li>
 * </ul>
 *
 * <p><b>Units:</b> Distance is in blocks, time is in server ticks (20 ticks/second).</p>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * double distance = calculateDistance(player1, player2);
 * System.out.println("Distance: " + distance + " blocks");
 * }</pre>
 *
 * @param player1 The first player (must not be null)
 * @param player2 The second player (must not be null)
 * @return The distance between players in blocks (always >= 0.0)
 * @throws IllegalArgumentException if either player is null
 * @throws IllegalStateException if either player is not online
 * @see #relatedMethod(Type)
 * @see OtherClass#anotherRelatedMethod()
 */
public double calculateDistance(Player player1, Player player2) {
    // implementation
}
```

### Fields

Public fields and important private fields should be documented with units and constraints:

```java
/**
 * Maximum attack range in blocks.
 *
 * <p>Valid range: 1.0 to 10.0 blocks. Values outside this range
 * may cause unexpected behavior.</p>
 *
 * <p><b>Units:</b> Minecraft blocks (1 block = 1 meter)</p>
 *
 * @see #MIN_ATTACK_RANGE
 */
public static final double MAX_ATTACK_RANGE = 5.0;

/**
 * Gravity damping coefficient for thrown items.
 *
 * <p>Higher values = more floaty arc, lower values = faster fall.</p>
 *
 * <p><b>Units:</b> Dimensionless coefficient (typical range: 30-60)</p>
 * <p><b>Default:</b> 46.0 (approximates realistic gravity at 20 TPS)</p>
 */
public static double gravityDamper = 46.0;
```

### Common Unit Documentation

When documenting numeric values, always specify units:

- **Distance**: blocks (1 block = 1 meter)
- **Time**: ticks (20 ticks/second = 1 second)
- **Angles**: radians (Math.PI = 180 degrees)
- **Velocity**: blocks/tick
- **Damage**: Minecraft health points (1 heart = 2 health points)
- **Percentages**: 0.0-1.0 (not 0-100)
- **Cooldowns**: ticks (e.g., 20 ticks = 1 second)

**Example:**

```java
/**
 * Applies knockback to the target entity.
 *
 * @param target The entity to knock back
 * @param power Knockback strength in blocks/tick (typical range: 0.1-2.0)
 * @param angle Direction angle in radians (0 = east, π/2 = north)
 */
public void applyKnockback(Entity target, double power, double angle) {
    // implementation
}
```

## Documentation Types

### 1. Architecture Decision Records (ADRs)

Located in `@docs/decisions/`, these document significant technical decisions.

**Format**:

- Filename: `###-short-title.md` (e.g., `001-gradle-wrapper.md`)
- Status: Proposed, Accepted, Deprecated, Superseded
- Include: Context, Decision, Reasoning, Consequences, References

### 2. Setup Guides

Located in `@docs/setup/`, these help developers set up their environment.

### 3. API Documentation

Generated from Javadoc comments using `./gradlew javadoc`

## Markdown Standards

- Use GitHub Flavored Markdown
- Headers: Use `#` for titles, `##` for sections, `###` for subsections
- Code blocks: Always specify language (```java,```bash, etc.)
- Links: Use descriptive text, not raw URLs

## Comments in Code

### Good Comments

- **Why**, not what: Explain reasoning, not obvious operations
- Document non-obvious behavior
- Explain complex algorithms
- Note important constraints or edge cases

### Bad Comments

```java
// Bad: Obvious comment
i++; // increment i

// Good: Explains reasoning
i++; // Skip the header row
```

## TODO Comments

Use standard format for tracking work:

```java
// TODO: Description of what needs to be done
// FIXME: Description of what's broken
// NOTE: Important information
```

## References

- [Microsoft Java Documentation Guidelines](https://learn.microsoft.com/en-us/java/openjdk/)
- [Oracle Javadoc Guide](https://www.oracle.com/technical-resources/articles/java/javadoc-tool.html)
