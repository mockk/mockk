# DSL Reference

Here are a few tables to help you master the DSL.

## Top level functions

| Function                  | Description                                                                                                |
|---------------------------|------------------------------------------------------------------------------------------------------------|
| `mockk<T>(...)`           | builds a regular mock                                                                                      |
| `spyk<T>()`               | builds a spy using the default constructor                                                                 |
| `spyk(obj)`               | builds a spy by copying from `obj`                                                                         |
| `slot`                    | creates a capturing slot                                                                                   |
| `every`                   | starts a stubbing block                                                                                    |
| `coEvery`                 | starts a stubbing block for coroutines                                                                     |
| `verify`                  | starts a verification block                                                                                |
| `coVerify`                | starts a verification block for coroutines                                                                 |
| `verifyAll`               | starts a verification block that should include all calls                                                  |
| `coVerifyAll`             | starts a verification block that should include all calls for coroutines                                   |
| `verifyOrder`             | starts a verification block that checks the order                                                          |
| `coVerifyOrder`           | starts a verification block that checks the order for coroutines                                           |
| `verifySequence`          | starts a verification block that checks whether all calls were made in a specified sequence                |
| `coVerifySequence`        | starts a verification block that checks whether all calls were made in a specified sequence for coroutines |
| `excludeRecords`          | exclude some calls from being recorded                                                                     |
| `confirmVerified`         | confirms that all recorded calls were verified                                                             |
| `checkUnnecessaryStub`    | confirms that all recorded calls are used at least once                                                    |
| `clearMocks`              | clears specified mocks                                                                                     |
| `registerInstanceFactory` | allows you to redefine the way of instantiation for certain object                                         |
| `mockkClass`              | builds a regular mock by passing the class as parameter                                                    |
| `mockkObject`             | turns an object into an object mock, or clears it if was already transformed                               |
| `unmockkObject`           | turns an object mock back into a regular object                                                            |
| `mockkStatic`             | makes a static mock out of a class, or clears it if it was already transformed                             |
| `unmockkStatic`           | turns a static mock back into a regular class                                                              |
| `clearStaticMockk`        | clears a static mock                                                                                       |
| `mockkConstructor`        | makes a constructor mock out of a class, or clears it if it was already transformed                        |
| `unmockkConstructor`      | turns a constructor mock back into a regular class                                                         |
| `clearConstructorMockk`   | clears the constructor mock                                                                                |
| `unmockkAll`              | unmocks object, static and constructor mocks                                                               |
| `clearAllMocks`           | clears regular, object, static and constructor mocks                                                       |
| `clearAllStubsFromMemory` | removes all mocks from the internal collection storing them, and as a result frees up memory.              |


## Matchers

By default, simple arguments are matched using `eq()`

