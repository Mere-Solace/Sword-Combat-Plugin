# Development Environment Setup

This guide walks you through setting up your development environment for Sword: Combat Evolved.

## Prerequisites

### Required Software

| Software | Version | Purpose |
|----------|---------|---------|
| **Java** | 21+ | Development and runtime |
| **Git** | Latest | Version control |
| **IDE** | Any | Code editing (VS Code or IntelliJ recommended) |

### Recommended

- **VS Code** with Java extensions, or
- **IntelliJ IDEA** Community/Ultimate Edition

## Java Installation

### Option 1: Microsoft Build of OpenJDK (Recommended for Windows)

1. **Download** the installer:
   ```
   https://learn.microsoft.com/en-us/java/openjdk/download
   ```

2. **Install** via MSI installer or winget:
   ```cmd
   winget install Microsoft.OpenJDK.21
   ```

3. **Verify** installation:
   ```bash
   java -version
   # Should show: openjdk version "21.x.x"
   ```

### Option 2: Eclipse Temurin (Cross-platform)

1. **Download** from [Adoptium](https://adoptium.net/temurin/releases/?version=21)

2. **Install** for your platform

3. **Verify**:
   ```bash
   java -version
   ```

### Set JAVA_HOME

#### Windows (PowerShell):
```powershell
# User-level (recommended)
[Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-21.x.x.x-hotspot', 'User')

# Add to PATH
$path = [Environment]::GetEnvironmentVariable('Path', 'User')
[Environment]::SetEnvironmentVariable('Path', $path + ';' + $env:JAVA_HOME + '\bin', 'User')

# Verify (restart terminal first)
echo $env:JAVA_HOME
```

#### Linux/Mac:
```bash
# Add to ~/.bashrc or ~/.zshrc
export JAVA_HOME="/path/to/jdk-21"
export PATH="$JAVA_HOME/bin:$PATH"

# Apply changes
source ~/.bashrc

# Verify
echo $JAVA_HOME
```

## IDE Setup

### VS Code

1. **Install Extensions**:
   - Language Support for Java (Red Hat)
   - Debugger for Java
   - Test Runner for Java
   - Gradle for Java

2. **Configure Java Runtime**:
   - `Ctrl+Shift+P` → "Java: Configure Java Runtime"
   - Select Java 21 as the default runtime

3. **Workspace Settings** (auto-created):
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-21",
         "path": "/path/to/jdk-21",
         "default": true
       }
     ],
     "java.import.gradle.enabled": true
   }
   ```

### IntelliJ IDEA

1. **Set Project SDK**:
   - File → Project Structure → Project
   - SDK: Java 21
   - Language Level: 21

2. **Enable Gradle**:
   - Import as Gradle project when opening
   - Use Gradle wrapper (automatic)

3. **Enable Lombok**:
   - Settings → Plugins → Install "Lombok"
   - Settings → Build → Compiler → Annotation Processors → Enable annotation processing

4. **Configure Import Settings** (prevents star imports):
   - Settings → Editor → Code Style → Java → Imports tab
   - Set "Class count to use import with '*'" to **99**
   - Set "Names count to use static import with '*'" to **99**
   - Import order: `java`, `javax`, blank line, `org`, blank line, `com`, blank line, all other imports
   - **Note**: `.editorconfig` auto-configures this if EditorConfig plugin is enabled

## Project Setup

### Clone Repository

```bash
git clone https://github.com/Mere-Solace/Sword-Combat-Plugin.git
cd Sword-Combat-Plugin
```

### Build Project

```bash
# Make Gradle wrapper executable (Linux/Mac)
chmod +x gradlew

# Build project
./gradlew build

# Expected output:
# BUILD SUCCESSFUL
```

### Verify Setup

```bash
# 1. Check Java version
java -version
# Output: openjdk version "21.x.x"

# 2. Check Gradle detects Java
./gradlew --version
# Output: JVM: 21.x.x

# 3. Run clean build
./gradlew clean build
# Output: BUILD SUCCESSFUL
```

## Common Commands

### Build

```bash
# Clean build
./gradlew clean build

# Build without tests (faster)
./gradlew build -x test

