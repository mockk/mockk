# Verification

## Verification atLeast, atMost or exactly times

You can check the call count with the `atLeast`, `atMost` or `exactly` parameters:

```kotlin

val car = mockk<Car>(relaxed = true)

car.accelerate(fromSpeed = 10, toSpeed = 20)
car.accelerate(fromSpeed = 10, toSpeed = 30)
car.accelerate(fromSpeed = 20, toSpeed = 30)

// all pass
verify(atLeast = 3) { car.accelerate(allAny()) }
verify(atMost  = 2) { car.accelerate(fromSpeed = 10, toSpeed = or(20, 30)) }
verify(exactly = 1) { car.accelerate(fromSpeed = 10, toSpeed = 20) }
verify(exactly = 0) { car.accelerate(fromSpeed = 30, toSpeed = 10) } // means no calls were performed

confirmVerified(car)
```

Or you can use `verifyCount`:

```kotlin

val car = mockk<Car>(relaxed = true)

car.accelerate(fromSpeed = 10, toSpeed = 20)
car.accelerate(fromSpeed = 10, toSpeed = 30)
car.accelerate(fromSpeed = 20, toSpeed = 30)

// all pass
verifyCount {
    (3..5) * { car.accelerate(allAny(), allAny()) } // same as verify(atLeast = 3, atMost = 5) { car.accelerate(allAny(), allAny()) }
    1 * { car.accelerate(fromSpeed = 10, toSpeed = 20) } // same as verify(exactly = 1) { car.accelerate(fromSpeed = 10, toSpeed = 20) }
    0 * { car.accelerate(fromSpeed = 30, toSpeed = 10) } // same as verify(exactly = 0) { car.accelerate(fromSpeed = 30, toSpeed = 10) }
}

confirmVerified(car)
```

## Verification order

* `verifyAll` verifies that all calls happened without checking their order.
* `verifySequence` verifies that the calls happened in a specified sequence.
* `verifyOrder` verifies that calls happened in a specific order.
* `wasNot Called` verifies that the mock (or the list of mocks) was not called at all.

```kotlin
class MockedClass {
    fun sum(a: Int, b: Int) = a + b
}

val obj = mockk<MockedClass>()
val slot = slot<Int>()

every {
    obj.sum(any(), capture(slot))
} answers {
    1 + firstArg<Int>() + slot.captured
}

obj.sum(1, 2) // returns 4
obj.sum(1, 3) // returns 5
obj.sum(2, 2) // returns 5

verifyAll {
    obj.sum(1, 3)
    obj.sum(1, 2)
    obj.sum(2, 2)
}

verifySequence {
    obj.sum(1, 2)
    obj.sum(1, 3)
    obj.sum(2, 2)
}

verifyOrder {
    obj.sum(1, 2)
    obj.sum(2, 2)
}

val obj2 = mockk<MockedClass>()
val obj3 = mockk<MockedClass>()
verify {
    listOf(obj2, obj3) wasNot Called
}

confirmVerified(obj)
```

## Verification confirmation

To double-check that all calls were verified by `verify...` constructs, you can use `confirmVerified`:

```kotlin
confirmVerified(mock1, mock2)
```

Since v1.14.6 you can pass `clear = true` to also clear verification marks and recorded calls for the provided mocks after confirmation.

```kotlin
confirmVerified(mock1, mock2, clear = true)
```

It doesn't make much sense to use it for `verifySequence` and `verifyAll`, as these verification methods already exhaustively cover all calls with verification.

It will throw an exception if there are some calls left without verification.

Some calls can be excluded from this confirmation, check the next section for more details.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK
every { car.drive(Direction.SOUTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK
car.drive(Direction.SOUTH) // returns OK

verify {
    car.drive(Direction.SOUTH)
    car.drive(Direction.NORTH)
}

confirmVerified(car) // makes sure all calls were covered with verification
confirmVerified(car, clear = true) // makes sure all calls were covered with verification and clears verification marks and recorded calls
```

## Unnecessary stubbing

Because clean & maintainable test code requires zero unnecessary code, you can ensure that there is no unnecessary stubs.

```kotlin
checkUnnecessaryStub(mock1, mock2)
```

It will throw an exception if there are some declared calls on the mocks that are not used by the tested code.
This can happen if you have declared some really unnecessary stubs or if the tested code doesn't call an expected one.


## Recording exclusions

To exclude unimportant calls from being recorded, you can use `excludeRecords`:

```kotlin
excludeRecords { mock.operation(any(), 5) }
```

All matching calls will be excluded from recording. This may be useful if you are using exhaustive verification: `verifyAll`, `verifySequence` or `confirmVerified`.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK
every { car.drive(Direction.SOUTH) } returns Outcome.OK

excludeRecords { car.drive(Direction.SOUTH) }

car.drive(Direction.NORTH) // returns OK
car.drive(Direction.SOUTH) // returns OK

verify {
    car.drive(Direction.NORTH)
}

confirmVerified(car) // car.drive(Direction.SOUTH) was excluded, so confirmation is fine with only car.drive(Direction.NORTH)
```

## Verification timeout

To verify concurrent operations, you can use `timeout = xxx`:

```kotlin
mockk<MockCls> {
    every { sum(1, 2) } returns 4

    Thread {
        Thread.sleep(2000)
        sum(1, 2)
    }.start()

    verify(timeout = 3000) { sum(1, 2) }
}
```

This will wait until one of two following states: either verification is passed or the timeout is reached.
