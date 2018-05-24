package io.mockk.proxy

interface MockKAgentLogFactory {
    fun logger(cls: Class<*>): MockKAgentLogger
}
