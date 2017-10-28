![mockk](doc/logo.png)

## Nice features

[![Join the chat at https://gitter.im/mockk-io/Lobby](https://badges.gitter.im/mockk-io/Lobby.svg)](https://gitter.im/mockk-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

 - removing finals (via Java Agent)
 - pure kotlin mocking DSL
 - partial arguments matchers
 - chained calls / deep stubs
 - matcher expressions
 - mocking coroutines
 - capturing lambdas
 - bunch of matchers
 - few verification modes

## Installation

There is two installation steps.

First you need to add dependency to the library itself.
<table>
<tr>
    <th>Gradle</th><th>Maven</th>
</tr>
<tr>
    <td>
    <pre>testCompile "io.mockk:mockk:1.0.0"</pre>
    </td><td>
    <pre>
    &lt;dependency&gt;
        &lt;groupId&gt;io.mockk&lt;/groupId&gt;
        &lt;artifactId&gt;mockk&lt;/artifactId&gt;
        &lt;version&gt;1.0.0&lt;/version&gt;
        &lt;scope&gt;test&lt;/scope&gt;
    &lt;/dependency&gt;</pre>
    </td>
</tr>
</table>

Second you need to add class transformation agent.
This agent is required to remove final modifier from running classes.
There is few ways to add Java Agent/Class Loader.
Only running as a Java Agent is an officially supported way.
All others are existing for convenience and should be used at your own risk.
<table>
<tr><th>Method</th><th>Instruction</th></tr>
<tr>
<td>Java Agent gradle</td>
<td>
	Add <a href="https://github.com/Zoltu/application-agent-gradle-plugin">agent</a> gradle plugin.
    Use following agent:
    <pre>agent "io.mockk:mockk-agent:1.0.0"</pre>
</td>
</tr><tr>
<td>Java Agent maven</td>
<td>
    Add dependency to mockk-agent. Configure maven surefire plugin:
    <pre>&lt;argLine&gt;-javaagent:${io.mockk:mockk-agent:jar}&lt;/argLine&gt;</pre>
</td>
</tr><tr>
<td>Java Agent JVM</td>
<td>
    Add JVM parameter to launch agent:
    <pre>-javaagent:libs/mockk-agent-1.0.0.jar</pre>
</td>
</tr><tr>
<td>JUnit4</td>
<td>
    Add this dependency to your project:
    <pre>testCompile "io.mockk:mockk-agent:1.0.0"</pre>
    Use annotation for each test:
    <pre>@RunWith(MockKJUnit4Runner::class)</pre>
    If @RunWith is specified on superclass then it will be used to run after class loader is set.
    So you can specify next runner doing the real running job.
    Use @ChainedRunWith to override such delegated runner.
    If neither is specified default JUnit runner is used.
</td>
</tr><tr>
<td>JUnit5</td>
<td>
    Just add this dependency to your project:
    <pre>testCompile "io.mockk:mockk-agent:1.0.0"</pre>
    JUnit5 test will be hooked via TestExecutionListener.
    Note: this implementation is totally a hack.
</td>
</tr>
</table>

## DSL

Simplest example:

  ```kotlin

    val car = mockk<Car>()

    every { car.drive(Direction.NORTH) } returns Outcome.OK

    car.drive(Direction.NORTH) // returns OK

    verify { car.drive(Direction.NORTH) }

  ```

## Partial argument matching

You can skip parameters while specifying matchers.
MockK runs your block few times, builds so called signature and
auto-detects places where matchers appear:

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: Int) = a + b
    }

    val obj = mockk<MockedClass>()

    every { obj.sum(1, eq(2)) } returns 5

    obj.sum(1, 2) // returns 5

    verify { obj.sum(eq(1), 2) }

  ```

## Chained calls

Mock can have child mocks. This allows to mock chains of calls:

  ```kotlin

    class MockedClass1 {
        fun op1(a: Int, b: Int) = a + b
    }

    class MockedClass2 {
        fun op2(c: Int, d: Int): MockedClass1 = ...
    }

    val obj = mockk<MockedClass2>()

    every { obj.op2(1, eq(2)).op1(3, any()) } returns 5

    obj.op2(1, 2) // returns child mock
    obj.op2(1, 2).op1(3, 22) // returns 5

    verify { obj.op2(any(), 2).op2(3, 22) }

  ```

### Capturing

Simplest way of capturing is capturing to the `CapturingSlot`:

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: Int) = a + b
    }

    val obj = mockk<MockedClass>()
    val slot = slot<Int>()

    every { obj.sum(1, capture(slot)) } answers { 2 + slot.captured }

    obj.sum(1, 2) // returns 4

    verify { obj.sum(1, 2) }


  ```

### Capturing lambda

You can capture lambdas with `CapturingSlot<Any>`,
but for convenience there is captureLambda construct present:

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: () -> Int) = a + b()
    }

    val obj = mockk<MockedClass>()
    val slot = slot<Int>()

    every { obj.sum(1, captureLambda(Function0::class)) }
        answers { 2 + lambda.invoke() }

    obj.sum(1) { 2 } // returns 4

    verify { obj.sum(1, any()) }

  ```

### Capturing to the list

If you need several captured values you can capture values to the `MutableList`.
Method `captured()` is returning the last element of the list.

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: Int) = a + b
    }

    val obj = mockk<MockedClass>()
    val lst = mutableListOf<Int>()

    every { obj.sum(1, capture(lst)) } answers { 2 + lst.captured() }

    obj.sum(1, 2) // returns 4

    verify { obj.sum(1, 2) }

  ```

