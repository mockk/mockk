package io.mockk.impl.verify

import io.mockk.Invocation
import io.mockk.RecordedCall
import io.mockk.StackElement
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.StubRepository

object VerificationHelpers {
    fun formatCalls(calls: List<Invocation>): String {
        return calls.mapIndexed { idx, call ->
            "${idx + 1}) $call"
        }.joinToString("\n")
    }

    fun stackTraces(calls: List<Invocation>): String {
        return calls.mapIndexed { idx, call ->
            val prefix = "${idx + 1})"
            "$prefix ${stackTrace(prefix.length + 1, call.callStack)}"
        }.joinToString("\n\n")
    }

    fun stackTrace(prefix: Int, stackTrace: List<StackElement>): String {
        fun columnSize(block: StackElement.() -> String) =
            stackTrace.map(block).map { it.length }.max() ?: 0

        fun StackElement.fileLine() =
            "($fileName:$line)${if (nativeMethod) "N" else ""}"

        fun spaces(n: Int) = if (n < 0) "" else (1..n).map { " " }.joinToString("")
        fun columnRight(s: String, sz: Int) = spaces(sz - s.length) + s
        fun columnLeft(s: String, sz: Int) = s + spaces(sz - s.length)


        val maxClassNameLen = columnSize { className }
        val maxMethodLen = columnSize { methodName }
        val maxThirdColumn = columnSize { fileLine() }

        return stackTrace.map {
            spaces(prefix) +
                    columnRight(it.className, maxClassNameLen) + "." +
                    columnLeft(it.methodName, maxMethodLen) + " " +
                    columnLeft(it.fileLine(), maxThirdColumn)
        }.joinToString("\n").substring(prefix)
    }


    fun List<RecordedCall>.allInvocations(stubRepo: StubRepository) =
        this.map { InternalPlatform.ref(it.matcher.self) }
            .distinct()
            .map { it.value }
            .flatMap { stubRepo.stubFor(it).allRecordedCalls() }
            .sortedBy { it.timestamp }

    fun reportCalls(calls: List<RecordedCall>, allCalls: List<Invocation>): String {
        return "\n\nMatchers: \n" + calls.map { it.matcher.toString() }.joinToString("\n") +
                "\n\nCalls:\n" +
                formatCalls(allCalls) +
                "\n\nStack traces:\n" +
                stackTraces(allCalls)
    }
}

