# Documentation Standards

## Code Documentation Standards

This project follows **Microsoft Java Documentation Standards** for inline code documentation.

## Javadoc Requirements

### Classes

All public classes must include:

```java
/**
 * Brief description of the class purpose.
 *
 * <p>More detailed description if needed, explaining the class's
 * role in the system and any important usage notes.</p>
 *
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
 * <p>Additional details about behavior, edge cases, or important notes.</p>
 *
 * @param paramName description of the parameter
 * @param anotherParam description of another parameter
 * @return description of return value
 * @throws ExceptionType when and why this exception is thrown
 */
public ReturnType methodName(Type paramName, Type anotherParam) throws ExceptionType {
}
```

### Fields

Public fields and important private fields should be documented:

```java
/** Brief description of the field's purpose. */
private String fieldName;
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
- Code blocks: Always specify language (`java,`bash, etc.)
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
