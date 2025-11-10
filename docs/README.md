# Project Documentation

This directory contains technical decisions, development guides, user documentation, and architectural information for the Sword Combat Plugin.

## Documentation Structure

- `user-guide/` - **Server administrator and player documentation**
  - [Command Reference](user-guide/commands.md) - Available commands and usage
  - [Configuration Guide](user-guide/configuration.md) - How to configure `config.yaml`
- `setup/` - **Development environment setup guides**
  - [Development Environment](setup/development-environment.md) - IDE and Java setup
  - [Automation Tools](setup/automation-tools.md) - Spotless, Checkstyle, CI/CD
  - [GitHub Actions](setup/github-actions-guide.md) - CI/CD workflows
- `decisions/` - Architecture Decision Records (ADRs) documenting significant technical decisions
- `standards/` - Coding and documentation standards

## Quick Links

### For Server Administrators

- [Command Reference](user-guide/commands.md) - Learn how to use `/sword` commands
- [Configuration Guide](user-guide/configuration.md) - Tune gameplay with `config.yaml`
- [Hot Reloading](user-guide/commands.md#sword-reload) - Update config without restart

### For Developers

- [Contributing Guide](../CONTRIBUTING.md) - How to contribute to the project
- [Architecture Decisions](decisions/) - Technical decision records
- [Setup Guides](setup/) - Development environment configuration
- [Configuration Lifecycle](setup/configuration-lifecycle.md) - How config files work from dev to runtime

## Documentation Standards

This project follows:

- **Code Documentation**: Microsoft Java Documentation Standards
- **Javadoc**: All public APIs must have Javadoc comments
- **Decision Records**: Use ADR format for technical decisions
- **User Documentation**: Clear, concise guides for server administrators

## For Contributors

See [CONTRIBUTING.md](../CONTRIBUTING.md) for contribution guidelines.
