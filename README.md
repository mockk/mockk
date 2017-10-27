![mockk](doc/logo.png)

### Nice features

 - [x] pure kotlin mocking DSL
 - [x] signature based partial arguments matchers (innovation)
 - [x] chained calls by default aka mockito deep stubs (but better)
 - [x] removing finals
 - [x] instrumenting no args constructor
 - [x] matcher expressions
 - [x] mocking coroutines
 - [x] capturing lambdas
 - [x] bunch of matchers, verification modes and useful DSL constructs

### Backlog (to be removed on first release)

- [x] removing final, no-args-consrtuctor byte code transformation
- [x] matcher signature
- [x] arguments: firstArg, secondArg, thirdArg, lastArg
- [x] capturing: captureNullable(lst) .captured() extension
- [x] verifyOrder { }, verifySequence { }
- [x] slot.invoke(arg1, arg2, ... lastArg) for lambdas
- [x] coroutines support
- [x] stub(returnsMany, throws)
- [x] clearMocks(answers, calls, mocks)
- [x] verify(atLeast, atMost, nCalls)
- [x] rename methods not to clash other mocking frameworks
- [x] matchers: refEq, more, less, and, or, not, null(), nonNull(), any(isNull=true, ofType=Any)
- [x] arrays support
- [x] deep array equality matching
- [x] child mock chained calls
- [x] don't display "creating mock" in logs for "fake mocks"
- [x] add some comments
- [x] PR
- [x] nulls
- [x] ***MILESTONE1***
- [x] fixing spies
- [x] spyk copy
- [x] relax gateway companion object
- [x] Java 9 support
- [x] split to separate files
- [x] Java 6 compatibility
- [x] running methods: agent, JUnit4, JUnit5
- [ ] Android
- [ ] objenesis
- [x] register on Maven OSSRH
- [ ] habr article
- [ ] ***MILESTONE2 - maven release***
- [ ] Kotlin popular in Germany, Japan, India, USA and Brasil
- [ ] running methods: TestNG
- [ ] documentation: matchers table
- [ ] documentation: answers table
- [ ] documentation: runners table
- [ ] logging "mock<CLS>#1"
- [ ] build agent, coverage ci
- [ ] contributing wiki, forum/group/chat
- [ ] IntelliJ javadoc experience
- [ ] annotation mocking
- [ ] static methods
- [ ] extension function
- [ ] mock: extraInterfaces, mock name
- [ ] better human readable exceptions
- [ ] performance optimization: HashableMatcher
- [ ] performance optimization: ProxyCache
- [ ] performance optimization: Method mapping
- [ ] check nanoTime is OK (otherwise counter)
- [ ] other JVMs
- [ ] PowerMock : Remove final modifier from a class, enum, static methods, methods and inner classes
- [ ] PowerMock : Remove final modifier from
- [ ] PowerMock : Make all constructor public
- [ ] PowerMock : Add additional constructor with specific parameter
- [ ] PowerMock : Clear static initialiser body
- [ ] PowerMock : Add body for native methods
- [ ] PowerMock : Set modifier to public for non-system package-private classes
- [ ] PowerMock : Insert calling mock repository for static methods
- [ ] PowerMock : Replace calling to fields by call to mock repository
- [ ] PowerMock : Replace constructor call by call to mock repository
- [ ] PowerMock : Replace call to system classes by call to mock repository
- [ ] PowerMock : Check size of method body after modification and replace method body by throwing exception if method body size more than allowed in java specification
- [ ] Mockito: Migrating to Mockito 2
- [ ] Mockito: Mockito Android support</a></br/>
- [ ] Mockito: Configuration-free inline mock making</a></br/>
- [ ] Mockito: Let's verify some behaviour!
- [ ] Mockito: How about some stubbing?
- [ ] Mockito: Argument matchers
- [ ] Mockito: Verifying exact number of invocations / at least once / never
- [ ] Mockito: Stubbing void methods with exceptions
- [ ] Mockito: Verification in order
- [ ] Mockito: Making sure interaction(s) never happened on mock
- [ ] Mockito: Finding redundant invocations
- [ ] Mockito: Shorthand for mocks creation - `&#064;Mock` annotation
- [ ] Mockito: Stubbing consecutive calls (iterator-style stubbing)
- [ ] Mockito: Stubbing with callbacks
- [ ] Mockito: `doReturn()`|`doThrow()`|`doAnswer()`|`doNothing()`|`doCallRealMethod()` family of methods
- [ ] Mockito: Spying on real objects
- [ ] Mockito: Changing default return values of unstubbed invocations (Since 1.7)
- [ ] Mockito: Capturing arguments for further assertions (Since 1.8.0)
- [ ] Mockito: Real partial mocks (Since 1.8.0)
- [ ] Mockito: Resetting mocks (Since 1.8.0)
- [ ] Mockito: Troubleshooting & validating framework usage (Since 1.8.0)
- [ ] Mockito: Aliases for behavior driven development (Since 1.8.0)
- [ ] Mockito: Serializable mocks (Since 1.8.1)
- [ ] Mockito: New annotations: `&#064;Captor`, `&#064;Spy`, `&#064;InjectMocks` (Since 1.8.3)
- [ ] Mockito: Verification with timeout (Since 1.8.5)
- [ ] Mockito: Automatic instantiation of `&#064;Spies`, `&#064;InjectMocks` and constructor injection goodness (Since 1.9.0)
- [ ] Mockito: One-liner stubs (Since 1.9.0)
- [ ] Mockito: Verification ignoring stubs (Since 1.9.0)
- [ ] Mockito: Mocking details (Improved in 2.2.x)
- [ ] Mockito: Delegate calls to real instance (Since 1.9.5)
- [ ] Mockito: `MockMaker` API (Since 1.9.5)
- [ ] Mockito: BDD style verification (Since 1.10.0)
- [ ] Mockito: Spying or mocking abstract classes (Since 1.10.12, further enhanced in 2.7.13 and 2.7.14)
- [ ] Mockito: Mockito mocks can be `serialized` / `deserialized` across classloaders (Since 1.10.0)</a></h3><br/>
- [ ] Mockito: Better generic support with deep stubs (Since 1.10.0)</a></h3><br/>
- [ ] Mockito: Mockito JUnit rule (Since 1.10.17)
- [ ] Mockito: Switch `on` or `off` plugins (Since 1.10.15)
- [ ] Mockito: Custom verification failure message (Since 2.1.0)
- [ ] Mockito: Java 8 Lambda Matcher Support (Since 2.1.0)
- [ ] Mockito: Java 8 Custom Answer Support (Since 2.1.0)
- [ ] Mockito: Meta data and generic type retention (Since 2.1.0)
- [ ] Mockito: Mocking final types, enums and final methods (Since 2.1.0)
- [ ] Mockito: (*new*) Improved productivity and cleaner tests with "stricter" Mockito (Since 2.+)
- [ ] Mockito: (**new**) Advanced public API for framework integrations (Since 2.10.+)
- [ ] Mockito: (**new**) New API for integrations: listening on verification start events (Since 2.11.+)

...

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

... TBD ...
