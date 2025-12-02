/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ninjasquad.springmockk.example

/**
 * Example bean that has dependencies on parameterized [ExampleGenericService]
 * collaborators.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 6.2
 */
@JvmRecord
data class ExampleGenericServiceCaller(
    val integerService: ExampleGenericService<Int>,
    val stringService: ExampleGenericService<String>
) {
    fun sayGreeting(): String {
        return "I say " + this.stringService.greeting() + " " + this.integerService.greeting()
    }
}
