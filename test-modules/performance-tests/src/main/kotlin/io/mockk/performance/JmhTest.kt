package io.mockk.performance

import io.mockk.every
import io.mockk.mockk
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.infra.Blackhole

@BenchmarkMode(Mode.Throughput)
open class JmhTest {

    @Benchmark
    fun simpleMockAndStub(blackhole: Blackhole) {
        val mockedClass: MockedClass = mockk()
        every { mockedClass.mockedFun() } returns "Hello, mockk!"
        blackhole.consume(mockedClass)
    }

    class MockedClass {
        fun mockedFun(): String = "Hello, world!"
    }
}
