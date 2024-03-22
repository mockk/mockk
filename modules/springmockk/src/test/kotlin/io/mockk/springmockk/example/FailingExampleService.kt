package io.mockk.springmockk.example

import org.springframework.stereotype.Service

/**
 * An [ExampleService] that always throws an exception.
 *
 * @author Phillip Webb
 */
@Service
class FailingExampleService : ExampleService {

    override fun greeting(): String {
        throw IllegalStateException("Failed")
    }

}