### Verification with atLeast

Checking at least how much method was called:

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: Int) = a + b
    }

    val obj = mockk<MockedClass>()
    val lst = mutableListOf<Int>()

    every { obj.sum(any(), capture(lst)) }
        answers { 1 + firstArg() + lst.captured() }

    obj.sum(1, 2) // returns 4
    obj.sum(1, 3) // returns 5
    obj.sum(2, 2) // returns 5

    verify(atLeast=3) { obj.sum(any(), any()) }

  ```


### Verification sequence

Checking the exact sequence of calls:

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: Int) = a + b
    }

    val obj = mockk<MockedClass>()
    val slot = slot<Int>()

    every { obj.sum(any(), capture(slot)) } answers { 1 + firstArg() + slot.captured }

    obj.sum(1, 2) // returns 4
    obj.sum(1, 3) // returns 5
    obj.sum(2, 2) // returns 5

    verifySequence {
        obj.sum(1, 2)
        obj.sum(1, 3)
        obj.sum(2, 2)
    }

  ```

### Returning nothing

If the method is returning Unit(i.e. no return value) you still need to specify return value:

  ```kotlin

    class MockedClass {
        fun sum(a: Int, b: Int): Unit {
            println(a + b)
        }
    }

    val obj = mockk<MockedClass>()

    every { obj.sum(any(), 1) } answers { nothing }
    every { obj.sum(any(), 2) } returns null

    obj.sum(1, 1)
    obj.sum(1, 2)

    verify {
        obj.sum(1, 1)
        obj.sum(1, 2)
    }

  ```

## DSL tables

Here is a few tables helping to master the DSL.

### Matchers

|Matcher|Description|
|-------|-----------|
|`any()`|matches any argument|
|`allAny()`|special matcher that uses any() instead of eq() for matchers that are provided as simple arguments|
|`isNull()`|checks if values is null|
|`isNull(inverse=true)`|checks if values is not null|
|`ofType(type)`|checks if values belongs to the type|
|`match { it.startsWith("string") }`|matches via arbitary lambda expression|
|`eq(value)`|matches if value is equal to the provided via deepEquals method|
|`refEq(value)`|matches if value is equal to the provided via reference comparation|
|`cmpEq(value)`|matches if value is equal to the provided via compareTo method|
|`less(value)`|matches if value is less to the provided via compareTo method|
|`more(value)`|matches if value is more to the provided via compareTo method|
|`less(value, andEquals=true)`|matches if value is less or equals to the provided via compareTo method|
|`more(value, andEquals=true)`|matches if value is more or equals to the provided via compareTo method|
|`and(left, right)`|combines two matchers via logical and|
|`or(left, right)`|combines two matchers via logical or|
|`not(matcher)`|negates the matcher|
|`capture(mutableList)`|captures a value to a list|
|`captureNullable(mutableList)`|captures a value to a list together with null values|
|`captureLambda(lambdaClass)`|captures lambda expression(allowed one per call)|

### Validators

|Validator|Description|
|---------|-----------|
|`verify { mock.call() }`|Do unordered verification that call were performed|
|`verify(inverse=true) { mock.call() }`|Do unordered verification that call were not performed|
|`verify(atLeast=n) { mock.call() }`|Do unordered verification that call were performed at least `n` times|
|`verify(atMost=n) { mock.call() }`|Do unordered verification that call were performed at most `n` times|
|`verify(excatly=n) { mock.call() }`|Do unordered verification that call were performed at exactly `n` times|
|`verifyOrder { mock.call1(); mock.call2() }`|Do verification that sequence of calls went one after another|
|`verifySequence { mock.call1(); mock.call2() }`|Do verification that only the specified sequence of calls were executed for mentioned mocks|

### Answers

|Answer|Description|
|------|-----------|
|`returns value`|specify that matched call returns one specified value|
|`returnsMany list`|specify that matched call returns value from the list, returning each time next element|
|`throws ex`|specify that matched call throws an exception|
|`answers { code }`|specify that matched call answers with lambda in answer scope|

### Answer scope

|Parameter|Description|
|---------|-----------|
|`call`|a call object that consists of invocation and matcher|
|`invocation`|contains information regarding actual method invoked|
|`matcher`|contains information regarding matcher used to match invocation|
|`self`|reference the object invocation made|
|`method`|reference to the method invocation made|
|`args`|reference to arguments of invocation|
|`nArgs`|number of invocation argument|
|`firstArg()`|first argument|
|`secondArg()`|second argument|
|`thirdArg()`|third argument|
|`lastArg()`|last argument|
|`caputred()`|the last element in the list for convenience when capturing to the list|
|`lambda`|captured lambda|
|`nothing`|null value for returning nothing as an answer|

## Getting Help

To ask questions please use stackoverflow or gitter.

* Chat/Gitter: [https://gitter.im/oleksiyp/mockk](https://gitter.im/oleksiyp/mockk)
* Stack Overflow: [http://stackoverflow.com/questions/tagged/mockk](http://stackoverflow.com/questions/tagged/mockk)

To report bugs, please use the GitHub project.

* Project Page: [https://github.com/oleksiyp/mockk](https://github.com/oleksiyp/mockk)
* Reporting Bugs: [https://github.com/oleksiyp/mockk/issues](https://github.com/oleksiyp/mockk/issues)
