package io.mockk.springmockk.example

/**
 * Example bean for mocking tests that calls [ExampleGenericService].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class ExampleGenericStringServiceCaller(private val stringService: ExampleGenericService<String>) {

    fun sayGreeting() = "I say " + this.stringService.greeting()

}
