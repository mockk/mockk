# Backlog
- [+] get rid of !! as much as possible
- [+] captured lateinit var
- [+] discover more co...Matcher
- [+] proxy id only for external mocks
- [+] instantiation factory
- [+] better human readable exceptions (see below)
- [+] mock: extraInterfaces, mock name
- [+] running methods: TestNG
- [ ] documentation coEvery/coVerify/coAny
- [ ] **MILESTONE 1.3**
- [ ] Runner + JaCoCo
- [ ] JavaDoc
- [ ] CONTRIBUTE
- [ ] annotation mocking
- [ ] static methods
- [ ] extension function
- [ ] other JVMs
- [ ] Kotlin Javascript

## Tasks

### Human readable exceptions

1. Hint hinting
```
java.lang.ClassCastException: javassist.util.proxy.java_lang_Object_$$_jvst935_10 cannot be cast to org.jetbrains.ktor.util.ValuesMap

	at org.jetbrains.demo.thinkter.RegisterKtTest$testPostRegisterOk$1$1.invoke(RegisterKtTest.kt:51)
	at org.jetbrains.demo.thinkter.RegisterKtTest$testPostRegisterOk$1$1.invoke(RegisterKtTest.kt:20)
	at io.mockk.impl.MockKGatewayImpl.record(Gateway.kt:119)
	at io.mockk.impl.MockKGatewayImpl.every(Gateway.kt:64)
```
2. Casting matcher
```
    HttpStatusCode cannot be cast to PostThoughtToken (matcher casting)
```
3. Reporting matching calls for method/mockk

## Geo

Beware Kotlin is popular in Germany, Japan, India, USA and Brasil

## Just for reference

### Class transformation features
- PowerMock : Remove final modifier from a class, enum, static methods, methods and inner classes
- PowerMock : Remove final modifier from
- PowerMock : Make all constructor public
- PowerMock : Add additional constructor with specific parameter
- PowerMock : Clear static initialiser body
- PowerMock : Add body for native methods
- PowerMock : Set modifier to public for non-system package-private classes
- PowerMock : Insert calling mock repository for static methods
- PowerMock : Replace calling to fields by call to mock repository
- PowerMock : Replace constructor call by call to mock repository
- PowerMock : Replace call to system classes by call to mock repository
- PowerMock : Check size of method body after modification and replace method body by throwing exception if method body size more than allowed in java specification

### Mockito features
- Mockito: Mockito Android support</a></br/>
- Mockito: Configuration-free inline mock making</a></br/>
- Mockito: Let's verify some behaviour!
- Mockito: How about some stubbing?
- Mockito: Argument matchers
- Mockito: Verifying exact number of invocations / at least once / never
- Mockito: Stubbing void methods with exceptions
- Mockito: Verification in order
- Mockito: Making sure interaction(s) never happened on mock
- Mockito: Finding redundant invocations
- Mockito: Shorthand for mocks creation - `&#064;Mock` annotation
- Mockito: Stubbing consecutive calls (iterator-style stubbing)
- Mockito: Stubbing with callbacks
- Mockito: `doReturn()` `doThrow()` `doAnswer()` `doNothing()` `doCallRealMethod()` family of methods
- Mockito: Spying on real objects
- Mockito: Changing default return values of unstubbed invocations (Since 1.7)
- Mockito: Capturing arguments for further assertions (Since 1.8.0)
- Mockito: Real partial mocks (Since 1.8.0)
- Mockito: Resetting mocks (Since 1.8.0)
- Mockito: Troubleshooting & validating framework usage (Since 1.8.0)
- Mockito: Aliases for behavior driven development (Since 1.8.0)
- Mockito: Serializable mocks (Since 1.8.1)
- Mockito: New annotations: `&#064;Captor`, `&#064;Spy`, `&#064;InjectMocks` (Since 1.8.3)
- Mockito: Verification with timeout (Since 1.8.5)
- Mockito: Automatic instantiation of `&#064;Spies`, `&#064;InjectMocks` and constructor injection goodness (Since 1.9.0)
- Mockito: One-liner stubs (Since 1.9.0)
- Mockito: Verification ignoring stubs (Since 1.9.0)
- Mockito: Mocking details (Improved in 2.2.x)
- Mockito: Delegate calls to real instance (Since 1.9.5)
- Mockito: `MockMaker` API (Since 1.9.5)
- Mockito: BDD style verification (Since 1.10.0)
- Mockito: Spying or mocking abstract classes (Since 1.10.12, further enhanced in 2.7.13 and 2.7.14)
- Mockito: Mockito mocks can be `serialized` / `deserialized` across classloaders (Since 1.10.0)</a></h3><br/>
- Mockito: Better generic support with deep stubs (Since 1.10.0)</a></h3><br/>
- Mockito: Mockito JUnit rule (Since 1.10.17)
- Mockito: Switch `on` or `off` plugins (Since 1.10.15)
- Mockito: Custom verification failure message (Since 2.1.0)
- Mockito: Java 8 Lambda Matcher Support (Since 2.1.0)
- Mockito: Java 8 Custom Answer Support (Since 2.1.0)
- Mockito: Meta data and generic type retention (Since 2.1.0)
- Mockito: Mocking final types, enums and final methods (Since 2.1.0)
- Mockito: (*new*) Improved productivity and cleaner tests with "stricter" Mockito (Since 2.+)
- Mockito: (**new**) Advanced public API for framework integrations (Since 2.10.+)
- Mockito: (**new**) New API for integrations: listening on verification start events (Since 2.11.+)
