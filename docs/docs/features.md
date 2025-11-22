# Features

## Annotations

You can use annotations to simplify the creation of mock objects:

```kotlin
class TrafficSystem {
  lateinit var car1: Car
  
  lateinit var car2: Car
  
  lateinit var car3: Car
}

class CarTest {
  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @MockK(relaxUnitFun = true)
  lateinit var car3: Car

  @SpyK
  var car4 = Car()
  
  @InjectMockKs
  var trafficSystem = TrafficSystem()
  
  @Before
  fun setUp() = MockKAnnotations.init(this, relaxUnitFun = true) // turn relaxUnitFun on for all mocks

  @Test
  fun calculateAddsValues1() {
      // ... use car1, car2, car3 and car4
  }
}
```

Injection first tries to match properties by name, then by class or superclass.
Check the `lookupType` parameter for customization.

Properties are injected even if `private` is applied. Constructors for injection are selected from the biggest
number of arguments to lowest.

`@InjectMockKs` by default injects only `lateinit var`s or `var`s that are not assigned.
To change this, use `overrideValues = true`. This would assign the value even if it is already initialized somehow.
To inject `val`s, use `injectImmutable = true`. For a shorter notation use `@OverrideMockKs` which does the same as
`@InjectMockKs` by default, but turns these two flags on.

## JUnit4

JUnit 4 exposes a rule-based API to allow for some automation following the test lifecycle. MockK includes a rule which uses this to set up and tear down your mocks without needing to manually call `MockKAnnotations.init(this)`. Example:

```kotlin
class CarTest {
  @get:Rule
  val mockkRule = MockKRule(this)

  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @Test
  fun something() {
     every { car1.drive() } just runs
     every { car2.changeGear(any()) } returns true
     // etc
  }
}
```

## JUnit5

In JUnit5 you can use `MockKExtension` to initialize your mocks.

```kotlin
@ExtendWith(MockKExtension::class)
class CarTest {
  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @MockK(relaxUnitFun = true)
  lateinit var car3: Car

  @SpyK
  var car4 = Car()

  @Test
  fun calculateAddsValues1() {
      // ... use car1, car2, car3 and car4
  }
}
```

Additionally, it adds the possibility to use `@MockK` and `@RelaxedMockK` on test function parameters:

```kotlin
@Test
fun calculateAddsValues1(@MockK car1: Car, @RelaxedMockK car2: Car) {
  // ... use car1 and car2
}
```

Finally, this extension will call `unmockkAll` and `clearAllMocks` in a `@AfterAll` callback, ensuring your test environment is clean after
each test class execution.
You can disable this behavior by adding the `@MockKExtension.KeepMocks` annotation to your class or globally by setting
the `mockk.junit.extension.keepmocks=true` property.
(Since v1.13.11)
Alternatively, since `clearAllMocks` by default (`currentThreadOnly=false`) is not thread-safe, if you need to run test in parallel you can add the
`MockKExtension.RequireParallelTesting` annotation to your class or set the `mockk.junit.extension.requireParallelTesting=true`
property to disable calling it in the `@AfterAll` callback.
If `clearAllMocks` is explicitly called, you can supply `clearAllMocks(currentThreadOnly = true)` so that it only clears mocks created within the same thread (since v1.13.12).

### Automatic verification confirmation

You can make sure that all stubbed methods are actually verified by also annotating your test class with `@MockKExtension.ConfirmVerification`.

This will internally call `confirmVerified` on all mocks after each test, to make sure there are no unnecessary stubbings.

Please note that this behavior may not work as expected when running tests in your IDE, as it is Gradle who takes care of handling the exception being thrown when these `confirmVerified` calls fail.

### Automatic unnecessary stubbing check

You can make sure that all stubbed methods are useful - used at least once - by also annotating your test class with `@MockKExtension.CheckUnnecessaryStub`.

This will internally call `checkUnnecessaryStub` on all mocks after each test, to make sure there are no unnecessary stubbings.


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

Note: relaxed mocking is working badly with generic return types. A class cast exception is usually thrown in this case.
Opt for stubbing manually in the case of a generic return type.

Workaround:

```kotlin
val func = mockk<() -> Car>(relaxed = true) // in this case invoke function has generic return type

// this line is workaround, without it the relaxed mock would throw a class cast exception on the next line
every { func() } returns Car() // or you can return mockk() for example 

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
    constructedWith<MockCls>(OfTypeMatcher<String>(String::class)).add(2) // Mocks the constructor which takes a String
} returns 3
every {
    constructedWith<MockCls>(EqMatcher(4)).add(any()) // Mocks the constructor which takes an Int
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

Note: if the function's return type is generic then the information about the actual type is gone.  
To make chained calls work, additional information is required.  
Most of the time the framework will catch the cast exception and do `autohinting`.  
In the case it is explicitly required, use `hint` before making the next call.

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
        speed = capture(speedSlot), // makes mock match calls with any value for `speed` and record it in a slot
        direction = Direction.NORTH, // makes mock and capturing only match calls with specific `direction`. Use `any()` to match calls with any `direction`
        roadType = captureNullable(roadTypeSlot), // makes mock match calls with any value for `roadType` and record it in a slot
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

car.recordTelemetry(speed = 15.0, direction = Direction.NORTH, null) // prints Speed: 15.0, roadType: null
car.recordTelemetry(speed = 16.0, direction = Direction.SOUTH, RoadType.HIGHWAY) // prints Speed: [16.0], roadType: HIGHWAY

verifyOrder {
    car.recordTelemetry(speed = or(15.0, 16.0), direction = any(), roadType = null)
    car.recordTelemetry(speed = 16.0, direction = any(), roadType = RoadType.HIGHWAY)
}

confirmVerified(car)
```

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

