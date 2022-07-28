package io.mockk.proxy

interface MockKAgentLogger {

    fun debug(msg: String)

    fun trace(msg: String)

    fun trace(ex: Throwable, msg: String)

    fun warn(msg: String)

    fun warn(ex: Throwable, msg: String)
}
