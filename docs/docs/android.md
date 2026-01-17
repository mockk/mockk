# Android Support

MockK supports:

- regular unit tests
- Android instrumented tests via subclassing (< Android P)
- Android instrumented tests via inlining (≥ Android P)

## DexOpener

To open classes before Android P you can use [DexOpener](https://github.com/tmurakami/dexopener), [example](https://github.com/tmurakami/dexopener/tree/master/examples/mockk)

## Implementation   

Implementation is based on [dexmaker](https://github.com/linkedin/dexmaker) project. With Android P, instrumentation tests may use full power of inline instrumentation, so object mocks, static mocks and mocking of final classes are supported. Before Android P, only subclassing can be employed, so you will need the 'all-open' plugin.

Unfortunately, public CIs such as Travis and Circle are not supporting Android P emulation due to the absence of ARM Android P images. Hopefully, this will change soon.
 
## Supported features


| Feature                                      | Unit tests | Instrumentation test < Android P | Instrumentation test ≥ Android P |
|----------------------------------------------|------------|----------------------------------|----------------------------------|
| annotations                                  | ✓          | ✓                                | ✓                                |
| mocking final classes                        | ✓          | can use DexOpener                | ✓                                |
| pure Kotlin mocking DSL                      | ✓          | ✓                                | ✓                                |
| matchers partial specification               | ✓          | ✓                                | ✓                                |
| chained calls                                | ✓          | ✓                                | ✓                                |
| matcher expressions                          | ✓          | ✓                                | ✓                                |
| mocking coroutines                           | ✓          | ✓                                | ✓                                |
| capturing lambdas                            | ✓          | ✓                                | ✓                                |
| object mocks                                 | ✓          |                                  | ✓                                |
| private function mocking                     | ✓          |                                  | ✓                                |
| property backing field access                | ✓          | ✓                                | ✓                                |
| extension function mocking (static mocks)    | ✓          |                                  | ✓                                |
| constructor mocking                          | ✓          |                                  | ✓                                |
