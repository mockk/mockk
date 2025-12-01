# Get Started

## Project Configuration
All you need to get started is just to add a dependency to `MockK` library.

[![current version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=current+version)](https://central.sonatype.com/artifact/io.mockk/mockk)
### Gradle
```kt
testImplementation("io.mockk:mockk:${mockkVersion}")
```
### Maven
```xml
<dependency>
    <groupId>io.mockk</groupId>
    <artifactId>mockk-jvm</artifactId>
    <version>${mockkVersion}</version>
    <scope>test</scope>
</dependency>
```

### Android Unit
```kt
testImplementation("io.mockk:mockk-android:${mockkVersion}")
testImplementation("io.mockk:mockk-agent:${mockkVersion}")
```
### Android Implementation
```kt
androidTestImplementation("io.mockk:mockk-android:${mockkVersion}")
androidTestImplementation("io.mockk:mockk-agent:${mockkVersion}")
```

## Usage

Simplest example. By default, mocks are strict, so you need to provide some behaviour.

```kotlin
val car = mockk<Car>()

every { car.drive(Direction.NORTH) } returns Outcome.OK

car.drive(Direction.NORTH) // returns OK

verify { car.drive(Direction.NORTH) }

confirmVerified(car)
```

## Spring support
Was abandoned and is currently being rebuilt. Meanwhile use mockk() for MockBeans.

## Quarkus support

* [quarkus-mockk](https://github.com/quarkiverse/quarkus-mockk) adds support for mocking beans in Quarkus. Documentation can be found [here](https://quarkiverse.github.io/quarkiverse-docs/quarkus-mockk/dev/index.html)

## Kotlin version

From version 1.13.0 MockK supports Kotlin 1.4 and higher
