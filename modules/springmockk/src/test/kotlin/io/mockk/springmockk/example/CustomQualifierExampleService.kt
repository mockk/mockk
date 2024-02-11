package io.mockk.springmockk.example

/**
 * An [ExampleService] that uses a custom qualifier.
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@CustomQualifier
class CustomQualifierExampleService : ExampleService {

    override fun greeting(): String {
        return "CustomQualifier"
    }

}
