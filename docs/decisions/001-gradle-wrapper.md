# ADR 001: Gradle Wrapper Files in Repository

**Status**: Proposed **Date**: 2025-10-30 **Authors**: Chris R., Mere Solace

## Context

The project uses Gradle as its build system. The Gradle wrapper consists of:

- `gradlew` (Unix shell script)
- `gradlew.bat` (Windows batch script)
- `gradle/wrapper/gradle-wrapper.jar` (wrapper executable JAR)
- `gradle/wrapper/gradle-wrapper.properties` (configuration)

We need to decide whether to commit these files to the repository.

## Decision

**We commit the Gradle wrapper files to the repository.**

## Reasoning

### Advantages

1. **Gradle Version Consistency**: Ensures all developers and CI/CD systems use the exact same
   Gradle version (8.8)
2. **Zero Dependencies**: New contributors can build the project immediately with `./gradlew build`
   without installing Gradle
3. **CI/CD Compatibility**: GitHub Actions and other CI systems can build without pre-installed
   Gradle
4. **Industry Standard**: This is the recommended practice by Gradle and most open-source Java
   projects
5. **Version Control**: Gradle version changes are tracked in git history

### Trade-offs

1. **Binary in Repository**: The `gradle-wrapper.jar` (~45KB) is a binary file in version control
2. **Repository Size**: Minimal impact (45KB is negligible for modern repositories)

### Alternatives Considered

- **Not committing the wrapper**: Would require all contributors to install Gradle manually,
  reducing accessibility

## Consequences

### Positive

- New contributors can clone and build immediately
- Consistent build environment across all machines
- Easier onboarding for junior developers

### Negative

- Binary file in git history (minimal impact)

## References

- [Gradle Wrapper Documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- Gradle official recommendation: "The wrapper should be committed to version control"
