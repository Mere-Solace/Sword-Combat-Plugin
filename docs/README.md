# Project Documentation

This directory contains technical decisions, development guides, and architectural documentation for
the Sword Combat Plugin.

## Documentation Structure

- `decisions/` - Architecture Decision Records (ADRs) documenting significant technical decisions
- `setup/` - Development environment setup guides and automation tools
- `standards/` - Coding and documentation standards
- `reports/` - Generated reports (Javadoc coverage, code quality metrics)

## Documentation Standards

This project follows:

- **Code Documentation**: Microsoft Java Documentation Standards
- **Javadoc**: All public APIs must have Javadoc comments
- **Decision Records**: Use ADR format for technical decisions

## Key Documentation Files

### Setup Guides

- [automation-tools.md](setup/automation-tools.md) - Spotless, Checkstyle, PMD, Javadoc coverage
- [github-actions-guide.md](setup/github-actions-guide.md) - CI/CD workflow documentation
- [pmd-guide.md](setup/pmd-guide.md) - PMD static analysis usage
- [javadoc-coverage-guide.md](setup/javadoc-coverage-guide.md) - Documentation coverage tracking
- [devops-roadmap.md](setup/devops-roadmap.md) - DevOps strategy and improvements

### Standards

- [documentation-standards.md](standards/documentation-standards.md) - Javadoc guidelines

### Decisions

- [001-gradle-wrapper.md](decisions/001-gradle-wrapper.md) - Gradle wrapper rationale

## For Contributors

See [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.
