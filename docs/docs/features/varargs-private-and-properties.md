# Varargs, Private Calls, and Properties

## Varargs

Extended vararg handling:

```kotlin
interface ClsWithManyMany {
    fun manyMany(vararg x: Any): Int
}

val obj = mockk<ClsWithManyMany>()

every { obj.manyMany(5, 6, *varargAll { it == 7 }) } returns 3

println(obj.manyMany(5, 6, 7)) // 3
println(obj.manyMany(5, 6, 7, 7)) // 3
println(obj.manyMany(5, 6, 7, 7, 7)) // 3

every { obj.manyMany(5, 6, *anyVararg(), 7) } returns 4

println(obj.manyMany(5, 6, 1, 7)) // 4
println(obj.manyMany(5, 6, 2, 3, 7)) // 4
println(obj.manyMany(5, 6, 4, 5, 6, 7)) // 4

every { obj.manyMany(5, 6, *varargAny { nArgs > 5 }, 7) } returns 5

println(obj.manyMany(5, 6, 4, 5, 6, 7)) // 5
println(obj.manyMany(5, 6, 4, 5, 6, 7, 7)) // 5

every {
    obj.manyMany(5, 6, *varargAny {
        if (position < 3) it == 3 else it == 4
    }, 7)
} returns 6

println(obj.manyMany(5, 6, 3, 4, 7)) // 6
println(obj.manyMany(5, 6, 3, 4, 4, 7)) // 6
```

## Private functions mocking / dynamic calls

If you need to mock private functions, you can do it via a dynamic call.
```kotlin
class Car {
    fun drive() = accelerate()

    private fun accelerate() = "going faster"
}

val mock = spyk<Car>(recordPrivateCalls = true)

every { mock["accelerate"]() } returns "going not so fast"

assertEquals("going not so fast", mock.drive())

verifySequence {
    mock.drive()
    mock["accelerate"]()
}
```

If you want to verify private calls, you should create a `spyk` with `recordPrivateCalls = true`

Additionally, a more verbose syntax allows you to get and set properties, combined with the same dynamic calls:

```kotlin
val mock = spyk(Team(), recordPrivateCalls = true)

every { mock getProperty "speed" } returns 33
every { mock setProperty "acceleration" value less(5) } just runs
justRun { mock invokeNoArgs "privateMethod" }
every { mock invoke "openDoor" withArguments listOf("left", "rear") } returns "OK"

verify { mock getProperty "speed" }
verify { mock setProperty "acceleration" value less(5) }
verify { mock invoke "openDoor" withArguments listOf("left", "rear") }
```

## Property backing fields

You can access the backing fields via `fieldValue` and use `value` for the value being set.

::: info
In the examples below, we use `propertyType` to specify the type of the `fieldValue`.
This is needed because it is possible to capture the type automatically for the getter.
Use `nullablePropertyType` to specify a nullable type.
:::
::: info
This is only for public fields. It is nearly impossible to mock private properties as they don't have getter methods attached. Use Java reflection to make the field accessible or use `@VisibleForTesting` annotation in the source.
:::
```kotlin
val mock = spyk(MockCls(), recordPrivateCalls = true)

every { mock.property } answers { fieldValue + 6 }
every { mock.property = any() } propertyType Int::class answers { fieldValue += value }
every { mock getProperty "property" } propertyType Int::class answers { fieldValue + 6 }
every { mock setProperty "property" value any<Int>() } propertyType Int::class answers  { fieldValue += value }
every {
    mock.property = any()
} propertyType Int::class answers {
    fieldValue = value + 1
} andThen {
    fieldValue = value - 1
}
```
