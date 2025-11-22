# Static and Extension Functions

## Top Level functions

Kotlin lets you declare functions that don’t belong to any class or object, called top-level functions. These calls are translated to static methods in `jvm` environments, and a special Java class is generated to hold the functions. These top-level functions can be mocked using `mockkStatic`. You just need to import the function and pass a reference as the argument:

```kotlin
import com.cars.buildCar

val testCar = Car()
mockkStatic(::buildCar)
every { buildCar() } returns testCar

assertEquals(testCar, buildCar())

verify { buildCar() }
```

Mocking a function will clear any existing mocks of other functions declared in the same file, equivalent to calling `clearStaticMockk` on the generated enclosing class.

## Extension functions

There are three types of extension function in Kotlin:

* class-wide
* object-wide
* module-wide

For an object or a class, you can mock extension functions just by creating a regular `mockk`:

```kotlin
data class Obj(val value: Int)

class Ext {
    fun Obj.extensionFunc() = value + 5
}

with(mockk<Ext>()) {
    every {
        Obj(5).extensionFunc()
    } returns 11

    assertEquals(11, Obj(5).extensionFunc())

    verify {
        Obj(5).extensionFunc()
    }
}
```

To mock module-wide extension functions you need to
build `mockkStatic(...)` with the module's class name as an argument.
For example "pkg.FileKt" for module `File.kt` in the `pkg` package.

```kotlin
data class Obj(val value: Int)

// declared in File.kt ("pkg" package)
fun Obj.extensionFunc() = value + 5

mockkStatic("pkg.FileKt")

every {
    Obj(5).extensionFunc()
} returns 11

assertEquals(11, Obj(5).extensionFunc())

verify {
    Obj(5).extensionFunc()
}
```

In `jvm` environments you can replace the class name with a function reference:
```kotlin
mockkStatic(Obj::extensionFunc)
```
Note that this will mock the whole `pkg.FileKt` class, and not just `extensionFunc`.

This syntax also applies for extension properties:
```kotlin
val Obj.squareValue get() = value * value

mockkStatic(Obj::squareValue)
```

If `@JvmName` is used, specify it as a class name.

KHttp.kt:
```kotlin
@file:JvmName("KHttp")

package khttp
// ... KHttp code
```

Testing code:
```kotlin
mockkStatic("khttp.KHttp")
```

Sometimes you need to know a little bit more to mock an extension function.
For example the extension function `File.endsWith()` has a totally unpredictable `classname`:
```kotlin
mockkStatic("kotlin.io.FilesKt__UtilsKt")
every { File("abc").endsWith(any<String>()) } returns true
println(File("abc").endsWith("abc"))
```
This is standard Kotlin behaviour that may be unpredictable.
Use `Tools -> Kotlin -> Show Kotlin Bytecode` or check `.class` files in JAR archive to detect such names.
