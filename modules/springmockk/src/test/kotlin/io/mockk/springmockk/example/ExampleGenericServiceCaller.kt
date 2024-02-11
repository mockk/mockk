package io.mockk.springmockk.example

/**
 * Example bean for mocking tests that calls [ExampleGenericService].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class ExampleGenericServiceCaller(
    val integerService: ExampleGenericService<Int>,
    val stringService: ExampleGenericService<String>
) {

    fun sayGreeting(): String {
        return ("I say " + this.integerService.greeting() + " "
            + this.stringService.greeting())
    }

}
