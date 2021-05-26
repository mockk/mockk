package io.mockk.gh

import io.mockk.mockk
import io.mockk.verifyAll
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class Tweet(val id: Int, val text: String)

interface TweetRepository {

    fun persist(tweet: Tweet)

}

class Issue389Test {

    @Test
    @Ignore
    // Temporarily ignored because it suddenly started failing only on Github actions
    internal fun verifyMultiplePersists() {
        val repositoryMock = mockk<TweetRepository>(relaxed = true)

        repositoryMock.persist(Tweet(1, "first tweet"))
        repositoryMock.persist(Tweet(2, "second tweet"))


        verifyAll {
            repositoryMock.persist(
                withArg {
                    assertEquals(it.id, 1)
                    assertEquals(it.text, "first tweet")
                })
            repositoryMock.persist(
                withArg {
                    assertEquals(it.id,2)
                    assertEquals(it.text, "second tweet")
                })
        }
    }
}
