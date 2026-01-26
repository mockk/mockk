# Advanced

## Multiple interfaces

Adding additional behaviours via interfaces and stubbing them:

```kotlin
val spy = spyk(System.out, moreInterfaces = arrayOf(Runnable::class))

spy.println(555)

every {
    (spy as Runnable).run()
} answers {
    (self as PrintStream).println("Run! Run! Run!")
}

val thread = Thread(spy as Runnable)
thread.start()
thread.join()
```

## Mocking Nothing

Nothing special here. If you have a function returning `Nothing`:

```kotlin
fun quit(status: Int): Nothing {
    exitProcess(status)
}
```

Then you can for example throw an exception as behaviour:

```kotlin
every { quit(1) } throws Exception("this is a test")
```

## Clearing vs Unmocking

* clear - deletes the internal state of objects associated with a mock, resulting in an empty object
* unmock - re-assigns transformation of classes back to original state prior to mock

## Scoped mocks

A Scoped mock is a mock that automatically unmocks itself after the code block passed as a parameter has been executed.
You can use the `mockkObject`, `mockkStatic` and `mockkConstructor` functions.

```kotlin
object ObjBeingMocked {
 fun add(a: Int, b: Int) = a + b
}

// ObjBeingMocked will be unmocked after this scope
mockkObject(ObjBeingMocked) {
 assertEquals(3, ObjBeingMocked.add(1, 2))
 every { ObjBeingMocked.add(1, 2) } returns 55
 assertEquals(55, ObjBeingMocked.add(1, 2))
}
```

## Suppressing superclass calls

To suppress a method call, especially a `super` call inside an overridden method, you can stub its behavior on a `spyk`.
Using `every { ... } just runs` replaces the entire method body, preventing the original code from executing.

This is particularly useful for users coming from frameworks like PowerMockito or for testing classes like Android Activities.
For reference, see [PowerMockito suppress documentation](https://github.com/powermock/powermock/wiki/Suppress-Unwanted-Behavior).

```kotlin
// A simple inheritance hierarchy
open class Parent {
    var superCalled = false
    open fun doWork() {
        superCalled = true
    }
}

class Child : Parent() {
    override fun doWork() {
        super.doWork() // We want to suppress this call during testing
    }
}

// In your test:
val child = spyk<Child>()

// Stub the method: prevents the original (super) implementation from being invoked
every { child.doWork() } just runs

child.doWork()

// Verify that the superclass method was not executed
assertFalse(child.superCalled)
```

This approach allows you to isolate the logic within your method for unit testing without executing unwanted parent class behavior.
