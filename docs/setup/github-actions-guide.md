# GitHub Actions CI/CD Guide

The project automatically runs code quality checks on every push and pull request using GitHub Actions.

## What Gets Checked Automatically

### Triggers

The workflow runs on:

- **Push to branches**: main, dev, gig-dev
- **Pull requests** targeting: main, dev
- **Manual trigger**: Via Actions tab on GitHub

### Checks Performed

1. **Build verification** - Ensures code compiles
2. **Checkstyle analysis** - Detects code quality issues
3. **Spotless formatting** - Verifies code formatting

## Viewing Results

### On GitHub

**Step 1: Navigate to Actions**

1. Go to repository on GitHub
2. Click "Actions" tab
3. See list of workflow runs

**Step 2: View Workflow Run**

1. Click on a workflow run (e.g., "Code Quality Checks")
2. See status: Success, Failure, or In Progress
3. Click "build-and-check" job to see details

**Step 3: View Step Details**
Each step shows:

- Build with Gradle
- Run Checkstyle
- Check code formatting with Spotless
- Upload Checkstyle report

**Step 4: Download Reports**

1. Scroll to "Artifacts" section
2. Download "checkstyle-report"
3. Unzip and open `main.html` in browser

### On Pull Requests

**Automatic Status Checks**

- Green checkmark: All checks passed
- Red X: Checks failed
- Yellow dot: Checks running

**Automatic Comments**
If formatting issues detected, bot posts:

```
Code formatting issues detected!

Run `./gradlew spotlessApply` to auto-fix formatting issues.
```

## Workflow File Location

`.github/workflows/code-quality.yml`

## How It Works

### Workflow Steps

**1. Checkout Code**

```yaml
- name: Checkout code
  uses: actions/checkout@v4
```

Clones the repository

**2. Setup Java 21**

```yaml
- name: Set up JDK 21
  uses: actions/setup-java@v4
  with:
    java-version: '21'
    distribution: 'microsoft'
    cache: 'gradle'
```

Installs Java and caches Gradle dependencies for speed

**3. Build Project**

```yaml
- name: Build with Gradle
  run: ./gradlew build --no-daemon
```

Compiles code and runs tests

**4. Run Checkstyle**

```yaml
- name: Run Checkstyle
  run: ./gradlew checkstyleMain --no-daemon
  continue-on-error: true
```

Analyzes code quality (warnings only, doesn't fail)

**5. Check Formatting**

```yaml
- name: Check code formatting with Spotless
  run: ./gradlew spotlessCheck --no-daemon
  continue-on-error: true
```

Verifies code formatting (warnings only)

**6. Upload Reports**

```yaml
- name: Upload Checkstyle report
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: checkstyle-report
    path: build/reports/checkstyle/
```

Makes reports downloadable

**7. Comment on PR**

```yaml
- name: Comment PR with Spotless suggestions
  if: failure() && github.event_name == 'pull_request'
  uses: actions/github-script@v7
```

Auto-comments on PRs with issues

## Configuration

### Non-Blocking Checks

All quality checks use `continue-on-error: true` meaning:

- Build must pass (enforced)
- Checkstyle warnings don't fail build
- Spotless warnings don't fail build

This allows development to continue while improving quality incrementally.

### Changing to Blocking

To make checks mandatory, remove `continue-on-error: true` lines.

## Manual Workflow Dispatch

Run workflow manually:

1. Go to Actions tab
2. Click "Code Quality Checks"
3. Click "Run workflow" button
4. Select branch
5. Click "Run workflow"

Useful for:

- Testing workflow changes
- Re-running failed checks
- Running on branches without pushing

## Workflow Status Badge

Add to README.md:

```markdown
![Code Quality](https://github.com/Mere-Solace/Sword-Combat-Plugin/actions/workflows/code-quality.yml/badge.svg)
```

Shows real-time status of latest workflow run.

## Troubleshooting

### Workflow Not Running

**Check:**

1. Workflow file exists: `.github/workflows/code-quality.yml`
2. Branch is pushed to GitHub
3. Repository has Actions enabled (Settings -> Actions)

### Build Fails

**Common causes:**

- Compilation errors
- Missing dependencies
- Java version mismatch

**Fix:**

1. Run `./gradlew build` locally
2. Fix errors shown
3. Push fix

### Checkstyle Fails

**View report:**

1. Download checkstyle-report artifact
2. Open `main.html`
3. See specific violations

**Fix:**
Most issues auto-fixable with `./gradlew spotlessApply`

### Spotless Fails

**Fix automatically:**

```bash
./gradlew spotlessApply
git add -u
git commit -m "Fix code formatting"
git push
```

## Permissions Required

Workflow needs:

- Read access to repository
- Write access for PR comments
- Artifact upload permissions

Already configured if repository has Actions enabled.

## Cost and Limits

**GitHub Free Tier:**

- 2,000 minutes/month for private repos
- Unlimited for public repos
- This workflow uses ~2-3 minutes per run

**Storage:**

- Artifacts stored for 90 days
- Checkstyle reports ~1MB each
- Well within free tier limits

## Local Testing

Test workflow steps locally before pushing:

```bash
# Step 3: Build
./gradlew build --no-daemon

# Step 4: Checkstyle
./gradlew checkstyleMain --no-daemon

# Step 5: Spotless
./gradlew spotlessCheck --no-daemon
```

## Advanced Features

### Matrix Builds

Test multiple Java versions:

```yaml
strategy:
  matrix:
    java: [17, 21]
```

### Caching

Already enabled for Gradle dependencies:

```yaml
cache: 'gradle'
```

Speeds up builds by 30-50%.

### Scheduled Runs

Run nightly:

```yaml
on:
  schedule:
    - cron: '0 0 * * *'
```

## Related Documentation

- [docs/setup/automation-tools.md](automation-tools.md) - Using tools locally
- [CONTRIBUTING.md](../../CONTRIBUTING.md) - Contribution workflow
- Issue #27 - GitHub Actions CI Pipeline
- Issue #21 - Automated Linting

## Example Workflow Run

**Successful Run:**

```
✓ Checkout code (2s)
✓ Set up JDK 21 (15s)
✓ Build with Gradle (45s)
⚠ Run Checkstyle (5s) - 3 warnings
⚠ Check code formatting (3s) - All clean
✓ Upload Checkstyle report (2s)
```

**Failed Run with Auto-fix:**

```
✓ Checkout code (2s)
✓ Set up JDK 21 (15s)
✓ Build with Gradle (45s)
⚠ Run Checkstyle (5s) - 3 warnings
✗ Check code formatting (3s) - 5 files need formatting
✓ Upload Checkstyle report (2s)
💬 Bot comments: "Run ./gradlew spotlessApply"
```

---

Last Updated: 2025-10-30
Workflow Status: Active and Running