# Build and copy to plugins folder
./gradlew build && cp build/libs/*.jar /path/to/server/plugins/
```

### Development Server

```bash
# Start Paper test server with plugin auto-loaded
./gradlew runServer

# Server files will be in: run/
# Plugin auto-configured with:
#   - EULA accepted
#   - Resource pack configured
#   - Hot-reload ready
```

### Code Quality

```bash
# Auto-fix formatting and imports
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Run code quality checks (includes star import detection)
./gradlew checkstyle

# Run all quality checks
./gradlew check
```

**Import Standards:**
- ✅ **Explicit imports only** - Never use star imports (`import java.util.*`)
- ✅ **Automatic enforcement** - Checkstyle fails build on star imports
- ✅ **Auto-fix available** - `./gradlew spotlessApply` removes unused imports
- ✅ **IDE alignment** - `.editorconfig` and `.vscode/settings.json` prevent star generation

**Why no star imports?**
- **Performance**: JVM loads only needed classes (optimization)
- **Clarity**: Explicitly shows all dependencies
- **Conflict prevention**: Avoids ambiguous class names
- **IDE support**: Better autocomplete and refactoring

### Clean

```bash
# Clean build artifacts
./gradlew clean

# Clean including Gradle cache (full reset)
./gradlew clean --refresh-dependencies
```

## Troubleshooting

### "JAVA_HOME is not set"

**Symptoms:**
```
ERROR: JAVA_HOME is not set and no 'java' command could be found
```

**Solutions:**

1. **Set for current session** (temporary):
   ```bash
   # Windows
   set JAVA_HOME=C:\Program Files\Microsoft\jdk-21.x.x.x-hotspot

   # Linux/Mac
   export JAVA_HOME="/path/to/jdk-21"
   ```

2. **Set permanently** (see [Set JAVA_HOME](#set-java_home) above)

3. **Restart terminal/IDE** after setting environment variables

### Gradle Build Fails

**Symptoms:**
```
Task :compileJava FAILED
```

**Checks:**

1. **Verify Java version**:
   ```bash
   java -version  # Must be 21+
   ./gradlew --version  # JVM should show 21.x.x
   ```

2. **Clean and rebuild**:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

3. **Check build.gradle**:
   - Source compatibility: Java 21
   - Target compatibility: Java 21

### VS Code Java Extension Not Working

**Symptoms:**
- No IntelliSense
- Import errors
- Red underlines everywhere

**Solutions:**

1. **Reload window**:
   - `Ctrl+Shift+P` → "Reload Window"

2. **Clean Java workspace**:
   - `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"
   - Reload window when prompted

3. **Verify Java runtime**:
   - `Ctrl+Shift+P` → "Java: Configure Java Runtime"
   - Ensure Java 21 is selected and recognized

4. **Reinstall extensions**:
   - Remove all Java extensions
   - Restart VS Code
   - Reinstall Language Support for Java

### Lombok Not Working

**Symptoms:**
- `@Getter`/`@Setter` annotations not recognized
- "Cannot find symbol" errors for generated methods

**Solutions:**

**VS Code:**
1. Install "Lombok Annotations Support" extension
2. Clean workspace: `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"

**IntelliJ:**
1. Install Lombok plugin: Settings → Plugins → "Lombok"
2. Enable annotation processing: Settings → Build → Compiler → Annotation Processors
3. Invalidate caches: File → Invalidate Caches / Restart

### Import Errors

**Symptoms:**
```
Package 'org.bukkit' does not exist
```

**Solutions:**

1. **Sync Gradle**:
   ```bash
   ./gradlew build --refresh-dependencies
   ```

2. **VS Code**: Reload window after Gradle sync

3. **IntelliJ**: Reload Gradle project
   - Gradle tab → Right-click → Reload Gradle Project

## Development Workflow

### Standard Workflow

1. **Create feature branch**:
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Make changes** and test locally:
   ```bash
   # Edit code
   vim src/main/java/...

   # Format code
   ./gradlew spotlessApply

   # Build and test
   ./gradlew clean build

   # Test in server
   ./gradlew runServer
   ```

3. **Commit changes**:
   ```bash
   git add .
   git commit -m "Add feature: description"
   ```

4. **Push and create PR**:
   ```bash
   git push origin feature/my-feature
   # Create Pull Request on GitHub
   ```

### Hot-Reload Development

For rapid iteration on config or code:

```bash
# Terminal 1: Run server
./gradlew runServer

# Terminal 2: Make changes
vim src/main/resources/config.yaml

# In-game: Reload config
/sword reload

# For code changes:
# 1. Stop server (Ctrl+C)
# 2. Rebuild: ./gradlew build
# 3. Restart server: ./gradlew runServer
```

## IDE-Specific Tips

### VS Code

**Keyboard Shortcuts:**
- `Ctrl+Shift+O`: Organize imports
- `Shift+Alt+F`: Format document
- `F2`: Rename symbol
- `Ctrl+.`: Quick fix

**Useful Commands:**
- `Ctrl+Shift+P` → "Java: Clean Java Language Server Workspace"
- `Ctrl+Shift+P` → "Java: Force Java Compilation"

### IntelliJ IDEA

**Keyboard Shortcuts:**
- `Ctrl+Alt+L`: Format code
- `Ctrl+Alt+O`: Optimize imports
- `Shift+F6`: Refactor/rename
- `Alt+Enter`: Show intention actions

**Useful Actions:**
- File → Invalidate Caches (fix weird errors)
- Analyze → Inspect Code (code quality check)
- Build → Rebuild Project (full rebuild)

## Next Steps

Once your environment is set up:

1. Read [Contributing Guidelines](../../CONTRIBUTING.md)
2. Check [Automation Tools Guide](automation-tools.md) for code quality tools
3. Review [Architecture Decisions](../decisions/) for technical context
4. Start with a [good first issue](https://github.com/Mere-Solace/Sword-Combat-Plugin/labels/good%20first%20issue)

## Resources

- [Microsoft OpenJDK Downloads](https://learn.microsoft.com/en-us/java/openjdk/download)
- [Eclipse Temurin (Adoptium)](https://adoptium.net/)
- [VS Code Java Documentation](https://code.visualstudio.com/docs/languages/java)
- [IntelliJ IDEA Documentation](https://www.jetbrains.com/idea/documentation/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
- [Paper API Documentation](https://docs.papermc.io/paper/dev)

## Getting Help

- **Build issues**: Check [Troubleshooting](#troubleshooting) section
- **Configuration**: See [Configuration Guide](../user-guide/configuration.md)
- **Commands**: See [Command Reference](../user-guide/commands.md)
- **Contributing**: See [CONTRIBUTING.md](../../CONTRIBUTING.md)
- **Questions**: Open an [issue](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues)