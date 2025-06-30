package io.mockk.it

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals


@OptIn(ExperimentalCoroutinesApi::class)
class GenericSubclassTest {

    /**
     * See https://github.com/mockk/mockk/issues/920
     */
    @Test
    fun `StackOverflow minimal setup`() = runTest {
        val repo = mockk<RepositoryToMock> {
            coEvery { create(any()) } coAnswers { true }
        }

        assertEquals(
            true,
            repo.create(JsonModel()),
        )
    }

}


private class JsonModel

private class RepositoryToMock :
    AbstractRepository<JsonModel>("Collection", JsonModel::class)

private abstract class AbstractRepository<T : Any>(
    private val collectionName: String,
    private var clazz: KClass<T>
) {

    open suspend fun create(model: T): Boolean {
        return true
    }
}
