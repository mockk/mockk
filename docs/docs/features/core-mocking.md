# Core Mocking

## Spy

Spies allow you to mix mocks and real objects.

```kotlin
val car = spyk(Car()) // or spyk<Car>() to call the default constructor

car.drive(Direction.NORTH) // returns whatever the real function of Car returns

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

Note 1: the spy object is a copy of the passed object.

Note 2: there is a known issue if using a spy with a suspending function: https://github.com/mockk/mockk/issues/554

## Relaxed mock

A `relaxed mock` is the mock that returns some simple value for all functions.
This allows you to skip specifying behavior for each case, while still stubbing things you need.
For reference types, chained mocks are returned.

```kotlin
val car = mockk<Car>(relaxed = true)

car.drive(Direction.NORTH) // returns null

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

::: info
Relaxed mocking is working badly with generic return types. A class cast exception is usually thrown in this case.
Opt for stubbing manually in the case of a generic return type.
:::

Workaround:

```kotlin
// in this case invoke function has generic return type
val func = mockk<() -> Car>(relaxed = true) 

// this line is workaround, without it the relaxed mock 
// would throw a class cast exception on the next line

every { func() } returns Car() 
// or you can return mockk() for example

func()
```

## Partial mocking

Sometimes, you need to stub some functions, but still call the real method on others, or on specific arguments.
This is possible by passing `callOriginal()` to `answers`, which works for both relaxed and non-relaxed mocks.

```kotlin
class Adder {
 fun addOne(num: Int) = num + 1
}

val adder = mockk<Adder>()

every { adder.addOne(any()) } returns -1
every { adder.addOne(3) } answers { callOriginal() }

assertEquals(-1, adder.addOne(2))
assertEquals(4, adder.addOne(3)) // original function is called
```

## Mock relaxed for functions returning Unit

If you want `Unit`-returning functions to be relaxed, you can use `relaxUnitFun = true` as an argument to the `mockk` function,
`@MockK`annotation or `MockKAnnotations.init` function.

Function:
```kotlin
mockk<ClassBeingMocked>(relaxUnitFun = true)
```

Annotation:
```kotlin
@MockK(relaxUnitFun = true)
lateinit var mock1: ClassBeingMocked
init {
    MockKAnnotations.init(this)
}
```

MockKAnnotations.init:
```kotlin
@MockK
lateinit var mock2: ClassBeingMocked
init {
    MockKAnnotations.init(this, relaxUnitFun = true)
}
```

## Object mocks

Objects can be turned into mocks in the following way:

```kotlin
object ObjBeingMocked {
  fun add(a: Int, b: Int) = a + b
}

mockkObject(ObjBeingMocked) // applies mocking to an Object

assertEquals(3, ObjBeingMocked.add(1, 2))

every { ObjBeingMocked.add(1, 2) } returns 55

assertEquals(55, ObjBeingMocked.add(1, 2))
```

To revert back, use `unmockkObject` or `unmockkAll` (more destructive: cancels object, static and constructor mocks)

```kotlin
@Before
fun beforeTests() {
    mockkObject(ObjBeingMocked)
    every { ObjBeingMocked.add(1,2) } returns 55
}

@Test
fun willUseMockBehaviour() {
    assertEquals(55, ObjBeingMocked.add(1,2))
}

@After
fun afterTests() {
    unmockkObject(ObjBeingMocked)
    // or unmockkAll()
}
```

Despite the Kotlin language restrictions, you can create new instances of objects if required by testing logic:
```kotlin
val newObjectMock = mockk<ObjBeingMocked>()
```

## Class mock

Sometimes you need a mock of an arbitrary class. Use `mockkClass` in those cases.

```kotlin
val car = mockkClass(Car::class)

every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

verify { car.drive(Direction.NORTH) }
```

## Enumeration mocks

Enums can be mocked using `mockkObject`:

```kotlin
enum class Enumeration(val goodInt: Int) {
    CONSTANT(35),
    OTHER_CONSTANT(45);
}

mockkObject(Enumeration.CONSTANT)
every { Enumeration.CONSTANT.goodInt } returns 42
assertEquals(42, Enumeration.CONSTANT.goodInt)
```

## Constructor mocks

Sometimes, especially in code you don't own, you need to mock newly created objects.
For this purpose, the following constructs are provided:

```kotlin
class MockCls {
  fun add(a: Int, b: Int) = a + b
}

mockkConstructor(MockCls::class)

every { anyConstructed<MockCls>().add(1, 2) } returns 4

assertEquals(4, MockCls().add(1, 2)) // note new object is created

verify { anyConstructed<MockCls>().add(1, 2) }
```

The basic idea is that just after the constructor of the mocked class is executed (any of them), objects become a `constructed mock`.
Mocking behavior of such a mock is connected to the special `prototype mock` denoted by `anyConstructed<MockCls>()`.
There is one instance per class of such a `prototype mock`. Call recording also happens to the `prototype mock`.
If no behavior for the function is specified, then the original function is executed.

In case a class has more than one constructor, each can be mocked separately:

```kotlin
class MockCls(private val a: Int = 0) {
  constructor(x: String) : this(x.toInt())
  fun add(b: Int) = a + b
}

mockkConstructor(MockCls::class)

every { constructedWith<MockCls>().add(1) } returns 2
every {
    // Mocks the constructor which takes a String
    constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).add(2) 
} returns 3
every {
    // Mocks the constructor which takes an Int
    constructedWith<MockCls>(EqMatcher(4)).add(any()) 
} returns 4

assertEquals(2, MockCls().add(1))
assertEquals(3, MockCls("2").add(2))
assertEquals(4, MockCls(4).add(7))

verify {
    constructedWith<MockCls>().add(1)
    constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).add(2)
    constructedWith<MockCls>(EqMatcher(4)).add(7)
}
```

