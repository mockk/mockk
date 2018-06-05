
[![Gitter](https://badges.gitter.im/mockk-io/Lobby.svg)](https://gitter.im/mockk-io/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge)
[![Build Status](https://travis-ci.org/mockk/mockk.svg?branch=master)](https://travis-ci.org/mockk/mockk)
[![Relase Version](https://img.shields.io/maven-central/v/io.mockk/mockk.svg?label=release)](http://search.maven.org/#search%7Cga%7C1%7Cmockk)
[![Change log](https://img.shields.io/badge/change%20log-%E2%96%A4-yellow.svg)](https://github.com/mockk/mockk/releases)
[![Matrix tests](https://img.shields.io/badge/matrix-test-e53994.svg)](http://mockk.io/MATRIX)
[![codecov](https://codecov.io/gh/mockk/mockk/branch/master/graph/badge.svg)](https://codecov.io/gh/mockk/mockk) 
[![Documentation](https://img.shields.io/badge/documentation-%E2%86%93-yellowgreen.svg)](#nice-features) 
[![GitHub stars](https://img.shields.io/github/stars/mockk/mockk.svg?label=stars)](https://github.com/mockk/mockk)


It is important to make right decisions about deprecation of some functionality. 
Otherwise lib code or user code may become a mess. 
I know it may be inconvenient for end user, but here sooner is better than later.

### Scoped mocking

From the beginning syntax of scoped mocking was weird. But not only that. 
More and more I am finding that people misusing it in code I see through GH, Gitter and Slack.
This misuse exposes tests to errors and dependency.

<table>
<thead>
<tr><th>Old syntax</th><th>New syntax</th><th>Annotation</th></tr>
</thead>
<tbody>
<tr>
<td>
<pre>
objectMockk(Obj).use {
   // mocking, usage, verification
}
</pre>
</td>
<td>
<pre>
mockkObject(Obj)
// mocking, usage, verification
</pre>
</td>
<td>

<code>mockkObject</code> will automatically clear mock before usage. 
It is safe to use it alone without <code>clearing</code> or <code>unmocking</code>

</td>
</tr>

<tr>
<td>
<pre>
staticMockk&lt;Cls&gt;().use {
   // mocking, usage, verification
}
</pre>
</td>
<td>
<pre>
mockkStatic(Cls::class)
// mocking, usage, verification
</pre>
</td>
<td>

<code>mockkStatic</code> will automatically clear mock before usage. 
It is safe to use it alone without <code>clearing</code> or <code>unmocking</code>

</td>
</tr>
</tbody>
</table>

So basically there is no scopes, `mock`, `unmock` or `use`. 
There is just one call that creates a mock or clears if it is already created.
It should be safe to have only that one declaration in test before using mock.
