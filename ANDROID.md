![mockk](doc/logo-site.png) ![kotlin](doc/robot.png)

[![Gitter](https://badges.gitter.im/mockk-io/Lobby.svg)](https://gitter.im/mockk-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge)
[![Build Status](https://travis-ci.org/mockk/mockk.svg?branch=master)](https://travis-ci.org/mockk/mockk)
[![Relase Version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=release)](http://search.maven.org/#search%7Cga%7C1%7Cmockk)
[![Change log](https://img.shields.io/badge/change%20log-%E2%96%A4-yellow.svg)](https://github.com/mockk/mockk/releases)
[![Matrix tests](https://img.shields.io/badge/matrix-test-e53994.svg)](http://mockk.io/MATRIX)
[![codecov](https://codecov.io/gh/mockk/mockk/branch/master/graph/badge.svg)](https://codecov.io/gh/mockk/mockk) 
[![Documentation](https://img.shields.io/badge/documentation-%E2%86%93-yellowgreen.svg)](#nice-features) 
[![GitHub stars](https://img.shields.io/github/stars/mockk/mockk.svg?label=stars)](https://github.com/mockk/mockk)

## Support

MockK supports:
 
 * regular unit tests
 * Android instrumented tests via subclassing(< Android P)
 * Android instrumented tests via inlining(≥ Android P)

## Implementation

Implementation is based on [dexmaker](https://github.com/linkedin/dexmaker) project. With Anroid P instrumentation tests may use full power of inline instrumentation, so object mocks, static mocks and mocking of final classes are supported. Before Android P only subclassing can be employed and that means you need 'all-open' plugin.

Unfortunatelly public CIs alike Travis and Circle are not supporting emulation of Android P because of absense of ARM Anroid P images. Hope this will change soon
 
## Supported features

<table>
    <thead>
    <tr>
        <th>Feature</th>
        <th>Unit tests</th>
        <th colspan="2">Instrumetation test</th>
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
        <td></td>
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
        <td>✓</td>
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

All you need to get started is just to add a dependency to `MockK` library.

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

Check [full documentation](http://mockk.io#markdown-toc) here
