![mockk](doc/logo-site.png) ![kotlin](doc/robot.png)

[![Gitter](https://badges.gitter.im/mockk-io/Lobby.svg)](https://gitter.im/mockk-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge) [![Build Status](https://travis-ci.org/mockk/mockk.svg?branch=master)](https://travis-ci.org/mockk/mockk) [![Release Version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=release)](http://search.maven.org/#search%7Cga%7C1%7Cmockk)  [![Change log](https://img.shields.io/badge/change%20log-%E2%96%A4-yellow.svg)](https://github.com/mockk/mockk/releases) [![Back log](https://img.shields.io/badge/back%20log-%E2%96%A4-orange.svg)](/BACKLOG) [![codecov](https://codecov.io/gh/mockk/mockk/branch/master/graph/badge.svg)](https://codecov.io/gh/mockk/mockk) [![Documentation](https://img.shields.io/badge/documentation-%E2%86%93-yellowgreen.svg)](#nice-features)

Table of contents:

* auto-gen TOC:
{:toc}

## Supported features

|Feature|Unit tests <td colspan=2>Instrumetation test</td>
| ---  --- | --- | --- |
|       |           |before Android P|Android P and later|
|annotations| ✓ | ✓ | ✓ |
|mocking final classes| ✓ | | ✓ |
|pure Kotlin mocking DSL| ✓ | ✓ | ✓ |
|matchers partial specification| ✓ | ✓ | ✓ |
|chained calls| ✓ | ✓ | ✓ |
|matcher expressions| ✓ | ✓ | ✓ |
|mocking coroutines| ✓ | ✓ | ✓ |
|capturing lambdas| ✓ | ✓ | ✓ |
|object mocks| ✓ | | ✓ |
|private function mocking| ✓ | ✓ | ✓ |
|property backing field access| ✓ | ✓ | ✓ |
|extension function mocking (static mocks)| ✓ | | ✓ |

## Installation

All you need to get started is just to add a dependency to `MockK` library.

#### Unit tests

```
testImplementation "io.mockk:mockk:{version}"
```

#### Android instrumentation tests

```
androidTestImplementation "io.mockk:mockk-android:{version}"
```

<img align="middle" src="https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=current+version" alt="current version" />