## Coroutines

To mock coroutines you need to add another dependency to the support library.
### Gradle

```kotlin
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:x.x")
```

### Maven

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>x.x</version>
    <scope>test</scope>
</dependency>
```
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

Note: there is a known issue if using a spy with a suspending function: https://github.com/mockk/mockk/issues/554

## Top Level functions

Kotlin lets you declare functions that don’t belong to any class or object, called top-level functions. These calls are translated to static methods in `jvm` environments, and a special Java class is generated to hold the functions. These top-level functions can be mocked using `mockkStatic`. You just need to import the function and pass a reference as the argument:

```kotlin
import com.cars.buildCar

val testCar = Car()
mockkStatic(::buildCar)
every { buildCar() } returns testCar

assertEquals(testCar, buildCar())

verify { buildCar() }
```

Mocking a function will clear any existing mocks of other functions declared in the same file, equivalent to calling `clearStaticMockk` on the generated enclosing class.

## Extension functions

There are three types of extension function in Kotlin:

* class-wide
* object-wide
* module-wide

For an object or a class, you can mock extension functions just by creating a regular `mockk`:

```kotlin
data class Obj(val value: Int)

class Ext {
    fun Obj.extensionFunc() = value + 5
}

with(mockk<Ext>()) {
    every {
        Obj(5).extensionFunc()
    } returns 11

    assertEquals(11, Obj(5).extensionFunc())

    verify {
        Obj(5).extensionFunc()
    }
}
```

To mock module-wide extension functions you need to
build `mockkStatic(...)` with the module's class name as an argument.
For example "pkg.FileKt" for module `File.kt` in the `pkg` package.

```kotlin
data class Obj(val value: Int)

// declared in File.kt ("pkg" package)
fun Obj.extensionFunc() = value + 5

mockkStatic("pkg.FileKt")

every {
    Obj(5).extensionFunc()
} returns 11

assertEquals(11, Obj(5).extensionFunc())

verify {
    Obj(5).extensionFunc()
}
```

In `jvm` environments you can replace the class name with a function reference:
```kotlin
mockkStatic(Obj::extensionFunc)
```
Note that this will mock the whole `pkg.FileKt` class, and not just `extensionFunc`.

This syntax also applies for extension properties:
```kotlin
val Obj.squareValue get() = value * value

mockkStatic(Obj::squareValue)
```

If `@JvmName` is used, specify it as a class name.

KHttp.kt:
```kotlin
@file:JvmName("KHttp")

package khttp
// ... KHttp code 
```

Testing code:
```kotlin
mockkStatic("khttp.KHttp")
```

Sometimes you need to know a little bit more to mock an extension function.
For example the extension function `File.endsWith()` has a totally unpredictable `classname`:
```kotlin
mockkStatic("kotlin.io.FilesKt__UtilsKt")
every { File("abc").endsWith(any<String>()) } returns true
println(File("abc").endsWith("abc"))
```
This is standard Kotlin behaviour that may be unpredictable.
Use `Tools -> Kotlin -> Show Kotlin Bytecode` or check `.class` files in JAR archive to detect such names.

## Varargs

From version 1.9.1, more extended vararg handling is possible:

```kotlin
interface ClsWithManyMany {
    fun manyMany(vararg x: Any): Int
}

val obj = mockk<ClsWithManyMany>()

every { obj.manyMany(5, 6, *varargAll { it == 7 }) } returns 3

println(obj.manyMany(5, 6, 7)) // 3
println(obj.manyMany(5, 6, 7, 7)) // 3
println(obj.manyMany(5, 6, 7, 7, 7)) // 3

every { obj.manyMany(5, 6, *anyVararg(), 7) } returns 4

println(obj.manyMany(5, 6, 1, 7)) // 4
println(obj.manyMany(5, 6, 2, 3, 7)) // 4
println(obj.manyMany(5, 6, 4, 5, 6, 7)) // 4

every { obj.manyMany(5, 6, *varargAny { nArgs > 5 }, 7) } returns 5

println(obj.manyMany(5, 6, 4, 5, 6, 7)) // 5
println(obj.manyMany(5, 6, 4, 5, 6, 7, 7)) // 5

