# Automation Tools Guide

This project uses automated tools to maintain code quality without manual effort.

## Tools Overview

### 1. Spotless - Auto-formatting
Automatically fixes code formatting issues including:
- Removes unused imports
- Organizes import statements
- Converts tabs to spaces (4 spaces)
- Trims trailing whitespace
- Ensures files end with newline

### 2. Checkstyle - Code quality analysis
Detects code quality issues including:
- Unused imports
- Empty catch blocks
- Missing @Override annotations
- Naming convention violations
- Line length issues
- Missing Javadoc (info level)

### 3. GitHub Actions - CI/CD
Automatically runs checks on:
- Every push to main, dev, gig-dev
- Every pull request
- Manual workflow dispatch

## Using the Tools

### Auto-fix Code Formatting

**Fix all formatting issues automatically:**
```bash
./gradlew spotlessApply
```

This command will:
- Remove all unused imports
- Fix indentation
- Organize imports
- Clean up whitespace

**Check formatting without fixing:**
```bash
./gradlew spotlessCheck
```

### Run Code Quality Checks

**Run Checkstyle analysis:**
```bash
./gradlew checkstyleMain
```

View report: `build/reports/checkstyle/main.html`

### Run All Checks

**Build + format check + quality check:**
```bash
./gradlew build spotlessCheck checkstyleMain
```

## Workflow Integration

### Before Committing
```bash
# Auto-fix formatting
./gradlew spotlessApply

# Run quality checks
./gradlew checkstyleMain

# Build to verify
./gradlew build
```

### Pre-commit Hook (Optional)
Create `.git/hooks/pre-commit`:
```bash
#!/bin/sh
./gradlew spotlessApply --quiet
git add -u
```

Make executable: `chmod +x .git/hooks/pre-commit`

## IDE Integration

### IntelliJ IDEA

**Use project Checkstyle config:**
1. Settings -> Tools -> Checkstyle
2. Add Configuration File: `config/checkstyle/checkstyle.xml`
3. Set as active

**Auto-format on save:**
1. Settings -> Tools -> Actions on Save
2. Enable: Reformat code
3. Enable: Optimize imports

### VS Code

**Java extension automatically uses Checkstyle config**

**Format on save:**
```json
{
    "editor.formatOnSave": true,
    "java.format.settings.url": "config/checkstyle/checkstyle.xml"
}
```

## GitHub Actions

### Automatic Checks

Every push and PR triggers:
1. Build verification
2. Spotless format check
3. Checkstyle quality check

### PR Comments

If formatting issues detected, bot comments:
> Code formatting issues detected!
> Run `./gradlew spotlessApply` to auto-fix formatting issues.

### View Reports

1. Go to Actions tab on GitHub
2. Click on workflow run
3. Download "checkstyle-report" artifact
4. Open `main.html` to view issues

## Configuration

### Checkstyle Rules

Edit: `config/checkstyle/checkstyle.xml`

Current configuration:
- Severity: warning (doesn't fail builds)
- Unused imports: error
- Line length: 120 characters
- Javadoc: info level only

### Spotless Rules

Edit: `build.gradle` spotless block

Current configuration:
- Remove unused imports: enabled
- Import ordering: enabled
- Indentation: 4 spaces
- Trim trailing whitespace: enabled

## Results from Initial Run

**Files automatically fixed: 56**

Common fixes applied:
- Tabs -> 4 spaces
- Removed unused imports
- Organized import statements
- Fixed whitespace

**Example (PlayerListener.java):**
- Removed 7 unused imports
- Fixed indentation
- Organized remaining imports

## Troubleshooting

### Spotless fails to apply

**Issue:** Encoding errors
**Fix:** Ensure UTF-8 encoding

### Checkstyle reports too many issues

**Adjust severity in** `config/checkstyle/checkstyle.xml`:
```xml
<property name="severity" value="info"/>
```

### Build fails due to formatting

**Quick fix:**
```bash
./gradlew spotlessApply
```

## Benefits

1. **No Manual Work** - Run one command to fix issues
2. **Consistent Style** - All code follows same standards
3. **Catch Issues Early** - CI catches problems before merge
4. **Educational** - Learn best practices from automated feedback
5. **Time Saving** - No arguments about style in code review

## Related Issues

- Issue #21 - Automated Linting Setup
- Issue #22 - Remove Unused Imports (auto-fixed by Spotless)
- Issue #27 - GitHub Actions CI Pipeline
- Issue #29 - Checkstyle Configuration

---

Last Updated: 2025-10-30
