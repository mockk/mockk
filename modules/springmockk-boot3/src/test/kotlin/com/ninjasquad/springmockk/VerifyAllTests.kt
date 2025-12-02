package com.ninjasquad.springmockk

import io.mockk.every
import io.mockk.verifyAll
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.test.context.junit.jupiter.SpringExtension

/**
 * Tests for issue https://github.com/Ninja-Squad/springmockk/issues/90
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class VerifyAllTests @Autowired constructor(@MockkBean val testService: VerifyAllTestService) {

    @Test
    fun testService() {
        every { testService.one() } returns 2
        assertThat(testService.one()).isEqualTo(2)
        verifyAll { testService.one() }
    }
}

@Component
class VerifyAllTestService {
    fun one() = 1
}
