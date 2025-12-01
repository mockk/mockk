package com.ninjasquad.springmockk

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.context.junit.jupiter.SpringExtension


/**
 * Test [MockBean] for a factory bean.
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
@ExtendWith(SpringExtension::class)
class MockBeanForBeanFactoryIntegrationTests {

    // gh-7439

    @MockkBean(relaxed = true)
    private lateinit var testFactoryBean: TestFactoryBean

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Test
    fun testName() {
        val testBean = mockk<TestBean>()
        every { testBean.hello() } returns "amock"

        every { testFactoryBean.getObjectType() } returns TestBean::class.java as Class<*>
        every { testFactoryBean.getObject() } returns testBean

        val bean = this.applicationContext.getBean<TestBean>()
        assertThat(bean.hello()).isEqualTo("amock")
    }

    @Configuration
    internal class Config {

        @Bean
        fun testFactoryBean(): TestFactoryBean {
            return TestFactoryBean()
        }

    }

    internal class TestFactoryBean : FactoryBean<TestBean> {

        override fun getObject(): TestBean {
            return object: TestBean {
                override fun hello() = "normal"
            }
        }

        override fun getObjectType(): Class<*> {
            return TestBean::class.java
        }

        override fun isSingleton(): Boolean {
            return false
        }

    }

    internal interface TestBean {
        fun hello(): String
    }

}
