package io.mockk.proxy

interface MockKAgentLogFactory {
    fun logger(cls: Class<*>): MockKAgentLogger

    companion object {
        val NO_OP = object : MockKAgentLogFactory {
            override fun logger(cls: Class<*>) = MockKAgentLogger.NO_OP
        }
    }
}
