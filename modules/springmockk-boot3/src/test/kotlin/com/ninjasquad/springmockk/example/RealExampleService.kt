package com.ninjasquad.springmockk.example

/**
 * Example service implementation for spy tests.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
open class RealExampleService(private val greeting: String) : ExampleService {

    override fun greeting(): String {
        return this.greeting
    }

}
