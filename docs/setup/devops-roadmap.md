# DevOps Roadmap

This document outlines the infrastructure and tooling improvements being implemented to support the
project's growth.

## Philosophy

**The owner focuses on core features. DevOps handles the infrastructure.**

## Current Status

### ✅ Completed

- [x] Documentation structure (`docs/` folder)
- [x] Contribution guidelines (`CONTRIBUTING.md`)
- [x] Documentation standards (Microsoft Java + Javadoc)
- [x] Architecture Decision Records (ADR) format
- [x] GitHub labels for issue management
- [x] Development branch (`gig-dev`)

### 🔄 In Review (GitHub Issues Created)

- [ ] [#19](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/19) - Documentation structure
      review
- [ ] [#20](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/20) - Gradle wrapper approval
- [ ] [#21](https://github.com/Mere-Solace/Sword-Combat-Plugin/issues/21) - Automated linting setup

### 📋 Planned

- [ ] GitHub Actions CI/CD pipeline
  - Build verification
  - Test execution
  - Javadoc generation
- [ ] Checkstyle integration (warning mode)
- [ ] Documentation coverage reports
- [ ] Automated PR checks
- [ ] Javadoc improvement tracking

## Strategy

### Phase 1: Foundation (Current)

**Goal**: Establish documentation standards and get buy-in

- Create docs structure
- Get owner approval on standards
- Set expectations for future work

### Phase 2: Automation (Next)

**Goal**: Automate quality checks without blocking development

- Add linting (warnings only)
- Set up GitHub Actions
- Create baseline reports

### Phase 3: Incremental Improvement

**Goal**: Gradually improve code quality over time

- Track documentation coverage
- Add Javadocs to new/modified code
- Refactor as opportunities arise

### Phase 4: Education

**Goal**: Help owner learn best practices through automated feedback

- Provide helpful lint messages
- Link to standards in PR comments
- Share resources and examples

## Roles

### Owner (Mere Solace)

**Focus**: Core feature development

- Review DevOps proposals when convenient
- Provide feedback on tooling
- Learn from automated checks
- Stay focused on the fun stuff!

### DevOps (Chris R.)

**Focus**: Infrastructure, tooling, documentation

- Set up CI/CD pipelines
- Configure linting and quality tools
- Improve existing documentation
- Handle the boring but important stuff

## Principles

1. **Non-Blocking**: Quality tools should warn, not fail (initially)
2. **Educational**: Feedback should teach, not criticize
3. **Incremental**: Improve gradually, don't demand perfection
4. **Practical**: Focus on high-value improvements
5. **Transparent**: Document decisions and rationale

## Current Codebase Stats

- **58 Java files**
- **Target**: Java 21
- **Minecraft Version**: 1.21 (Paper)
- **Build System**: Gradle 8.8

## Key Resources

- [CONTRIBUTING.md](../../CONTRIBUTING.md) - How to contribute
- [docs/standards/](../standards/) - Code and documentation standards
- [docs/decisions/](../decisions/) - Architecture decisions

## Labels for Issues

- `devops` - Infrastructure and tooling
- `automation` - CI/CD and automated checks
- `documentation` - Docs improvements
- `review-needed` - Requires owner review
- `low-priority` - Can wait until convenient

## Next Steps for DevOps

1. **Wait for owner approval** on GitHub issues #19, #20, #21
2. **While waiting**, prepare:
   - Checkstyle configuration
   - GitHub Actions workflow templates
   - Javadoc coverage scripts
3. **After approval**, implement incrementally
4. **Monitor and adjust** based on feedback

---

_Last Updated: 2025-10-30_
