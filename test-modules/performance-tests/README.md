This is a performance testing setup for `mockk` using [JMH](https://github.com/openjdk/jmh).

### How to run

There are 2 ways to execute the benchmark code

Basic with Gradle

```shell
./gradlew benchmark
```

It will use default configuration set in  [build.gradle.kts](build.gradle.kts) and print out the results

#### Advanced with JMH binary

If you want access to more JMH features you can

```shell
./gradlew mainBenchmarkJar
```

to build the `.jar` file containing the benchmark code. It will be saved to `./build/benchmarks/main/jars` directory.

From there you can run JMH with all the JMH-specific configuration options form CLI (even on different machine).

```shell
java -jar ./build/benchmarks/main/jars/mockk-performance-main-jmh-<version>.jar -w 1 -i 2 -r 60s  -prof jfr
```

which will attach the JFR profiler (not possible with `kontlinx-benchmark` executed from Gradle)
