package io.mockk.jvm

import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import javax.sql.DataSource

class Issue280 {
    private val dataSource = mockk<DataSource>()

    @Test
    fun test() {
        every { dataSource.getConnection(any(), any()) } returns null
    }
}