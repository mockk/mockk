# First Mock

This example shows how to mock a car. Because mocks are strict by default, the expected behavior must be defined upfront.

```kotlin
// create a strict mock with no default behavior
val car = mockk<Car>()

// define how the mock should respond
every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

// assert that a call occurred
verify { car.drive(Direction.NORTH) }

// ensure no unverified calls remain
confirmVerified(car)
```

See [Core Mocking](../features/core-mocking) for more detailed examples.
