package io.mockk.proxy

interface MockKAgentLogFactory {
    fun logger(cls: Class<*>): MockKAgentLogger

    companion object {
        val simpleConsoleLogFactory = object : MockKAgentLogFactory {
            override fun logger(cls: Class<*>) = object : MockKAgentLogger {
                override fun debug(msg: String) {
                    println("DEBUG(${cls.simpleName}): $msg")
                }

                override fun trace(msg: String) {
                    println("TRACE(${cls.simpleName}): $msg")
                }

                override fun trace(ex: Throwable, msg: String) {
                    println("TRACE(${cls.simpleName}): $msg")
                    ex.printStackTrace(System.out)
                }

                override fun warn(msg: String) {
                    println("WARN(${cls.simpleName}): $msg")
                }

                override fun warn(ex: Throwable, msg: String) {
                    println("WARN(${cls.simpleName}): $msg")
                    ex.printStackTrace(System.out)
                }
            }
        }
    }
}
