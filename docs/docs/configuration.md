# Configuration

To adjust parameters globally, you can specify settings in a configuration file.

1. Create the configuration file at `src/test/resources/mockk.properties`.
2. Add any of the following configuration options:

```properties
# MockK Settings
relaxed=true|false
relaxUnitFun=true|false
recordPrivateCalls=true|false
stackTracesOnVerify=true|false
stackTracesAlignment=left|center
failOnSetBackingFieldException=true|false

# Restricted Mocking Configuration (see Restricted Mocking section below)
mockk.restrictedClasses=com.example.MyClass,com.example.AnotherClass
mockk.throwExceptionOnBadMock=true|false
```

## Configuration Options

| **Property**                     | **Description**                                                                         | **Default Value** |
|----------------------------------|-----------------------------------------------------------------------------------------|-------------------|
| `relaxed`                        | Enable relaxed mocking globally                                                         | `false`           |
| `relaxUnitFun`                   | Enable relaxed mocking for Unit-returning functions                                     | `false`           |
| `recordPrivateCalls`             | Record private calls for verification                                                   | `false`           |
| `stackTracesOnVerify`            | Show stack traces on verification failures                                              | `true`            |
| `stackTracesAlignment`           | Align stack traces to `left` or `center`                                                | `center`          |
| `failOnSetBackingFieldException` | Fail tests if backing field cannot be set                                               | `false`           |
| `mockk.restrictedClasses`        | Add fully qualified names of classes to restrict from mocking (comma-separated)         | N/A               |
| `mockk.throwExceptionOnBadMock`  | Throw exception when mocking restricted classes (`true`), or log warning only (`false`) | `false`           |

**Notes:**
* `stackTracesAlignment` determines whether to align the stack traces to the center (default),
  or to the left (more consistent with usual JVM stackTraces).
* If `failOnSetBackingFieldException` is set to `true`, tests fail if a backing field could not be set.
  Otherwise, only the warning "Failed to set backing field" will be logged.
  See [here](https://github.com/mockk/mockk/issues/1291) for more details.

## Legacy Configuration

For backward compatibility, MockK also supports the legacy configuration file:
```
src/test/resources/io/mockk/settings.properties
```

If both files exist, `mockk.properties` takes precedence. The legacy location is deprecated and will be removed in a future version.