| Matcher                                                 | Description                                                                                            |
|---------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| `any<T>()`                                              | matches any argument of type `T` (type-checked)                                                        |
| `any(Class)`                                            | matches any argument of the given Class (type-checked; useful for reflective mocking)                  |
| `allAny()`                                              | special matcher that uses `any()` instead of `eq()` for matchers that are provided as simple arguments |
| `isNull()`                                              | checks if the value is null                                                                            |
| `isNull(inverse=true)`                                  | checks if the value is not null                                                                        |
| `ofType(type)`                                          | checks if the value belongs to the type                                                                |
| `match { it.startsWith("string") }`                     | matches via the passed predicate                                                                       |
| `coMatch { it.startsWith("string") }`                   | matches via the passed coroutine predicate                                                             |
| `matchNullable { it?.startsWith("string") }`            | matches nullable value via the passed predicate                                                        |
| `coMatchNullable { it?.startsWith("string") }`          | matches nullable value via the passed coroutine predicate                                              |
| `eq(value)`                                             | matches if the value is equal to the provided value via the `deepEquals` function                      |
| `eq(value, inverse=true)`                               | matches if the value is not equal to the provided value via the `deepEquals` function                  |
| `neq(value)`                                            | matches if the value is not equal to the provided value via the `deepEquals` function                  |
| `refEq(value)`                                          | matches if the value is equal to the provided value via reference comparison                           |
| `refEq(value, inverse=true)`                            | matches if the value is not equal to the provided value via reference comparison                       |
| `nrefEq(value)`                                         | matches if the value is not equal to the provided value via reference comparison                       |
| `cmpEq(value)`                                          | matches if the value is equal to the provided value via the `compareTo` function                       |
| `less(value)`                                           | matches if the value is less than the provided value via the `compareTo` function                      |
| `more(value)`                                           | matches if the value is more than the provided value via the `compareTo` function                      |
| `less(value, andEquals=true)`                           | matches if the value is less than or equal to the provided value via the `compareTo` function          |
| `more(value, andEquals=true)`                           | matches if the value is more than or equal to the provided value via the `compareTo` function          |
| `range(from, to, fromInclusive=true, toInclusive=true)` | matches if the value is in range via the `compareTo` function                                          |
| `and(left, right)`                                      | combines two matchers via a logical and                                                                |
| `or(left, right)`                                       | combines two matchers via a logical or                                                                 |
| `not(matcher)`                                          | negates the matcher                                                                                    |
| `capture(slot)`                                         | captures a Non Nullable value to a `CapturingSlot`                                                     |
| `captureNullable(slot)`                                 | captures a Nullable value to a `CapturingSlot`                                                         |
| `capture(mutableList)`                                  | captures a value to a list                                                                             |
| `captureNullable(mutableList)`                          | captures a value to a list together with null values                                                   |
| `captureLambda()`                                       | captures a lambda                                                                                      |
| `captureCoroutine()`                                    | captures a coroutine                                                                                   |
| `invoke(...)`                                           | calls a matched argument                                                                               |
| `coInvoke(...)`                                         | calls a matched argument for a coroutine                                                               |
| `hint(cls)`                                             | hints the next return type in case it's gotten erased                                                  |
| `anyVararg()`                                           | matches any elements in a vararg                                                                       |
| `varargAny(matcher)`                                    | matches if any element matches the matcher                                                             |
| `varargAll(matcher)`                                    | matches if all elements match the matcher                                                              |
| `any...Vararg()`                                        | matches any elements in vararg (specific to primitive type)                                            |
| `varargAny...(matcher)`                                 | matches if any element matches the matcher (specific to the primitive type)                            |
| `varargAll...(matcher)`                                 | matches if all elements match the matcher (specific to the primitive type)                             |

A few special matchers available in verification mode only:

| Matcher                      | Description                                                          |
|------------------------------|----------------------------------------------------------------------|
| `withArg { code }`           | matches any value and allows to execute some code                    |
| `withNullableArg { code }`   | matches any nullable value and allows to execute some code           |
| `coWithArg { code }`         | matches any value and allows to execute some coroutine code          |
| `coWithNullableArg { code }` | matches any nullable value and allows to execute some coroutine code |

## Validators

| Validator                                       | Description                                                                                     |
|-------------------------------------------------|-------------------------------------------------------------------------------------------------|
| `verify { mock.call() }`                        | Do unordered verification that a call was performed                                             |
| `verify(inverse=true) { mock.call() }`          | Do unordered verification that a call was not performed                                         |
| `verify(atLeast=n) { mock.call() }`             | Do unordered verification that a call was performed at least `n` times                          |
| `verify(atMost=n) { mock.call() }`              | Do unordered verification that a call was performed at most `n` times                           |
| `verify(exactly=n) { mock.call() }`             | Do unordered verification that a call was performed exactly `n` times                           |
| `verifyAll { mock.call1(); mock.call2() }`      | Do unordered verification that only the specified calls were executed for the mentioned mocks   |
| `verifyOrder { mock.call1(); mock.call2() }`    | Do verification that the sequence of calls went one after another                               |
| `verifySequence { mock.call1(); mock.call2() }` | Do verification that only the specified sequence of calls were executed for the mentioned mocks |
| `verify { mock wasNot Called }`                 | Do verification that a mock was not called                                                      |
| `verify { listOf(mock1, mock2) wasNot Called }` | Do verification that a list of mocks were not called                                            |

## Answers

