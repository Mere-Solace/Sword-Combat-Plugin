# Testing Guide

This document describes the testing strategy and practices for the Sword Combat Plugin.

## Table of Contents
- [Overview](#overview)
- [Testing Framework](#testing-framework)
- [Running Tests](#running-tests)
- [Writing Tests](#writing-tests)
- [Test Organization](#test-organization)
- [Testing Patterns](#testing-patterns)
- [Known Limitations](#known-limitations)

## Overview

The Sword Combat Plugin uses **JUnit 5** for unit testing and **MockBukkit** for mocking Minecraft/Paper APIs. Testing is essential for:
- Preventing regressions when refactoring
- Documenting expected behavior
- Validating combat calculations
- Ensuring plugin reliability

### Current Coverage
As of the initial testing setup, we have:
- ✅ **VectorUtil** - 26/27 tests passing (geometric calculations)
- ✅ **BezierUtil** - 12/12 tests passing (curve generation)

## Testing Framework

### Dependencies
```groovy
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.1'
    testImplementation 'org.mockito:mockito-core:5.7.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.7.0'
    testImplementation 'com.github.seeseemelk:MockBukkit-v1.20:3.9.0'
    testImplementation("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
}
```

### Test Structure
```
src/
├── main/
│   └── java/
│       └── btm/sword/
│           ├── util/
│           │   ├── VectorUtil.java
│           │   └── BezierUtil.java
│           └── ...
└── test/
    └── java/
        └── btm/sword/
            └── util/
                ├── VectorUtilTest.java
                └── BezierUtilTest.java
```

## Running Tests

### Run All Tests
```bash
./gradlew test
```

### Run Specific Test Class
```bash
./gradlew test --tests btm.sword.util.VectorUtilTest
```

### Run Specific Test Method
```bash
./gradlew test --tests btm.sword.util.VectorUtilTest.testGetPitchHorizontal
```

### View Test Reports
After running tests, open the HTML report at:
```
build/reports/tests/test/index.html
```

### Test Output
The test task is configured to show:
- ✅ Passed tests
- ⏭️ Skipped tests
- ❌ Failed tests
- Full stack traces for failures

## Writing Tests

### Test Class Template
```java
package btm.sword.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link YourClass}.
 */
class YourClassTest {

    private static final double EPSILON = 0.0001; // For floating-point comparisons

    @Test
    @DisplayName("Should describe what this test validates")
    void testSomeBehavior() {
        // Given - Setup test data
        int input = 5;

        // When - Execute the code under test
        int result = YourClass.someMethod(input);

        // Then - Verify the outcome
        assertEquals(10, result);
    }
}
```

### Naming Conventions

**Test Classes**: `<ClassName>Test.java`
- Example: `VectorUtil.java` → `VectorUtilTest.java`

**Test Methods**: `test<WhatIsBeingTested>()`
- Example: `testGetPitchHorizontal()`
- Use `@DisplayName` for readable descriptions

### Assertions

```java
// Equality
assertEquals(expected, actual);
assertEquals(expected, actual, delta); // For floating-point

// Boolean
assertTrue(condition);
assertFalse(condition);

// Null checks
assertNull(object);
assertNotNull(object);

// Exceptions
assertThrows(IllegalArgumentException.class, () -> {
    someMethod(invalidInput);
});
```

### Floating-Point Comparisons
Always use epsilon for floating-point comparisons:

```java
private static final double EPSILON = 0.0001;

@Test
void testVectorLength() {
    Vector v = new Vector(3, 4, 0);
    assertEquals(5.0, v.length(), EPSILON);
}
```

## Test Organization

### Test Categories

**1. Pure Utility Tests** (No Minecraft dependencies)
- Math utilities (VectorUtil, BezierUtil)
- Data structures
- Algorithms

**2. Mock-Based Tests** (Uses MockBukkit)
- Combat calculations
- Player state management
- Entity interactions

**3. Integration Tests** (Future)
- Full plugin behavior
- Command execution
- Event handling

### Test Data Organization

For complex test data, consider using:
- **@BeforeEach** - Setup method that runs before each test
- **@AfterEach** - Cleanup method that runs after each test
- **Test fixtures** - Helper methods to create test objects

```java
class CombatTest {
    private Combatant testCombatant;

    @BeforeEach
    void setUp() {
        testCombatant = createTestCombatant();
    }

    @Test
    void testAttack() {
        // testCombatant is ready to use
    }

    private Combatant createTestCombatant() {
        // Helper method to create test data
        return new Combatant(/* ... */);
    }
}
```

## Testing Patterns

### Pattern 1: Testing Pure Functions

Pure functions (no side effects) are the easiest to test:

```java
@Test
@DisplayName("getPitch should return 0 for horizontal vector")
void testGetPitchHorizontal() {
    Vector v = new Vector(1, 0, 1);
    double pitch = VectorUtil.getPitch(v);
    assertEquals(0, pitch, EPSILON);
}
```

### Pattern 2: Testing with Mocks

For code that depends on Bukkit APIs, use mocks:

```java
@Test
void testWithMockedLocation() {
    World mockWorld = Mockito.mock(World.class);
    Location loc = new Location(mockWorld, 0, 0, 0, 0, 0);

    // Test code using location
}
```

### Pattern 3: Testing Edge Cases

Always test boundary conditions:

```java
@Test
void testEmptyList() {
    List<Vector> empty = Collections.emptyList();
    assertThrows(IllegalArgumentException.class, () -> {
        processVectors(empty);
    });
}
```

### Pattern 4: Parameterized Tests

For testing multiple inputs:

```java
@ParameterizedTest
@ValueSource(doubles = {0.0, 0.25, 0.5, 0.75, 1.0})
@DisplayName("Bezier curve should work for all t values")
void testBezierAtMultipleT(double t) {
    Function<Double, Vector> curve = BezierUtil.cubicBezier3d(start, end, c1, c2);
    Vector result = curve.apply(t);
    assertNotNull(result);
}
```

## Known Limitations

### 1. VectorUtil.getBasis() Test Failure

**Issue**: The `testGetBasisStraightUp()` test fails because `VectorUtil.getBasis()` calls `Cache.testObsidianTearParticle.display()`, which requires a running Bukkit server.

**Why It Fails**:
```java
// In VectorUtil.java line 20
Cache.testObsidianTearParticle.display(origin.clone().add(ref));
```

This is a debug/visualization call embedded in a utility method, causing:
- `NullPointerException: Cannot invoke "org.bukkit.Server.createBlockData(...)"`

**Workaround**: This test is currently expected to fail. It documents a code smell that should be addressed in the architectural refactor (see Issue #42).

**Proper Fix** (Future): Remove side effects from VectorUtil:
```java
// Instead of displaying particles in the utility method,
// return the debug info and let the caller decide to display it
public static BasisResult getBasis(Location origin, Vector dir) {
    // ... calculation ...
    return new BasisResult(basis, debugPoint); // No display() call
}
```

### 2. MockBukkit Limitations

MockBukkit doesn't support all Paper/Spigot features. Some advanced features may require:
- Partial mocking
- Custom mock implementations
- Integration tests on a real test server

## Best Practices

### ✅ DO:
- Write tests for all new pure utility methods
- Test edge cases and boundary conditions
- Use descriptive test names and `@DisplayName`
- Keep tests focused on one behavior
- Use epsilon for floating-point comparisons
- Mock external dependencies (Bukkit APIs)

### ❌ DON'T:
- Test private methods directly (test through public API)
- Write tests that depend on execution order
- Hardcode magic numbers without explanation
- Ignore failing tests (fix or document them)
- Skip testing error cases

## Resources

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [MockBukkit Documentation](https://github.com/MockBukkit/MockBukkit)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Effective Unit Testing (Book)](https://www.manning.com/books/effective-unit-testing)

## Contributing Tests

When contributing tests:
1. Follow the existing patterns in `VectorUtilTest` and `BezierUtilTest`
2. Ensure tests are deterministic (no randomness, unless testing random behavior)
3. Include both positive and negative test cases
4. Document any known failures or limitations
5. Run `./gradlew test` before committing

## Future Work

- [ ] Add tests for combat calculation methods
- [ ] Mock complex Bukkit interactions (Entity, Player)
- [ ] Set up integration testing framework
- [ ] Configure CI/CD to run tests automatically (Issue #27)
- [ ] Increase coverage to 80%+ for utility classes
- [ ] Address code smells revealed by testing (side effects in utilities)

---

*"Tests are the programmer's safety net"* - Let's keep our code reliable and maintainable!
