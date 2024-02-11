package io.mockk.springmockk

import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockBean] with `relaxUnitFun`.
 *
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
@MockkBean(UnitReturningService::class, relaxUnitFun = true)
class MockBeanWithRelaxUnitFunIntegrationTests {

    @Autowired
    private lateinit var caller: UnitReturningServiceCaller

    @Test
    fun testMocking() {
        caller.call("Boot")
        verify { caller.service.greet("Boot") }
    }

    @Configuration
    @Import(UnitReturningServiceCaller::class)
    internal class Config
}

interface UnitReturningService {
    fun greet(message: String): Unit
}

class UnitReturningServiceCaller(val service: UnitReturningService) {
    fun call(message: String) {
        this.service.greet(message)
    }
}