An Answer can be followed up by one or more additional answers.

| Answer                       | Description                                                                                                        |
|------------------------------|--------------------------------------------------------------------------------------------------------------------|
| `returns value`              | specify that the matched call returns a specified value                                                            |
| `returnsMany list`           | specify that the matched call returns a value from the list, with subsequent calls returning the next element      |
| `returnsArgument(n)`         | specify that the matched call returns the nth argument of that call                                                |
| `throws ex`                  | specify that the matched call throws an exception                                                                  |
| `throwsMany list`            | specify that the matched call throws an exception from the list, with subsequent calls throwing the next exception |
| `answers { code }`           | specify that the matched call answers with a code block scoped with `answer scope`                                 |
| `coAnswers { code }`         | specify that the matched call answers with a coroutine code block  with `answer scope`                             |
| `answers answerObj`          | specify that the matched call answers with an Answer object                                                        |
| `answers { nothing }`        | specify that the matched call answers null                                                                         |
| `just Runs`                  | specify that the matched call is returning Unit (returns null)                                                     |
| `just Awaits`                | specify that the matched call never returns (available since v1.13.3)                                              |
| `propertyType Class`         | specify the type of the backing field accessor                                                                     |
| `nullablePropertyType Class` | specify the type of the backing field accessor as a nullable type                                                  |


## Additional answer(s)

A next answer is returned on each consequent call and the last value is persisted.
So this is similar to the `returnsMany` semantics.

| Additional answer         | Description                                                                                                        |
|---------------------------|--------------------------------------------------------------------------------------------------------------------|
| `andThen value`           | specify that the matched call returns one specified value                                                          |
| `andThenMany list`        | specify that the matched call returns a value from the list, with subsequent calls returning the next element      |
| `andThenThrows ex`        | specify that the matched call throws an exception                                                                  |
| `andThenThrowsMany ex`    | specify that the matched call throws an exception from the list, with subsequent calls throwing the next exception |
| `andThen { code }`        | specify that the matched call answers with a code block scoped with `answer scope`                                 |
| `coAndThen { code }`      | specify that the matched call answers with a coroutine code block with `answer scope`                              |
| `andThenAnswer answerObj` | specify that the matched call answers with an Answer object                                                        |
| `andThen { nothing }`     | specify that the matched call answers null                                                                         |
| `andThenJust Runs`        | specify that the matched call is returning Unit (available since v1.12.2)                                          |
| `andThenJust Awaits`      | specify that the matched call is never returning (available since v1.13.3)                                         |

## Answer scope

| Parameter                     | Description                                                             |
|-------------------------------|-------------------------------------------------------------------------|
| `call`                        | a call object that consists of an invocation and a matcher              |
| `invocation`                  | contains information regarding the actual function invoked              |
| `matcher`                     | contains information regarding the matcher used to match the invocation |
| `self`                        | reference to the object invocation made                                 |
| `method`                      | reference to the function invocation made                               |
| `args`                        | reference to the invocation arguments                                   |
| `nArgs`                       | number of invocation arguments                                          |
| `arg(n)`                      | nth argument                                                            |
| `firstArg()`                  | first argument                                                          |
| `secondArg()`                 | second argument                                                         |
| `thirdArg()`                  | third argument                                                          |
| `lastArg()`                   | last argument                                                           |
| `captured()`                  | the last element in the list for convenience when capturing to a list   |
| `lambda<...>().invoke()`      | call the captured lambda                                                |
| `coroutine<...>().coInvoke()` | call the captured coroutine                                             |
| `nothing`                     | null value for returning `nothing` as an answer                         |
| `fieldValue`                  | accessor to the property backing field                                  |
| `fieldValueAny`               | accessor to the property backing field with `Any?` type                 |
| `value`                       | value being set, cast to the same type as the property backing field    |
| `valueAny`                    | value being set, with `Any?` type                                       |
| `callOriginal`                | calls the original function                                             |

## Vararg scope

| Parameter  | Description                                   |
|------------|-----------------------------------------------|
| `position` | the position of an argument in a vararg array |
| `nArgs`    | overall count of arguments in a vararg array  |
