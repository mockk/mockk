Besides [documentation](mockk.io) you can find many other examples here. 
Fill free to submit pull request, it is really easy to do.

Table of contents:

* auto-gen TOC:
{:toc}

#### Custom matcher to match list without order
```kotlin
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

```
