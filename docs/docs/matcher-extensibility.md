# Matcher extensibility

A very simple way to create new matchers is by attaching a function
to `MockKMatcherScope` or `MockKVerificationScope` and using the `match` function:

```kotlin
fun MockKMatcherScope.seqEq(seq: Sequence<String>) = match<Sequence<String>> {
    it.toList() == seq.toList()
}
```

It's also possible to create more advanced matchers by implementing the `Matcher` interface.

## Custom matchers

Example of a custom matcher that compares list without order:

```kotlin 
@Test
fun test() {
    class MockCls {
        fun op(a: List<Int>) = a.reversed()
    }

    val mock = mockk<MockCls>()

    every { mock.op(any()) } returns listOf(5, 6, 9)

    println(mock.op(listOf(1, 2, 3)))

    verify { mock.op(matchListWithoutOrder(3, 2, 1)) }

}

data class ListWithoutOrderMatcher<T>(
    val expectedList: List<T>,
    val refEq: Boolean
) : Matcher<List<T>> {
    val map = buildCountsMap(expectedList, refEq)

    override fun match(arg: List<T>?): Boolean {
        if (arg == null) return false
        return buildCountsMap(arg, refEq) == map
    }

    private fun buildCountsMap(list: List<T>, ref: Boolean): Map<Any?, Int> {
        val map = mutableMapOf<Any?, Int>()

        for (item in list) {
            val key = when {
                item == null -> nullKey
                refEq -> InternalPlatform.ref(item)
                else -> item
            }
            map.compute(key, { _, value -> (value ?: 0) + 1 })
        }

        return map
    }

    override fun toString() = "matchListWithoutOrder($expectedList)"

    @Suppress("UNCHECKED_CAST")
    override fun substitute(map: Map<Any, Any>): Matcher<List<T>> {
        return copy(expectedList = expectedList.map { map.getOrDefault(it as Any?, it) } as List<T>)
    }

    companion object {
        val nullKey = Any()
    }
}

inline fun <reified T : List<E>, E : Any> MockKMatcherScope.matchListWithoutOrder(
    vararg items: E,
    refEq: Boolean = true
): T = match(ListWithoutOrderMatcher(listOf(*items), refEq))
```

## Reflection matchers

Example using reflection to mock all methods on a builder-style object

```kotlin
val builderFunctions = MyBuilder::class.memberFunctions.filter { it.returnType.classifier == MyBuilder::class }
val builderMock = mockk<MyBuilder> {
  builderFunctions.forEach { func ->
    every {
      val params = listOf<Any?>(builderMock) + func.parameters.drop(1).map { any(it.type.classifier as KClass<Any>) }
      func.call(*params.toTypedArray())
    } answers {
      this@mockk
    }
  }
}
```
