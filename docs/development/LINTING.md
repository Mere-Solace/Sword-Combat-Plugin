# Linting and Code Quality Tools

This document describes all the linters and code quality tools available in the project.

## Quick Reference

```bash
# Check everything (all linters + tests)
./gradlew check

# Individual tools
./gradlew checkstyleMain # Java style checker
./gradlew pmdMain        # Java bug detector
./gradlew spotlessCheck  # Java + Markdown formatter (check only)
./gradlew spotlessApply  # Java + Markdown formatter (auto-fix)
```

## Java Linters

### 1. Checkstyle

**Purpose**: Enforces coding standards and style conventions

**Run:**

```bash
./gradlew checkstyleMain
```

**Report:** `build/reports/checkstyle/main.html`

**What it checks:**

- Naming conventions (classes, methods, variables)
- Code organization and structure
- Whitespace and indentation
- Import statements
- Javadoc presence
- Code complexity
- Magic numbers
- And much more...

**Configuration:** `config/checkstyle/checkstyle.xml`

**Build Behavior:** Warnings only (doesn't fail builds) - `ignoreFailures = true`

### 2. PMD

**Purpose**: Detects potential bugs and code quality issues

**Run:**

```bash
./gradlew pmdMain
```

**Report:** `build/reports/pmd/main.html`

**What it checks:**

- Unused variables and fields
- Dead code
- Overcomplicated expressions
- Duplicate code
- Inefficient code
- Best practice violations
- Potential bugs

**Configuration:** `config/pmd/pmd-rules.xml`

**Build Behavior:** Informational only (never fails builds) - `ignoreFailures = true`

### 3. Spotless (Java)

**Purpose**: Automatically formats Java code to consistent style

**Run:**

```bash
# Check for violations
./gradlew spotlessJavaCheck

# Auto-fix violations
./gradlew spotlessJavaApply
```

**What it fixes:**

- Indentation (4 spaces, no tabs)
- Import ordering and organization
- Removes unused imports
- Trailing whitespace
- Line endings (LF)
- Ensures files end with newline

**Configuration:** Defined in `build.gradle`

**Build Behavior:** FAILS builds if violations found in CI (enforces consistency)

## Markdown Linters

### Spotless (Markdown)

**Purpose**: Automatically formats Markdown files with Prettier

**Run:**

```bash
# Check for violations
./gradlew spotlessMarkdownCheck

# Auto-fix violations
./gradlew spotlessMarkdownApply

# Check both Java and Markdown
./gradlew spotlessCheck

# Fix both Java and Markdown
./gradlew spotlessApply
```

**What it fixes:**

- Line wrapping at 100 characters
- Consistent list formatting
- Spacing around headings
- Consistent emphasis markers
- Code block formatting
- Table alignment

**Configuration:**

```groovy
prettier(['prettier': '2.8.8', 'prettier-plugin-sh': '0.12.8'])
    .config([
        'parser': 'markdown',
        'printWidth': 100,
        'proseWrap': 'always',
        'tabWidth': 2
    ])
```

**Build Behavior:** FAILS builds if violations found in CI

## Running All Checks

### Check Everything

```bash
./gradlew check
```

This runs:

- ✅ Checkstyle (Java style)
- ✅ PMD (Java quality)
- ✅ Spotless (Java + Markdown formatting)
- ✅ Tests (JUnit)

### CI/CD Behavior

In GitHub Actions, the following are enforced:

- **Spotless (Java)** - MUST pass (auto-formatting)
- **Spotless (Markdown)** - MUST pass (auto-formatting)
- **Tests** - MUST pass
- **Checkstyle** - Warnings only
- **PMD** - Warnings only

## Pre-Commit Workflow

Before committing code, run:

```bash
# Auto-fix any formatting issues
./gradlew spotlessApply

# Run all checks
./gradlew check

# Or just verify formatting is correct
./gradlew spotlessCheck
```

## IDE Integration

### IntelliJ IDEA

**Checkstyle Plugin:**

1. Install "Checkstyle-IDEA" plugin
2. Settings → Tools → Checkstyle
3. Add configuration file: `config/checkstyle/checkstyle.xml`

**PMD Plugin:**

1. Install "PMDPlugin" plugin
2. Settings → Tools → PMD
3. Add ruleset: `config/pmd/pmd-rules.xml`

**Spotless:**

- Spotless runs via Gradle, no plugin needed
- Can configure IntelliJ to run spotlessApply on save (optional)

### VS Code

**Checkstyle:**

- Extension: "Checkstyle for Java"
- Configure to use `config/checkstyle/checkstyle.xml`

**Markdown:**

- Extension: "Prettier - Code formatter"
- Will use project's Prettier config automatically

## Customizing Rules

### Checkstyle

Edit: `config/checkstyle/checkstyle.xml`

Example - disable a specific check:

```xml
<!-- Disable magic number check -->
<module name="MagicNumber">
    <property name="severity" value="ignore"/>
</module>
```

### PMD

Edit: `config/pmd/pmd-rules.xml`

Example - exclude a rule:

```xml
<rule ref="category/java/errorprone.xml">
    <exclude name="AvoidLiteralsInIfCondition"/>
</rule>
```

### Spotless (Java)

Edit `build.gradle`:

```groovy
spotless {
    java {
        // Add custom formatting rules
        importOrder('java', 'javax', '', 'org', 'com')
        // Or use Google Java Format
        googleJavaFormat()
    }
}
```

### Spotless (Markdown)

Edit `build.gradle`:

```groovy
format 'markdown', {
    prettier().config([
        'printWidth': 120,  // Change line wrap width
        'proseWrap': 'never'  // Disable line wrapping
    ])
}
```

## Suppressing Warnings

### Java - @SuppressWarnings

For legitimate cases where warnings should be ignored:

```java
@SuppressWarnings("PMD.UnusedPrivateField")  // PMD
@SuppressWarnings("checkstyle:MagicNumber")  // Checkstyle
private static final int PORT = 8080;
```

### Markdown - Disable Prettier

For code blocks or sections that shouldn't be formatted:

```markdown
<!-- prettier-ignore -->
| Unformatted | Table |
|---|---|
```

## Troubleshooting

### Spotless "File has been changed" error

```bash
# Reset and reapply formatting
./gradlew spotlessApply
```

### Conflicting IDE and Spotless formatting

- Let Spotless win - always run `spotlessApply` before committing
- Configure your IDE to match Spotless settings

### Gradle cache issues

```bash
# Clear Gradle cache
./gradlew clean

# Rerun checks
./gradlew check
```

## Best Practices

1. **Run `spotlessApply` before every commit** - Ensures consistent formatting
2. **Fix legitimate warnings** - Checkstyle and PMD warnings often indicate real issues
3. **Don't suppress without reason** - Only suppress warnings when truly necessary
4. **Keep configs in sync** - IDE formatting should match Spotless
5. **Review reports regularly** - Check HTML reports for patterns of issues

## Resources

- [Checkstyle Documentation](https://checkstyle.org/)
- [PMD Documentation](https://pmd.github.io/)
- [Spotless Documentation](https://github.com/diffplug/spotless)
- [Prettier Documentation](https://prettier.io/)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

---

_Consistent code style makes collaboration easier and reduces cognitive load!_
