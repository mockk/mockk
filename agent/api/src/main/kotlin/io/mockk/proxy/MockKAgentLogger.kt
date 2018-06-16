package io.mockk.proxy

interface MockKAgentLogger {

    fun debug(msg: String)

    fun trace(msg: String)

    fun trace(ex: Throwable, msg: String)

    fun warn(ex: Throwable, msg: String)

    companion object {
        val NO_OP: MockKAgentLogger = object : MockKAgentLogger {
            override fun debug(msg: String) {}

            override fun trace(msg: String) {}

            override fun trace(ex: Throwable, msg: String) {}

            override fun warn(ex: Throwable, msg: String) {

            }
        }
    }
}
