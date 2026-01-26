# Annotations and JUnit

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

## @InjectMockKs dependency order

If multiple `@InjectMockKs` properties depend on each other via constructor parameters, initialization order matters.
By default, MockK processes them in reflection order; to resolve dependencies deterministically, enable dependency
order:

```kotlin
@Before
fun setUp() = MockKAnnotations.init(this, useDependencyOrder = true)
```

This applies a topological sort across `@InjectMockKs` and throws `MockKException` on circular dependencies.
Enabling `useDependencyOrder` adds an approximate 3-5% performance overhead during initialization.


## JUnit 4

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

## JUnit 5

In JUnit 5 you can use `MockKExtension` to initialize your mocks.

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

## Automatic verification confirmation

You can make sure that all stubbed methods are actually verified by also annotating your test class with `@MockKExtension.ConfirmVerification`.

This will internally call `confirmVerified` on all mocks after each test, to make sure there are no unnecessary stubbings.

Please note that this behavior may not work as expected when running tests in your IDE, as it is Gradle who takes care of handling the exception being thrown when these `confirmVerified` calls fail.

## Automatic unnecessary stubbing check

You can make sure that all stubbed methods are useful - used at least once - by also annotating your test class with `@MockKExtension.CheckUnnecessaryStub`.

This will internally call `checkUnnecessaryStub` on all mocks after each test, to make sure there are no unnecessary stubbings.
