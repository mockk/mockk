![mockk](doc/logo.png)

### Installation

There is two steps to make `mockk` running.

First you need to add dependency on library itself.

<table>
<tr>
    <th>Gradle</th><th>Maven</th>
</tr>
<tr>
    <td>
    <pre>testCompile "io.mockk:mockk:1.0.0"</pre>
    </td><td>
    <pre>
    <dependency>
        <groupId>io.mockk</groupId>
        <artifactId>mockk</artifactId>
        <version>1.0.0</version>
    </dependency>
    </pre>
    </td>
</tr>
</table>

## Nice features

 - [x] pure kotlin mocking DSL
 - [x] signature based partial arguments matchers (innovation)
 - [x] chained calls by default aka mockito deep stubs (but better)
 - [x] removing finals
 - [x] instrumenting no args constructor
 - [x] matcher expressions
 - [x] mocking coroutines
 - [x] capturing lambdas
 - [x] bunch of matchers, verification modes and useful DSL constructs

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
