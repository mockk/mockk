# Known Issues

* PowerMock needs a workaround to run together with MockK [#79](https://github.com/mockk/mockk/issues/79#issuecomment-437646333). (not sure after workaround if it is generally usable or not, please somebody report it)
* Inline functions cannot be mocked: see the discussion on [this issue](https://github.com/mockk/mockk/issues/27)
* Spies, `mockkStatic` may not work on JDK 16+; `InaccessibleObjectException`/`IllegalAccessException`: [read more here](jdk16-access-exceptions.md)
* Using a spy with a suspending function [will give unexpected test results](https://github.com/mockk/mockk/issues/554)
