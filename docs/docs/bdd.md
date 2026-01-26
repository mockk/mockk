# Behavior-Driven Development

For teams using Behavior-Driven Development, MockK provides BDD-style aliases

```kotlin
testImplementation("io.mockk:mockk:LATEST")
testImplementation("io.mockk:mockk-bdd:LATEST")
```

```kotlin
androidTestImplementation("io.mockk:mockk-android:LATEST")
androidTestImplementation("io.mockk:mockk-bdd-android:LATEST")
```

### BDD aliases

| Standard MockK     | BDD style         |
|--------------------|-------------------|
| `every { ... }`    | `given { ... }`   |
| `coEvery { ... }`  | `coGiven { ... }` |
| `verify { ... }`   | `then { ... }`    |
| `coVerify { ... }` | `coThen { ... }`  |
