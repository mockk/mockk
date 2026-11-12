package io.mockk.it

// The facade class of this file is never statically mocked anywhere in the test suite,
// so confirmVerified on its callables is expected to fail with "can't find stub".
infix fun Int.unmockedOp(b: Int) = this + b

val unmockedTopLevelProperty: Int get() = 42
