package io.mockk.proxy

interface MockKAgentLogFactory {
    fun logger(cls: Class<*>): MockKAgentLogger

    companion object {
        val SIMPLE_CONSOLE_LOGGER = object : MockKAgentLogFactory {
            override fun logger(cls: Class<*>) = object : MockKAgentLogger {
                override fun debug(msg: String) {
                    println("DEBUG: $msg")
                }

                override fun trace(msg: String) {
                    println("TRACE: $msg")
                }

                override fun trace(ex: Throwable, msg: String) {
                    println("TRACE: $msg")
                    ex.printStackTrace(System.out)
                }

                override fun warn(msg: String) {
                    println("WARN: $msg")
                }

                override fun warn(ex: Throwable, msg: String) {
                    println("WARN: $msg")
                    ex.printStackTrace(System.out)
                }
            }
        }
    }
}
