package io.mockk.springmockk.example

/**
 * Example service interface for mocking tests.
 *
 * @param T the generic type
 * @author Phillip Webb
 * @author JB Nizet
 */
interface ExampleGenericService<T> {

    fun greeting(): T

}
