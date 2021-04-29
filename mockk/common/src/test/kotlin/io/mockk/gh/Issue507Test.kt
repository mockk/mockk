package io.mockk.gh

import io.mockk.mockk
import io.mockk.verifyOrder
import kotlin.test.Test

interface Tracker {
    fun track(song: String, action: String, param: String, moreParam: Map<String, String>)
}

class Player(private val tracker: Tracker) {
    fun goCrazy() {
        tracker.track("song 2", "play", "param", mapOf(Pair("key", "value")))
        tracker.track("song 2", "pause", "param", mapOf(Pair("key", "value")))
        tracker.track("song 2", "play", "param", mapOf(Pair("key", "value")))
        tracker.track("song 2", "pause", "param", mapOf(Pair("key", "value")))
        tracker.track("song 2", "play", "param", mapOf(Pair("key", "value")))
    }
}


class Issue507Test {

    /**
     * A regression occurred in version 1.10.2 causing verify order to use
     * eq() instead of any() matcher.
     * This test exist to avoid this kind of regression in the future.
     */
    @Test
    fun checkAgainstVerifyOrder() {
        val tracker = mockk<Tracker>(relaxUnitFun = true)
        val player = Player(tracker)

        player.goCrazy()

        verifyOrder {
            tracker.track(any(), "play", "param", any())
            tracker.track(any(), "pause", "param", any())
            tracker.track(any(), "play", "param", any())
            tracker.track(any(), "pause", "param", any())
            tracker.track(any(), "play", "param", any())
        }
    }
}
