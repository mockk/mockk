package io.mockk.performance

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.benchmark.Scope
import kotlinx.benchmark.State
import kotlinx.benchmark.TearDown
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.infra.Blackhole

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
open class JmhTest {

    @TearDown(Level.Invocation)
    fun tearDown() = unmockkAll()

    private fun noMockOrStub() = MockedClass()
    private fun simpleMock() = mockk<MockedClass>()
    private fun simpleMockAndStub() = mockk<MockedClass> { every { mockedFun() } returns "Hello, mockk!" }

    private fun mockThenOperation(
        blackhole: Blackhole,
        mockedClassFactory: () -> MockedClass,
        operation: () -> Unit,
    ) {
        blackhole.consume(mockedClassFactory())
        operation()
    }

    @Benchmark
    fun noMockOrStub(blackhole: Blackhole) {
        val mockedClass = noMockOrStub()
        blackhole.consume(mockedClass)
    }

    @Benchmark
    fun simpleMock(blackhole: Blackhole) {
        val mockedClass = simpleMock()
        blackhole.consume(mockedClass)
    }

    @Benchmark
    fun simpleMockAndStub(blackhole: Blackhole) {
        val mockedClass = simpleMockAndStub()
        blackhole.consume(mockedClass)
    }

    @Benchmark
    fun clearAllMocksAfterNoMockOrStub(blackhole: Blackhole) =
        mockThenOperation(blackhole, ::noMockOrStub) { clearAllMocks() }

    @Benchmark
    fun clearAllMocksAfterSimpleMock(blackhole: Blackhole) =
        mockThenOperation(blackhole, ::simpleMock) { clearAllMocks() }

    @Benchmark
    fun clearAllMocksAfterSimpleMockAndStub(blackhole: Blackhole) =
        mockThenOperation(blackhole, ::simpleMockAndStub) { clearAllMocks() }

    @Benchmark
    fun unmockkAllAfterNoMockOrStub(blackhole: Blackhole) =
        mockThenOperation(blackhole, ::noMockOrStub) { unmockkAll() }

    @Benchmark
    fun unmockkAllAfterSimpleMock(blackhole: Blackhole) =
        mockThenOperation(blackhole, ::simpleMock) { unmockkAll() }

    @Benchmark
    fun unmockkAllAfterSimpleMockAndStub(blackhole: Blackhole) =
        mockThenOperation(blackhole, ::simpleMockAndStub) { unmockkAll() }

    class MockedClass {
        fun mockedFun(): String = "Hello, world!"
    }
}
