# Javadoc Coverage Tool Guide

This tool measures and tracks Javadoc documentation coverage across the codebase.

## What It Measures

- **Class Documentation**: Percentage of classes with Javadoc comments
- **Method Documentation**: Percentage of public methods with Javadoc comments
- **Coverage Per File**: Documentation coverage for each Java file
- **Progress Tracking**: Compare reports over time to see improvement

## Running the Tool

```bash
./gradlew javadocCoverage
```

**Output:**
- Console summary showing overall statistics
- Detailed report saved to `docs/reports/javadoc-coverage-baseline.md`

## Understanding the Report

### Summary Section
Shows overall statistics:
```
Total Classes: 53
Documented Classes: 15 (28.3%)
Total Public Methods: 232
Documented Methods: 61 (26.3%)
```

### Classes Needing Documentation
Lists files sorted by priority (worst coverage first):
- Files with most methods and 0% coverage are highest priority
- Shows which classes lack class-level Javadoc (✗ in Class Doc column)

### Well-Documented Classes
Lists files with 100% coverage as examples of good documentation.

## How to Use This Tool

### 1. Establish Baseline
Run once to create initial report:
```bash
./gradlew javadocCoverage
```

Commit the baseline report to track starting point.

### 2. Pick High-Priority Files
From the report, choose files to document based on:
- **High method count** - More impact
- **Missing class Javadoc** - Quick wins
- **Core functionality** - Public APIs first

### 3. Add Javadocs
Follow [docs/standards/documentation-standards.md](../standards/documentation-standards.md)

**Minimum requirements:**
- Class-level Javadoc explaining purpose
- Method Javadoc for all public methods
- @param tags for all parameters
- @return tags for non-void methods

### 4. Track Progress
Re-run after adding documentation:
```bash
./gradlew javadocCoverage
```

Watch the percentages improve!

## Current Baseline (2025-10-30)

**Starting Point:**
- Class Documentation: 28.3% (15/53 classes)
- Method Documentation: 26.3% (61/232 methods)

**Target Goals:**
- **Short term** (1 month): 50% class coverage, 40% method coverage
- **Medium term** (3 months): 75% class coverage, 60% method coverage
- **Long term** (6 months): 90% class coverage, 80% method coverage

## Interpretation Guide

### Coverage Levels

| Percentage | Status | Action |
|------------|--------|--------|
| 0-25% | Poor | Urgent: Document public APIs immediately |
| 26-50% | Fair | Focus: Add class-level docs, document key methods |
| 51-75% | Good | Polish: Fill gaps, improve existing docs |
| 76-100% | Excellent | Maintain: Keep documentation up-to-date |

### Priority Matrix

Focus on files that are:
1. **High methods + Low coverage** = Highest priority
2. **No class doc** = Quick win
3. **Core APIs** (listeners, actions, entities) = Public-facing

## Integration with Other Tools

### Checkstyle
Checkstyle also checks for missing Javadocs (info level):
```bash
./gradlew checkstyleMain
```

View report: `build/reports/checkstyle/main.html`

### Workflow
1. Run `javadocCoverage` to identify gaps
2. Add Javadocs following standards
3. Run `checkstyleMain` to verify format
4. Run `javadocCoverage` again to see improvement

## Automation (Future)

Potential enhancements:
- Add to GitHub Actions workflow
- Post coverage as PR comment
- Fail build if coverage drops
- Generate historical trend chart

## Example: Improving a File

**Before (0% coverage):**
```java
public class ExampleClass {
    public void doSomething(String input) {
        // implementation
    }
}
```

**After (100% coverage):**
```java
/**
 * Manages example functionality for the plugin.
 * <p>
 * This class demonstrates proper Javadoc documentation.
 */
public class ExampleClass {
    /**
     * Processes the given input string.
     *
     * @param input the string to process
     */
    public void doSomething(String input) {
        // implementation
    }
}
```

**Result:**
- Run `./gradlew javadocCoverage`
- Coverage for ExampleClass: 0% → 100%
- Overall method coverage: 26.3% → 26.7%

## Tips for Success

1. **Start small**: Pick 1-2 files per session
2. **Document as you code**: Add Javadocs when writing new methods
3. **Use templates**: Copy good examples from well-documented classes
4. **Be concise**: Explain **why**, not **what** (code shows what)
5. **Track progress**: Re-run the tool to stay motivated

## Troubleshooting

### Task not found
Ensure you're in the project root directory and build.gradle has the task.

### Report not generated
Check console output for errors. Ensure `docs/reports/` is writable.

### Coverage seems wrong
The tool uses regex to detect Javadoc (`/** */`). Ensure proper formatting:
- Must use `/**` (not `/*`)
- Must be immediately before class/method declaration

## Related Documentation

- [documentation-standards.md](../standards/documentation-standards.md) - Javadoc guidelines
- [automation-tools.md](automation-tools.md) - All code quality tools
- Issue #26 - Javadoc coverage tracking
- Issue #25 - Adding missing Javadocs

---

Last Updated: 2025-10-30
Current Coverage: 28.3% classes, 26.3% methods
