# Settings file

To adjust parameters globally, there are a few settings you can specify in a resource file.

How to use:
1. Create a `io/mockk/settings.properties` file in `src/test/resources`.
2. Put any of the following options:
```properties
relaxed=true|false
relaxUnitFun=true|false
recordPrivateCalls=true|false
stackTracesOnVerify=true|false
stackTracesAlignment=left|center
failOnSetBackingFieldException=true|false
```

* `stackTracesAlignment` determines whether to align the stack traces to the center (default),
  or to the left (more consistent with usual JVM stackTraces).
* If `failOnSetBackingFieldException` (`false` by default) is set to `true`, tests fail if a
  backing field could not be set. Otherwise, only the warning "Failed to set backing field" will be logged.
  See [here](https://github.com/mockk/mockk/issues/1291) for more details.
