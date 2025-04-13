package io.mockk.springmockk

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.Configuration
import org.springframework.util.ReflectionUtils


/**
 * Tests for [QualifierDefinition].
 *
 * @author Phillip Webb
 * @author JB Nizet
 */
class QualifierDefinitionTests {

    @MockK(relaxed = true)
    private lateinit var beanFactory: ConfigurableListableBeanFactory

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun forElementWhenElementIsNotFieldShouldReturnNull() {
        assertThat(QualifierDefinition.forElement(javaClass)).isNull()
    }

    @Test
    fun forElementWhenElementIsFieldWithNoQualifiersShouldReturnNull() {
        val definition = QualifierDefinition.forElement(ReflectionUtils.findField(ConfigA::class.java, "noQualifier")!!)
        assertThat(definition).isNull()
    }

    @Test
    fun forElementWhenElementIsFieldWithQualifierShouldReturnDefinition() {
        val definition = QualifierDefinition.forElement(ReflectionUtils.findField(ConfigA::class.java, "directQualifier")!!)
        assertThat(definition).isNotNull()
    }

    @Test
    fun matchesShouldCallBeanFactory() {
        val field = ReflectionUtils.findField(ConfigA::class.java, "directQualifier")!!
        val qualifierDefinition = QualifierDefinition.forElement(field)!!
        qualifierDefinition.matches(this.beanFactory, "bean")
        verify {
            beanFactory.isAutowireCandidate(
                "bean",
                withArg { assertThat(it.annotatedElement).isEqualTo(field) }
            )
        }
    }

    @Test
    fun applyToShouldSetQualifierElement() {
        val field = ReflectionUtils.findField(ConfigA::class.java, "directQualifier")!!
        val qualifierDefinition = QualifierDefinition.forElement(field)!!
        val definition = RootBeanDefinition()
        qualifierDefinition.applyTo(definition)
        assertThat(definition.qualifiedElement).isEqualTo(field)
    }

    @Test
    fun hashCodeAndEqualsShouldWorkOnDifferentClasses() {
        val directQualifier1 =
            QualifierDefinition.forElement(ReflectionUtils.findField(ConfigA::class.java, "directQualifier")!!)!!
        val directQualifier2 =
            QualifierDefinition.forElement(ReflectionUtils.findField(ConfigB::class.java, "directQualifier")!!)!!
        val differentDirectQualifier1 =
            QualifierDefinition.forElement(ReflectionUtils.findField(ConfigA::class.java, "differentDirectQualifier")!!)!!
        val differentDirectQualifier2 =
            QualifierDefinition.forElement(ReflectionUtils.findField(ConfigB::class.java, "differentDirectQualifier")!!)!!
        val customQualifier1 =
            QualifierDefinition.forElement(ReflectionUtils.findField(ConfigA::class.java, "customQualifier")!!)!!
        val customQualifier2 =
            QualifierDefinition.forElement(ReflectionUtils.findField(ConfigB::class.java, "customQualifier")!!)!!

        assertThat(directQualifier1.hashCode()).isEqualTo(directQualifier2.hashCode())
        assertThat(differentDirectQualifier1.hashCode())
            .isEqualTo(differentDirectQualifier2.hashCode())
        assertThat(customQualifier1.hashCode()).isEqualTo(customQualifier2.hashCode())
        assertThat(differentDirectQualifier1).isEqualTo(differentDirectQualifier1)
            .isEqualTo(differentDirectQualifier2).isNotEqualTo(directQualifier2)
        assertThat(directQualifier1).isEqualTo(directQualifier1)
            .isEqualTo(directQualifier2).isNotEqualTo(differentDirectQualifier1)
        assertThat(customQualifier1).isEqualTo(customQualifier1)
            .isEqualTo(customQualifier2).isNotEqualTo(differentDirectQualifier1)
    }

    @Configuration
    internal class ConfigA {

        @MockkBean
        private lateinit var noQualifier: Any

        @MockkBean
        @Qualifier("test")
        private lateinit var directQualifier: Any

        @MockkBean
        @Qualifier("different")
        private lateinit var differentDirectQualifier: Any

        @MockkBean
        @CustomQualifier
        private lateinit var customQualifier: Any

    }

    internal class ConfigB {

        @MockkBean
        @Qualifier("test")
        private lateinit var directQualifier: Any

        @MockkBean
        @Qualifier("different")
        private lateinit var differentDirectQualifier: Any

        @MockkBean
        @CustomQualifier
        private lateinit var customQualifier: Any

    }

    @Qualifier
    @Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
    annotation class CustomQualifier
}
