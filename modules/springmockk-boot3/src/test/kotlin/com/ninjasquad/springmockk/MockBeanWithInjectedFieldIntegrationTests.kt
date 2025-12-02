package com.ninjasquad.springmockk

import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Tests for a mock bean where the class being mocked uses field injection.
 *
 * @author Andy Wilkinson
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class MockBeanWithInjectedFieldIntegrationTests {

    @MockkBean
    private lateinit var myService: MyService

    @Test
    fun fieldInjectionIntoMyServiceMockIsNotAttempted() {
        every { myService.count() } returns 5
        assertThat(this.myService.count()).isEqualTo(5)
    }

    private class MyService {

        @Autowired
        private lateinit var repository: MyRepository

        fun count() = this.repository.findAll().size

    }

    private interface MyRepository {

        fun findAll(): List<Any>

    }

}
