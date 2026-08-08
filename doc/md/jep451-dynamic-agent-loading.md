# JEP 451 dynamic agent loading warnings (JDK 21+)

## Problem symptoms

Running tests that use MockK on JDK 21 or newer prints a warning similar to:

```
WARNING: A Java agent has been loaded dynamically (/home/user/.gradle/caches/.../byte-buddy-agent-1.14.19.jar)
WARNING: If a serviceability tool is in use, please run with -XX:+EnableDynamicAgentLoading to hide this warning
WARNING: If a serviceability tool is not in use, please run with -Djdk.instrument.traceUsage for more information
WARNING: Dynamic loading of agents will be disallowed by default in a future release
```

Tests still pass — the warning is informational for now. However, a future JDK release
will disallow dynamic agent loading by default, at which point MockK will fail to
initialize its inline mock maker unless one of the workarounds below is applied.

## Problem cause

MockK uses the [Byte Buddy](https://bytebuddy.net) agent to instrument classes. This is
required to mock final classes (which most Kotlin classes are), and for `mockkStatic`,
`mockkObject` and `mockkConstructor`. By default the agent is attached to the already
running JVM via `ByteBuddyAgent.install()`.

[JEP 451](https://openjdk.org/jeps/451) (delivered in JDK 21, part of the OpenJDK
"integrity by default" effort) issues a warning whenever an agent is loaded into a
running JVM, in preparation for disallowing this by default. Loading agents at JVM
startup via `-javaagent` is explicitly *not* affected and remains fully supported.

## Solution / workaround

Preferred solution: load the Byte Buddy agent at JVM startup with `-javaagent`.
`ByteBuddyAgent.install()` detects the already-installed instrumentation and skips the
dynamic attach, so no warning is issued and nothing else changes.

Keep the `byte-buddy-agent` version aligned with the version MockK pulls in
transitively (check with `./gradlew dependencies --configuration testRuntimeClasspath`
or `mvn dependency:tree`).

Gradle (Kotlin DSL):

```kotlin
val byteBuddyAgent: Configuration by configurations.creating

dependencies {
    byteBuddyAgent("net.bytebuddy:byte-buddy-agent:1.14.19")
}

tasks.withType<Test> {
    jvmArgs("-javaagent:${byteBuddyAgent.singleFile}")
}
```

Gradle (Groovy DSL):

```groovy
configurations {
    byteBuddyAgent
}

dependencies {
    byteBuddyAgent "net.bytebuddy:byte-buddy-agent:1.14.19"
}

test {
    jvmArgs "-javaagent:${configurations.byteBuddyAgent.singleFile}"
}
```

> **Gradle note — configuration cache and build cache.** `singleFile` above resolves the
> `byteBuddyAgent` configuration while the build is being configured. This is compatible with
> Gradle's configuration cache (the resolved path is stored in the entry and reused), but the
> resolution is re-paid on every configuration-cache miss, and the absolute jar path becomes part
> of the test task's `jvmArgs` input — so test tasks are not relocatable across machines for a
> shared remote build cache. If either matters to your build, wire the argument lazily instead:
>
> ```kotlin
> tasks.withType<Test>().configureEach {
>     val agentJar = byteBuddyAgent.elements.map { it.single().asFile.absolutePath }
>     jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-javaagent:${agentJar.get()}") })
> }
> ```
>
> (An argument provider without annotated properties is excluded from the task's input
> fingerprint, which is what restores relocatability — with the corollary that bumping the
> agent version alone will not re-run tests.)

Maven — add `byte-buddy-agent` as a test dependency, then let the dependency plugin
resolve its path into a property used by Surefire:

```xml
<dependency>
    <groupId>net.bytebuddy</groupId>
    <artifactId>byte-buddy-agent</artifactId>
    <version>1.14.19</version>
    <scope>test</scope>
</dependency>
```

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>properties</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-javaagent:${net.bytebuddy:byte-buddy-agent:jar}</argLine>
    </configuration>
</plugin>
```

Alternative: acknowledge dynamic loading explicitly by adding the JVM option
`-XX:+EnableDynamicAgentLoading` to the test JVM. This suppresses the warning and will
keep working as an explicit opt-in even after the default changes, but it opts the test
JVM out of the integrity guarantees rather than avoiding the dynamic attach.

## Related warning you may also see

```
OpenJDK 64-Bit Server VM warning: Sharing is only supported for boot loader classes because bootstrap classpath has been appended
```

This one is **not** fixed by `-javaagent` and is unrelated to JEP 451. MockK's inline
agent injects its dispatcher classes into the bootstrap class loader (via
`Instrumentation.appendToBootstrapClassLoaderSearch`) so that instrumented JDK classes
can reach mock handlers; a JVM whose Class Data Sharing (CDS) archive is active then
notes that non-boot classes can no longer use the shared archive. It is informational
and benign — the cost is a few milliseconds of startup in that test JVM. Whether it
appears at all depends on whether the CDS archive actually mapped in that particular
JVM, so it can show up in one environment (e.g. an IDE test runner) and not another
(e.g. a Gradle worker) running the same tests. Adding `-Xshare:off` to the test JVM
silences it, at the price of disabling CDS entirely.

## Linked issues

* [#1171](https://github.com/mockk/mockk/issues/1171) — dynamically loaded agent warning (JEP-451)
* [JEP 451: Prepare to Disallow the Dynamic Loading of Agents](https://openjdk.org/jeps/451)
* [Byte Buddy discussion #1535](https://github.com/raphw/byte-buddy/discussions/1535) — how to define the agent for a test run in Gradle and Maven
* [mockito/mockito#3037](https://github.com/mockito/mockito/issues/3037) — Mockito's handling of the same change
