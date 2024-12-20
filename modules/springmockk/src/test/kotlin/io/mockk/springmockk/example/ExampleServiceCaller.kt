package io.mockk.springmockk.example

/**
 * Example bean for mocking tests that calls [ExampleService].
 *
 * @author Phillip Webb
 */
class ExampleServiceCaller(val service: ExampleService) {

    fun sayGreeting(): String {
        return "I say " + this.service.greeting()
    }

}
