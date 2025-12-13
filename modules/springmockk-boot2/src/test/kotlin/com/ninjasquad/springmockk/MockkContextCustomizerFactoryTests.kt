package com.ninjasquad.springmockk

import io.mockk.MockKAnnotations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


/**
 * Tests for [MockkContextCustomizerFactory].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class MockkContextCustomizerFactoryTests {

    private val factory = MockkContextCustomizerFactory()

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun getContextCustomizerWithoutAnnotationReturnsCustomizer() {
        val customizer = this.factory.createContextCustomizer(NoMockBeanAnnotation::class.java, emptyList())
        assertThat(customizer).isNotNull()
    }

    @Test
    fun getContextCustomizerWithAnnotationReturnsCustomizer() {
        val customizer = this.factory.createContextCustomizer(WithMockBeanAnnotation::class.java, emptyList())
        assertThat(customizer).isNotNull()
    }

    @Test
    fun getContextCustomizerUsesMocksAsCacheKey() {
        val customizer = this.factory.createContextCustomizer(WithMockBeanAnnotation::class.java, emptyList())
        assertThat(customizer).isNotNull()
        val same = this.factory.createContextCustomizer(WithSameMockBeanAnnotation::class.java, emptyList())
        assertThat(customizer).isNotNull()
        val different = this.factory.createContextCustomizer(WithDifferentMockBeanAnnotation::class.java, emptyList())
        assertThat(different).isNotNull()
        assertThat(customizer.hashCode()).isEqualTo(same.hashCode())
        assertThat(customizer.hashCode()).isNotEqualTo(different.hashCode())
        assertThat(customizer).isEqualTo(customizer)
        assertThat(customizer).isEqualTo(same)
        assertThat(customizer).isNotEqualTo(different)
    }

    internal class NoMockBeanAnnotation

    @MockkBean(Service1::class, Service2::class)
    internal class WithMockBeanAnnotation

    @MockkBean(Service2::class, Service1::class)
    internal class WithSameMockBeanAnnotation

    @MockkBean(Service1::class)
    internal class WithDifferentMockBeanAnnotation

    internal interface Service1

    internal interface Service2

}
