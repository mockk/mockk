package com.ninjasquad.springmockk.example

/**
 * Example generic service implementation for spy tests.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class SimpleExampleIntegerGenericService : ExampleGenericService<Int> {

    override fun greeting() = 123

}
