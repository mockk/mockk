---
title: MockK Android support
description: MockK supports regular unit tests, Android instrumented tests via subclassing(< Android P) and Android instrumented tests via inlining(≥ Android P)
---
![mockk](doc/logo-site.png) ![kotlin](doc/robot.png)

## Support

MockK supports:
 
 * regular unit tests
 * Android instrumented tests via subclassing(< Android P)
 * Android instrumented tests via inlining(≥ Android P)

## DexOpener

To open classes before Android P you can use [DexOpener](https://github.com/tmurakami/dexopener), [example](https://github.com/tmurakami/dexopener/tree/master/examples/mockk)

## Implementation

Implementation is based on [dexmaker](https://github.com/linkedin/dexmaker) project. With Android P, instrumentation tests may use full power of inline instrumentation, so object mocks, static mocks and mocking of final classes are supported. Before Android P, only subclassing can be employed, so you will need the 'all-open' plugin.

Unfortunately, public CIs such as Travis and Circle are not supporting Android P emulation due to the absence of ARM Android P images. Hopefully, this will change soon.
 
## Supported features

<table>
    <thead>
    <tr>
        <th>Feature</th>
        <th>Unit tests</th>
        <th colspan="2">Instrumentation test</th>
    </tr>
    <tr>
        <td></td>
        <td></td>
        <td>&lt; Android P</td>
        <td>≥ Android P</td>
    </tr>
    </thead>
    <tbody>
    <tr>
        <td>annotations</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>mocking final classes</td>
        <td>✓</td>
        <td>can use DexOpener</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>pure Kotlin mocking DSL</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>matchers partial specification</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>chained calls</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>matcher expressions</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>mocking coroutines</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>capturing lambdas</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>object mocks</td>
        <td>✓</td>
        <td></td>
        <td>✓</td>
    </tr>
    <tr>
        <td>private function mocking</td>
        <td>✓</td>
        <td></td>
        <td>✓</td>
    </tr>
    <tr>
        <td>property backing field access</td>
        <td>✓</td>
        <td>✓</td>
        <td>✓</td>
    </tr>
    <tr>
        <td>extension function mocking (static mocks)</td>
        <td>✓</td>
        <td></td>
        <td>✓</td>
    </tr>
    <tr>
        <td>constructor mocking</td>
        <td>✓</td>
        <td></td>
        <td>✓</td>
    </tr>
    </tbody>
</table>

## Installation

All you need to get started is by adding a dependency to `MockK` library.

#### Unit tests

```
testImplementation "io.mockk:mockk:{version}"
```

#### Android instrumented tests

```
androidTestImplementation "io.mockk:mockk-android:{version}"
```

<img align="middle" src="https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=current+version" alt="current version" />

### Documentation

Check [full documentation](https://mockk.io#markdown-toc) here
