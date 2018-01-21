![mockk](doc/logo-s-alt2.png) ![kotlin](doc/kotlin-logo.png)

[![Gitter](https://badges.gitter.im/mockk-io/Lobby.svg)](https://gitter.im/mockk-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge) [![Build Status](https://travis-ci.org/oleksiyp/mockk.svg?branch=master)](https://travis-ci.org/oleksiyp/mockk) [![Relase Version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=release)](http://search.maven.org/#search%7Cga%7C1%7Cmockk)  [![Change log](https://img.shields.io/badge/change%20log-%E2%96%A4-yellow.svg)](https://github.com/oleksiyp/mockk/releases) [![Back log](https://img.shields.io/badge/back%20log-%E2%96%A4-orange.svg)](/BACKLOG) [![codecov](https://codecov.io/gh/oleksiyp/mockk/branch/master/graph/badge.svg)](https://codecov.io/gh/oleksiyp/mockk) [![Documentation](https://img.shields.io/badge/documentation-%E2%86%93-yellowgreen.svg)](#nice-features)

```
***Note: version 1.7 has breaking changes***
 * now "just Runs" is an extension function applicable to Unit type
 * function "assertEquals" were renamed
```

Table of contents:

* auto-gen TOC:
{:toc}

## Nice features

 - annotations
 - mocking final classes and functions (via inlining)
 - pure Kotlin mocking DSL
 - matchers partial specification
 - chained calls
 - matcher expressions
 - mocking coroutines
 - capturing lambdas
 - object mocks
 - extension function mocking (static mocks)
 - multiplatform support (JS support is highly experimental)

## Examples & articles

 - [kotlin-fullstack-sample](https://github.com/Kotlin/kotlin-fullstack-sample/pull/28/files#diff-eade18fbfd0abfb6338dbfa647b3215dR17) project covered with tests
 - [DZone article](https://dzone.com/articles/new-mocking-tool-for-kotlin-an-alternative-to-java)
 - [Habrahabr article](https://habrahabr.ru/post/341202/) (RU)
## Installation

All you need to get started is just to add a dependency to `MockK` library.

#### Gradle/maven dependency

<table>
<thead><tr><th>Tool</th><th>Instruction</th></tr></thead>
<tr>
<td width="100"><img src="doc/gradle.png" alt="Gradle"/></td>
<td>
    <pre>testCompile "io.mockk:mockk:1.7"</pre>
    </td>
</tr>
<tr>
<td><img src="doc/maven.png" alt="Maven"/></td>
<td>
<pre>&lt;dependency&gt;
    &lt;groupId&gt;io.mockk&lt;/groupId&gt;
    &lt;artifactId&gt;mockk&lt;/artifactId&gt;
    &lt;version&gt;1.7&lt;/version&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;</pre>
    </td>
</tr>
</table>

## DSL examples

Simplest example. By default mocks are strict, so you need to provide some behaviour.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

verify { car.drive(Direction.NORTH) }
```

### Annotations

Use can use annotations to simplify creation of mock objects:

```
class Test {
  @MockK
  lateinit var car1: Car

  @RelaxedMockK
  lateinit var car2: Car

  @SpyK
  val car3 = Car()

  @Before
  fun setUp() = MockKAnnotations.init(this)

  @Test
  fun calculateAddsValues1() {
      // ... use car1, car2 and car3
  }
}
```

### Spy

Spies allow to mix mocks and real objects.

```kotlin
val car = spyk(Car()) // or spyk<Car>() to call default constructor

car.drive(Direction.NORTH) // returns whatever real function of Car returns

verify { car.drive(Direction.NORTH) }
```

Note: the spy object is a copy of a passed object.

### Relaxed mock

`Relaxed mock` is the mock that returns some simple value for all functions. 
This allows to skip specifying behavior for each case, while still allow to stub things you need.
For reference types chained mocks are returned.

```kotlin
val car = mockk<Car>(relaxed = true)

car.drive(Direction.NORTH) // returns null

verify { car.drive(Direction.NORTH) }
```

Note: relaxed mocking is working badly with generic return type. Usually in this case class cast exception is thrown. You need to specify stubbing manually for case of generic return type.

Workaround:

```kotlin
val func = mockk<() -> Car>(relaxed = true) // in this case invoke function has generic return type

// this line is workaround, without it relaxed mock would throw class cast exception on the next line
every { func() } returns Car() // or you can return mockk() for example 

func()
```

### Object mocks

Objects can be transformed to mocks following way:

```
object MockObj {
  fun add(a: Int, b: Int) = a + b
}

objectMockk(MockObj).use {
  assertEquals(3, MockObj.add(1, 2))

  every { MockObj.add(1, 2) } returns 55

  assertEquals(55, MockObj.add(1, 2))
}
```

Despite Kotlin language limits you can just create new instances of objects:
```
val newObjectMock = mockk<MockObj>()
```

### Partial argument matching

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

obj.recordTelemetry(60, Direction.NORTH, 51.1377382, 17.0257142)

verify { obj.recordTelemetry(60, Direction.NORTH, 51.1377382, 17.0257142) }
```

### Chained calls

You can stub chains of calls:

```kotlin
val car = mockk<Car>()

every { car.door(DoorType.FRONT_LEFT).windowState() } returns WindowState.UP

car.door(DoorType.FRONT_LEFT) // returns chained mock for Door
car.door(DoorType.FRONT_LEFT).windowState() // returns WindowState.UP

verify { car.door(DoorType.FRONT_LEFT).windowState() }
```

Note: in case function return type is generic the information about actual type is erased.
To make chained calls work additional information is required.
Most of the times framework will catch the cast exception and do `autohinting`.
But in the case it is explicitly needed just place `hint` before calls.

```kotlin

every { obj.op2(1, 2).hint(Int::class).op1(3, 4) } returns 5

```


### Capturing

You can capture an argument to a `CapturingSlot` or `MutableList`:

```kotlin
val car = mockk<Car>()

val slot = slot<Double>()
val list = mutableListOf<Double>()

every {
  obj.recordTelemetry(
    speed = capture(slot),
    direction = Direction.NORTH
  )
} answers {
  println(slot.captured)

  Outcome.RECORDED
}


every {
  obj.recordTelemetry(
    speed = capture(list),
    direction = Direction.SOUTH
  )
} answers {
  println(list.captured())

  Outcome.RECORDED
}

obj.recordTelemetry(speed = 15, direction = Direction.NORTH) // prints 15
obj.recordTelemetry(speed = 16, direction = Direction.SOUTH) // prints 16

verify(exactly = 2) { obj.recordTelemetry(speed = or(15, 16), direction = any()) }
```

### Verification atLeast, atMost or exactly times

You can check call count with `atLeast`, `atMost` or `exactly` parameters:

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
```

### Verification order

`verifyAll` verifies that all calls happened without checking it's order.
`verifySequence` verifies that the exact sequence happened and `verifyOrder` that calls happened in order.
`wasNot Called` verifies that mock or list of mocks was not called at all.

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
```

### Returning Unit

If the function is returning `Unit` you can use `just Runs` construct:

```kotlin
class MockedClass {
    fun sum(a: Int, b: Int): Unit {
        println(a + b)
    }
}

val obj = mockk<MockedClass>()

every { obj.sum(any(), 3) } just Runs

obj.sum(1, 1)
obj.sum(1, 2)
obj.sum(1, 3)

verify {
    obj.sum(1, 1)
    obj.sum(1, 2)
    obj.sum(1, 3)
}
```

### Coroutines

To mock coroutines you need to add dependency to the support library.
<table>
<tr>
    <th>Gradle</th>
</tr>
<tr>
    <td>
<pre>testCompile "org.jetbrains.kotlinx:kotlinx-coroutines-core:x.x"</pre>
    </td>
</tr>
</table>
<table>
<tr>
    <th>Maven</th>
</tr>
<tr>
<td>
    <pre>
&lt;dependency&gt;
    &lt;groupId&gt;org.jetbrains.kotlinx&lt;/groupId&gt;
    &lt;artifactId&gt;kotlinx-coroutines-core&lt;/artifactId&gt;
    &lt;version&gt;x.x&lt;/version&gt;
    &lt;scope&gt;test&lt;/scope&gt;
&lt;/dependency&gt;</pre>
    </td>
</tr>
</table>

Then you can use `coEvery`, `coVerify`, `coMatch`, `coAssert`, `coRun`, `coAnswers` or `coInvoke` to mock suspend functions

```kotlin
val car = mockk<Car>()

coEvery { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

coVerify { car.drive(Direction.NORTH) }
```
### Extension functions

There a 3 cases of extension function:

* class wide
* object wide
* module wide

In case of object and class you can mock extension function just by creating
regular `mockk`:

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

To mock module wide extension function you need to
build staticMockk(...) with argument specifying module class name.
For example "pkg.FileKt" for module "File.kt" in "pkg" package

```kotlin
data class Obj(val value: Int)

// declared in File.kt ("pkg" package)
fun Obj.extensionFunc() = value + 5

staticMockk("pkg.FileKt").use {
    every {
        Obj(5).extensionFunc()
    } returns 11

    assertEquals(11, Obj(5).extensionFunc())

    verify {
        Obj(5).extensionFunc()
    }
}
```

### More interfaces

Adding additional behaviours via interfaces and stubbing them:

```kotlin

val spy = spyk(System.out, moreInterfaces = Runnable::class)

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

## DSL tables

Here are few tables helping to master the DSL.

### Matchers

By default simple arguments are matched using `eq()`

|Matcher|Description|
|-------|-----------|
|`any()`|matches any argument|
|`allAny()`|special matcher that uses any() instead of eq() for matchers that are provided as simple arguments|
|`isNull()`|checks if values is null|
|`isNull(inverse=true)`|checks if values is not null|
|`ofType(type)`|checks if values belongs to the type|
|`match { it.startsWith("string") }`|matches via passed predicate|
|`coMatch { it.startsWith("string") }`|matches via passed coroutine predicate|
|`matchNullable { it?.startsWith("string") }`|matches nullable value via passe predicate|
|`coMatchNullable { it?.startsWith("string") }`|matches nullable value via passed coroutine predicate|
|`eq(value)`|matches if value is equal to the provided via deepEquals function|
|`refEq(value)`|matches if value is equal to the provided via reference comparation|
|`cmpEq(value)`|matches if value is equal to the provided via compareTo function|
|`less(value)`|matches if value is less to the provided via compareTo function|
|`more(value)`|matches if value is more to the provided via compareTo function|
|`less(value, andEquals=true)`|matches if value is less or equals to the provided via compareTo function|
|`more(value, andEquals=true)`|matches if value is more or equals to the provided via compareTo function|
|`and(left, right)`|combines two matchers via logical and|
|`or(left, right)`|combines two matchers via logical or|
|`not(matcher)`|negates the matcher|
|`capture(slot)`|captures a value to a `CapturingSlot`|
|`capture(mutableList)`|captures a value to a list|
|`captureNullable(mutableList)`|captures a value to a list together with null values|
|`captureLambda()`|captures lambda|
|`captureCoroutine()`|captures coroutine|
|`invoke(...)`|calls matched argument|
|`coInvoke(...)`|calls matched argument for coroutine|
|`hint(cls)`|hints next return type in case it's got erased|

Few special matchers available in verification mode only:

|Matcher|Description|
|-------|-----------|
|`run { code }`|matches any value and allows to execute some code|
|`runNullable { code }`|matches any nullable value and allows to execute some code|
|`coRun { code }`|matches any value and allows to execute some coroutine code|
|`coRunNullable { code }`|matches any nullable value and allows to execute some coroutine code|
|`assert(msg) { predicate }`|matches any value and checks the assertion|
|`assertNullable(msg) { predicate }`|matches any nullable value and checks the assertion|
|`coAssert(msg) { predicate }`|matches any value and checks the coroutine assertion|
|`coAssertNullable(msg) { predicate }`|matches any nullable value and checks the coroutine assertion|

### Validators

|Validator|Description|
|---------|-----------|
|`verify { mock.call() }`|Do unordered verification that call were performed|
|`verify(inverse=true) { mock.call() }`|Do unordered verification that call were not performed|
|`verify(atLeast=n) { mock.call() }`|Do unordered verification that call were performed at least `n` times|
|`verify(atMost=n) { mock.call() }`|Do unordered verification that call were performed at most `n` times|
|`verify(exactly=n) { mock.call() }`|Do unordered verification that call were performed at exactly `n` times|
|`verifyAll { mock.call1(); mock.call2() }`|Do unordered verification that only the specified calls were executed for mentioned mocks|
|`verifyOrder { mock.call1(); mock.call2() }`|Do verification that sequence of calls went one after another|
|`verifySequence { mock.call1(); mock.call2() }`|Do verification that only the specified sequence of calls were executed for mentioned mocks|
|`verify { mock wasNot Called }`|Do verification that mock was not called|
|`verify { listOf(mock1, mock2) wasNot Called }`|Do verification that list of mocks were not called|

### Answers

Answer can be followed by one or more additional answers.

|Answer|Description|
|------|-----------|
|`returns value`|specify that matched call returns one specified value|
|`returnsMany list`|specify that matched call returns value from the list, returning each time next element|
|`throws ex`|specify that matched call throws an exception|
|`answers { code }`|specify that matched call answers with code block scoped with `answer scope`|
|`coAnswers { code }`|specify that matched call answers with coroutine code block  with `answer scope`|
|`answers answerObj`|specify that matched call answers with Answer object|
|`answers { nothing }`|specify that matched call answers null|
|`just Runs`|specify that matched call is returning Unit (returns null)|

### Additional answer

Next answer is returned on each consequent call and last value is persisted.
So this has similiar to `returnsMany` semantics.

|Addititonal answer|Description|
|------------------|-----------|
|`andThen value`|specify that matched call returns one specified value|
|`andThenMany list`|specify that matched call returns value from the list, returning each time next element|
|`andThenThrows ex`|specify that matched call throws an exception|
|`andThen { code }`|specify that matched call answers with code block scoped with `answer scope`|
|`coAndThen { code }`|specify that matched call answers with coroutine code block  with `answer scope`|
|`andThenAnswer answerObj`|specify that matched call answers with Answer object|
|`andThen { nothing }`|specify that matched call answers null|

### Answer scope

|Parameter|Description|
|---------|-----------|
|`call`|a call object that consists of invocation and matcher|
|`invocation`|contains information regarding actual function invoked|
|`matcher`|contains information regarding matcher used to match invocation|
|`self`|reference the object invocation made|
|`method`|reference to the function invocation made|
|`args`|reference to arguments of invocation|
|`nArgs`|number of invocation argument|
|`arg(n)`|n-th argument|
|`firstArg()`|first argument|
|`secondArg()`|second argument|
|`thirdArg()`|third argument|
|`lastArg()`|last argument|
|`captured()`|the last element in the list for convenience when capturing to the list|
|`lambda<...>().invoke()`|call captured lambda|
|`coroutine<...>().coInvoke()`|call captured coroutine|
|`nothing`|null value for returning nothing as an answer|

## Getting Help

To ask questions, please use stackoverflow or gitter.

* Chat/Gitter: [https://gitter.im/mockk-io/Lobby](https://gitter.im/mockk-io/Lobby)
* Stack Overflow: [http://stackoverflow.com/questions/tagged/mockk](http://stackoverflow.com/questions/tagged/mockk)

To report bugs, please use the GitHub project.

* Project Page: [https://github.com/oleksiyp/mockk](https://github.com/oleksiyp/mockk)
* Reporting Bugs: [https://github.com/oleksiyp/mockk/issues](https://github.com/oleksiyp/mockk/issues)
