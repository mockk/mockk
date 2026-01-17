# Installation

All you need to get started is to add a dependency to the MockK library.

[![Latest Version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=Latest+Version)](https://central.sonatype.com/artifact/io.mockk/mockk)

## Gradle
```kotlin
testImplementation("io.mockk:mockk:LATEST")
```

## Maven
```xml
<dependency>
    <groupId>io.mockk</groupId>
    <artifactId>mockk-jvm</artifactId>
    <version>LATEST</version>
    <scope>test</scope>
</dependency>
```

## Android Unit
```kotlin
testImplementation("io.mockk:mockk-android:LATEST")
testImplementation("io.mockk:mockk-agent:LATEST")
```

## Android Instrumentation
```kotlin
androidTestImplementation("io.mockk:mockk-android:LATEST")
androidTestImplementation("io.mockk:mockk-agent:LATEST")
```

## Kotlin version
From version 1.13.0 MockK supports Kotlin 1.4 and higher.
