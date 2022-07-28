package io.mockk.jvm

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import javax.sql.DataSource

/**
 * ClassLoadingStrategy error while mocking javax.sql.DataSource.
 * Verifies issue #280.
 */
class ClassLoadingStrategyTest {
    private val dataSource = mockk<DataSource>()

    @Test
    fun testDataSource() {
        every { dataSource.getConnection(any(), any()) } returns null
    }
}
