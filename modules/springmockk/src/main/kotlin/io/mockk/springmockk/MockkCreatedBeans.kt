package io.mockk.springmockk

/**
 * Beans created using MockK.
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
internal class MockkCreatedBeans : Iterable<Any> {

    private val beans = ArrayList<Any>()

    fun add(bean: Any) {
        this.beans.add(bean)
    }

    override fun iterator(): Iterator<Any> {
        return this.beans.iterator()
    }

}
