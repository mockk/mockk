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

    @Test
    fun `check against verify order`() {
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