Note that in this case, a `prototype mock` is created for every set of argument matchers passed to `constructedWith`.
This means that when verifying invocations on a `prototype mock` you have to ensure that the argument matchers used are
the same that where used when stubbing, i.e. the matchers in the `verify` block and in the `every` block must be the
same.


## Partial argument matching

You can mix both regular arguments and matchers:

```kotlin
val car = mockk<Car>()

every {
  car.recordTelemetry(
    speed = more(50),
    direction = Direction.NORTH, // here eq() is used
    lat = any(),
    long = any()
  )
} returns Outcome.RECORDED

car.recordTelemetry(60, Direction.NORTH, 51.1377382, 17.0257142)

verify { car.recordTelemetry(60, Direction.NORTH, 51.1377382, 17.0257142) }

confirmVerified(car)
```

## Chained calls

You can stub chains of calls:

```kotlin
val car = mockk<Car>()

every { car.door(DoorType.FRONT_LEFT).windowState() } returns WindowState.UP

car.door(DoorType.FRONT_LEFT) // returns chained mock for Door
car.door(DoorType.FRONT_LEFT).windowState() // returns WindowState.UP

verify { car.door(DoorType.FRONT_LEFT).windowState() }

confirmVerified(car)
```

::: info
If the function's return type is generic then the information about the actual type is gone.
To make chained calls work, additional information is required.
Most of the time the framework will catch the cast exception and do `autohinting`.
In the case it is explicitly required, use `hint` before making the next call.
:::

```kotlin
every { obj.op2(1, 2).hint(Int::class).op1(3, 4) } returns 5
```

## Hierarchical mocking

From version 1.9.1 mocks may be chained into hierarchies:

```kotlin
interface AddressBook {
    val contacts: List<Contact>
}

interface Contact {
    val name: String
    val telephone: String
    val address: Address
}

interface Address {
    val city: String
    val zip: String
}

val addressBook = mockk<AddressBook> {
    every { contacts } returns listOf(
        mockk {
            every { name } returns "John"
            every { telephone } returns "123-456-789"
            every { address.city } returns "New-York"
            every { address.zip } returns "123-45"
        },
        mockk {
            every { name } returns "Alex"
            every { telephone } returns "789-456-123"
            every { address } returns mockk {
                every { city } returns "Wroclaw"
                every { zip } returns "543-21"
            }
        }
    )
}
```

## Capturing

You can capture an argument to a `CapturingSlot` or `MutableList`.

`CapturingSlot` is usually created via factory method `slot<T : Any?>()` and is possible to capture nullable and non nullable types.
`MutableList` is intended for capturing multiple values during testing.

```kotlin
enum class Direction { NORTH, SOUTH }
enum class RecordingOutcome { RECORDED }
enum class RoadType { HIGHWAY }
class Car {
    fun recordTelemetry(speed: Double, direction: Direction, roadType: RoadType?): RecordingOutcome {
        TODO("not implement for showcase")
    }
}

val car = mockk<Car>()
// allow to capture parameter with non nullable type `Double`
val speedSlot = slot<Double>()
// allow to capture parameter with nullable type `RoadType`
val roadTypeSlot = slot<RoadType?>()
val list = mutableListOf<Double>()

every {
    car.recordTelemetry(
        // makes mock match calls with any value for `speed`
        // and record it in a slot
        speed = capture(speedSlot),
        // makes mock and capturing only match calls with specific `direction`.
        // Use `any()` to match calls with any `direction`
        direction = Direction.NORTH,
        // makes mock match calls with any value for `roadType` 
        // and record it in a slot
        roadType = captureNullable(roadTypeSlot),
    )
} answers {
    println("Speed: ${speedSlot.captured}, roadType: ${roadTypeSlot.captured}")

    RecordingOutcome.RECORDED
}

every {
    car.recordTelemetry(
        speed = capture(list),
        direction = Direction.SOUTH,
        roadType = captureNullable(roadTypeSlot),
    )
} answers {
    println("Speed: ${list}, roadType: ${roadTypeSlot.captured}")

    RecordingOutcome.RECORDED
}

// prints Speed: 15.0, roadType: null
car.recordTelemetry(speed = 15.0, direction = Direction.NORTH, null)
// prints Speed: [16.0], roadType: HIGHWAY
car.recordTelemetry(speed = 16.0, direction = Direction.SOUTH, RoadType.HIGHWAY) 

verifyOrder {
    car.recordTelemetry(speed = or(15.0, 16.0), direction = any(), roadType = null)
    car.recordTelemetry(speed = 16.0, direction = any(), roadType = RoadType.HIGHWAY)
}

confirmVerified(car)
```

## Returning Unit

If a function returns `Unit`, you can use the `justRun` construct:

```kotlin
class MockedClass {
    fun sum(a: Int, b: Int): Unit {
        println(a + b)
    }
}

val obj = mockk<MockedClass>()

justRun { obj.sum(any(), 3) }

obj.sum(1, 1)
obj.sum(1, 2)
obj.sum(1, 3)

verify {
    obj.sum(1, 1)
    obj.sum(1, 2)
    obj.sum(1, 3)
}
```

Other ways to write `justRun { obj.sum(any(), 3) }`:
- `every { obj.sum(any(), 3) } just Runs`
- `every { obj.sum(any(), 3) } returns Unit`
- `every { obj.sum(any(), 3) } answers { Unit }`
