# JDK 16+ access exceptions

### Problem symptoms 
On JDK 16 and above you may encounter `InaccessibleObjectException` or `IllegalAccessException` in the following known cases:
1. _Some_ usages of `mockkStatic` on Java standard library classes, e.g. 
   ```kotlin
   mockkStatic(Instant::class)
   every { Instant.now() } returns Instant.parse("2022-09-15T09:31:26.919Z")
   ```
   ```
   java.time.format.DateTimeParseException: Text '2022-09-15T09:31:26.919Z' could not be parsed:
     Unable to make private static java.time.Instant java.time.Instant.create(long,int) accessible: 
     module java.base does not "opens java.time" to unnamed module @4501b7af
   ```

2. Spying on Java standard library classes, e.g.
   ```kotlin
   val socket = Socket()
   val spy = spyk<Socket>(socket)
   ```
    Note: this includes creating `@SpyKBean` on spring data repository instances, because they are instances of `java.lang.reflect.Proxy` in fact.
   ```
   Error creating bean with name 'featureRepository': Post-processing of FactoryBean's singleton object failed;
    nested exception is java.lang.IllegalAccessException: class io.mockk.impl.InternalPlatform 
    cannot access a member of class java.lang.reflect.Proxy (in module java.base) with modifiers "protected"
   ```

### Problem cause
JDK 16 enforces strong encapsulation of standard modules. In practice this mean overriding accessibility modifiers in JDK classes (for example,  `privateMember.setAccessible(true)`) is forbidden.

https://blogs.oracle.com/javamagazine/post/a-peek-into-java-17-continuing-the-drive-to-encapsulate-the-java-runtime-internals

### Solution / workaround
Add JVM argument `--add-opens java.base/full.package.name=ALL-UNNAMED` _for each package_ you need to mock. Package name should be fully qualified.  
Description: https://docs.oracle.com/en/java/javase/16/migrate/migrating-jdk-8-later-jdk-releases.html

Example for Gradle users:
```groovy
tasks.test {
    jvmArgs(
        "--add-opens", "java.base/java.time=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
    )
}
```

Example for Gradle users within an Android module:
```groovy
android {
    testOptions {
        unitTests.all {
            jvmArgs(
                "--add-opens", "java.base/java.time=ALL-UNNAMED",
                "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED"
            )
        }
    }
}
```

Example for Maven users:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>
            --add-opens java.base/java.time=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
        </argLine>
    </configuration>
</plugin>
```

### Linked issues
* https://github.com/mockk/mockk/issues/681
* https://github.com/Ninja-Squad/springmockk/issues/65
* https://github.com/mockk/mockk/issues/929
* https://github.com/mockk/mockk/issues/864
* https://github.com/mockk/mockk/issues/834
