# First Mock

Simplest example. By default, mocks are strict, so you need to provide some behavior.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

What happens in this example:
- `mockk<Car>()` creates a strict mock with no default behavior.
- `every { ... } returns ...` defines how the mock should respond.
- `verify { ... }` asserts that a call occurred.
- `confirmVerified(car)` ensures no unverified calls remain.

See [Core Mocking](../features/core-mocking) for more detailed examples.
