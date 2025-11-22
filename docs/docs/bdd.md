# Behavior-Driven Development

For teams using Behavior-Driven Development, MockK provides BDD-style aliases

```kotlin
testImplementation("io.mockk:mockk:${mockkVersion}")
testImplementation("io.mockk:mockk-bdd:${mockkVersion}")
```

```kotlin
androidTestImplementation("io.mockk:mockk-android:${mockkVersion}")
androidTestImplementation("io.mockk:mockk-bdd-android:${mockkVersion}")
```

### BDD aliases

| Standard MockK     | BDD style         |
|--------------------|-------------------|
| `every { ... }`    | `given { ... }`   |
| `coEvery { ... }`  | `coGiven { ... }` |
| `verify { ... }`   | `then { ... }`    |
| `coVerify { ... }` | `coThen { ... }`  |
