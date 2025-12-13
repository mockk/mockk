package com.ninjasquad.springmockk.example

/**
 * Example generic service implementation for spy tests.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class SimpleExampleStringGenericService(private val greeting: String = "simple") : ExampleGenericService<String> {

    override fun greeting() = this.greeting

}