every {
    obj.manyMany(5, 6, *varargAny {
        if (position < 3) it == 3 else it == 4
    }, 7)
} returns 6

println(obj.manyMany(5, 6, 3, 4, 7)) // 6
println(obj.manyMany(5, 6, 3, 4, 4, 7)) // 6
```

## Private functions mocking / dynamic calls

IF you need to mock private functions, you can do it via a dynamic call.
```kotlin
class Car {
    fun drive() = accelerate()

    private fun accelerate() = "going faster"
}

val mock = spyk<Car>(recordPrivateCalls = true)

every { mock["accelerate"]() } returns "going not so fast"

assertEquals("going not so fast", mock.drive())

verifySequence {
    mock.drive()
    mock["accelerate"]()
}
```

If you want to verify private calls, you should create a `spyk` with `recordPrivateCalls = true`

Additionally, a more verbose syntax allows you to get and set properties, combined with the same dynamic calls:

```kotlin
val mock = spyk(Team(), recordPrivateCalls = true)

every { mock getProperty "speed" } returns 33
every { mock setProperty "acceleration" value less(5) } just runs
justRun { mock invokeNoArgs "privateMethod" }
every { mock invoke "openDoor" withArguments listOf("left", "rear") } returns "OK"

verify { mock getProperty "speed" }
verify { mock setProperty "acceleration" value less(5) }
verify { mock invoke "openDoor" withArguments listOf("left", "rear") }
```

## Property backing fields

You can access the backing fields via `fieldValue` and use `value` for the value being set.

Note: in the examples below, we use `propertyType` to specify the type of the `fieldValue`.
This is needed because it is possible to capture the type automatically for the getter.
Use `nullablePropertyType` to specify a nullable type.

**Note:** This is only for public fields. It is nearly impossible to mock private properties as they don't have getter methods attached. Use Java reflection to make the field accessible or use `@VisibleForTesting` annotation in the source.

```kotlin
val mock = spyk(MockCls(), recordPrivateCalls = true)

every { mock.property } answers { fieldValue + 6 }
every { mock.property = any() } propertyType Int::class answers { fieldValue += value }
every { mock getProperty "property" } propertyType Int::class answers { fieldValue + 6 }
every { mock setProperty "property" value any<Int>() } propertyType Int::class answers  { fieldValue += value }
every {
    mock.property = any()
} propertyType Int::class answers {
    fieldValue = value + 1
} andThen {
    fieldValue = value - 1
}
```

## Multiple interfaces

Adding additional behaviours via interfaces and stubbing them:

```kotlin
val spy = spyk(System.out, moreInterfaces = arrayOf(Runnable::class))

spy.println(555)

every {
    (spy as Runnable).run()
} answers {
    (self as PrintStream).println("Run! Run! Run!")
}

val thread = Thread(spy as Runnable)
thread.start()
thread.join()
```

## Mocking Nothing

Nothing special here. If you have a function returning `Nothing`:

```kotlin
fun quit(status: Int): Nothing {
    exitProcess(status)
}
```

Then you can for example throw an exception as behaviour:

```kotlin
every { quit(1) } throws Exception("this is a test")
```

## Clearing vs Unmocking

* clear - deletes the internal state of objects associated with a mock, resulting in an empty object
* unmock - re-assigns transformation of classes back to original state prior to mock

## Scoped mocks

A Scoped mock is a mock that automatically unmocks itself after the code block passed as a parameter has been executed.
You can use the `mockkObject`, `mockkStatic` and `mockkConstructor` functions.

```kotlin
object ObjBeingMocked {
 fun add(a: Int, b: Int) = a + b
}

// ObjBeingMocked will be unmocked after this scope
mockkObject(ObjBeingMocked) {
 assertEquals(3, ObjBeingMocked.add(1, 2))
 every { ObjBeingMocked.add(1, 2) } returns 55
 assertEquals(55, ObjBeingMocked.add(1, 2))
}
```

## Suppressing superclass calls

To suppress a method call, especially a `super` call inside an overridden method, you can stub its behavior on a `spyk`.
Using `every { ... } just runs` replaces the entire method body, preventing the original code from executing.

This is particularly useful for users coming from frameworks like PowerMockito or for testing classes like Android Activities.
For reference, see [PowerMockito suppress documentation](https://github.com/powermock/powermock/wiki/Suppress-Unwanted-Behavior).

```kotlin
// A simple inheritance hierarchy
open class Parent {
    var superCalled = false
    open fun doWork() {
        superCalled = true
    }
}

class Child : Parent() {
    override fun doWork() {
        super.doWork() // We want to suppress this call during testing
    }
}

// In your test:
val child = spyk<Child>()

// Stub the method: prevents the original (super) implementation from being invoked
every { child.doWork() } just runs

child.doWork()

// Verify that the superclass method was not executed
assertFalse(child.superCalled)
```

This approach allows you to isolate the logic within your method for unit testing without executing unwanted parent class behavior.