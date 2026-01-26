# Coroutines

To mock coroutines you need to have the kotlinx-coroutines-core dependency.
## Gradle

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:LATEST")
```

## Maven

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>LATEST</version>
    <scope>test</scope>
</dependency>
```

## Usage

Then you can use `coEvery`, `coVerify`, `coMatch`, `coAssert`, `coRun`, `coAnswers` or `coInvoke` to mock suspend functions.

```kotlin
val car = mockk<Car>()

coEvery { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

coVerify { car.drive(Direction.NORTH) }
```

And to simulate a never returning `suspend` function, you can use `coJustAwait`:

```kotlin
runTest {
    val car = mockk<Car>()

    coJustAwait { car.drive(any()) } // car.drive(...) will never return

    val job = launch(UnconfinedTestDispatcher()) {
        car.drive(Direction.NORTH)
    }

    coVerify { car.drive(Direction.NORTH) }

    job.cancelAndJoin() // Don't forget to cancel the job
}
```
::: warning
There is a known issue if using a spy with a suspending function: https://github.com/mockk/mockk/issues/554
:::
