# Contributing to Sword Combat Plugin

Thank you for your interest in contributing! This document provides guidelines for contributing to the project.

## Development Setup

### Prerequisites

- **Java 21** (OpenJDK recommended)
- **IntelliJ IDEA** (recommended) or any Java IDE
- **Git** for version control

### Getting Started

1. **Clone the repository**

   ```bash
   git clone git@github.com:Mere-Solace/Sword-Combat-Plugin.git
   cd Sword-Combat-Plugin
   ```

2. **Build the project**

   ```bash
   ./gradlew build
   ```

3. **Run test server**

   ```bash
   ./gradlew runServer
   ```

## Development Workflow

### Branching Strategy

- `main` - Stable production code
- `dev` - Active development branch
- Feature branches - Named descriptively (e.g., `feature/combat-system`, `fix/entity-tracking`)

### Before You Commit

1. **Build successfully**: `./gradlew build`
2. **Format code**: Follow Microsoft Java code standards
3. **Add Javadoc**: Document all public APIs
4. **Test your changes**: Verify in a test server

## Code Standards

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Naming conventions**:
  - Classes: `PascalCase`
  - Methods/Variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Packages: `lowercase`

### Configuration Development

When adding new configuration values, follow the **[Hybrid Configuration Pattern](docs/decisions/003-hybrid-configuration-pattern.md)**:

- **Simple configs (2-3 values)**: Use direct fields with `@Getter` (no nested classes)
- **Complex configs (5+ values)**: Use nested static classes when structure adds value
- **Fully flat configs**: Use for 30+ independent parameters without natural grouping

See [ADR 003](docs/decisions/003-hybrid-configuration-pattern.md) for detailed guidelines and examples.

### Documentation

- Follow [Microsoft Java Documentation Standards](docs/standards/documentation-standards.md)
- All public classes and methods require Javadoc
- Explain **why**, not **what** in comments

### Example

```java
/**
 * Manages sword combat mechanics for players.
 *
 * <p>This class handles attack actions, damage calculations, and combo tracking
 * for the custom sword combat system.</p>
 *
 * @author Your Name
 * @since 1.0
 */
public class SwordCombatManager {

    /**
     * Executes a sword attack from the player.
     *
     * @param player the player performing the attack
     * @param target the entity being attacked
     * @return true if the attack was successful, false otherwise
     */
    public boolean executeAttack(Player player, Entity target) {
        // Implementation
    }
}
```

## Commit Messages

Use clear, descriptive commit messages:

```bash
Add: New feature description
Fix: Bug description
Update: Enhancement description
Refactor: Code improvement description
Docs: Documentation changes
```

## Pull Requests

1. **Create a feature branch** from `dev`
2. **Make your changes** following code standards
3. **Write descriptive PR title** and description
4. **Reference issues** if applicable (e.g., "Fixes #123")
5. **Request review** from project maintainers

### PR Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Enhancement
- [ ] Documentation

## Testing
How were these changes tested?

## Checklist
- [ ] Code builds without errors
- [ ] Added/updated Javadoc
- [ ] Tested in game
- [ ] Updated relevant documentation
```

## Architecture Decisions

Significant technical decisions are documented in `docs/decisions/` as Architecture Decision Records (ADRs).

Before making major architectural changes:

1. Discuss with project maintainers
2. Document the decision in an ADR
3. Reference the ADR in related code

## Project Structure

```bash
Sword-Combat-Plugin/
├── docs/               # Project documentation
│   ├── decisions/      # Architecture Decision Records
│   ├── setup/          # Setup guides
│   └── standards/      # Code and doc standards
├── src/
│   └── main/
│       ├── java/       # Plugin source code
│       └── resources/  # Plugin resources
├── build.gradle        # Build configuration
└── CONTRIBUTING.md     # This file
```

## Getting Help

- **Questions?** Open a GitHub issue with the `question` label
- **Bug reports?** Open a GitHub issue with the `bug` label
- **Feature requests?** Open a GitHub issue with the `enhancement` label

## Code of Conduct

- Be respectful and constructive
- Help others learn and grow
- Focus on the code, not the person
- Welcome newcomers and junior developers

Thank you for contributing! 🎮⚔️
