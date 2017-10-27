![mockk](doc/logo.png)

### Installation

There is two steps to make `mockk` running.

1. You need to add dependency on library itself.
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

2. You need to add class transformation agent.
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
</tr><tr>
<td>Java Agent maven</td>
<td>
	Configure maven surefire plugin: <pre>&lt;argLine&gt;-javaagent:${io.mockk:mockk-agent:jar}&lt;/argLine&gt;</pre>
</tr><tr>
<td>Java Agent JVM</td>
<td>
    Add JVM parameter to launch agent:
    <pre>-javaagent:libs/mockk-agent-1.0.0.jar</pre>
</tr><tr>
<td>JUnit4</td>
<td>
    Add this dependency to your project:
    <pre>testCompile "io.mockk:mockk-agent:1.0.0"</pre>
    Use annotation for each test:
    <pre>@RunWith(MockKJUnit4Runner::class)</pre>
    If @RunWith is specified on superclass then it will be used to run after class loader set. So you can specify several runners.
    Use @ChainedRunWith to override such delegated runner.
    If neither is specified default JUnit runner is used.
</tr><tr>
<td>JUnit5</td>
<td>
    Just add this dependency to your project:
    <pre>testCompile "io.mockk:mockk-agent:1.0.0"</pre>
    JUnit5 test will be hooked wia TestExecutionListener.
    Note this implementation is totally a hack.
</tr>
</table>

## Nice features

 - removing finals (via Java Agent)
 - pure kotlin mocking DSL
 - partial arguments matchers
 - chained calls / deep stubs
 - matcher expressions
 - mocking coroutines
 - capturing lambdas
 - bunch of matchers
 - few verification modes

### DSL

  Simplest example

  ```
    val car = mockk<Car>()
    every { car.drive(Direction.NORTH) } returns Outcome.OK

    car.drive(Direction.NORTH) // returns OK

    verify { car.drive(Direction.NORTH) }
  ```

### Partial argument matching

You can skip parameters while specifying matchers.
MockK runs your block few times, build so called signature and
auto-detect places where matchers appear.

  ```
    class MockedClass {
        fun op(a: Int, b: Int) = a + b
    }

    val obj = mockk<MockedClass>()
    every { obj.op(1, eq(2)) } returns 5

    obj.op(1, 2) // returns 5

    verify { obj.op(eq(1), 2) }

  ```

### Chained calls

Mock can have child mocks. This allows to mock chains of calls

  ```
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

## Getting Help

To ask questions please use stackoverflow or gitter.

* Chat/Gitter: [https://gitter.im/oleksiyp/mockk](https://gitter.im/oleksiyp/mockk)
* Stack Overflow: [http://stackoverflow.com/questions/tagged/mockk](http://stackoverflow.com/questions/tagged/mockk)

To report bugs, please use the GitHub project.

* Project Page: [https://github.com/oleksiyp/mockk](https://github.com/oleksiyp/mockk)
* Reporting Bugs: [https://github.com/oleksiyp/mockk/issues](https://github.com/oleksiyp/mockk/issues)
