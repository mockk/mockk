package io.mockk.impl.verify

import io.mockk.Invocation
import io.mockk.MockKSettings
import io.mockk.RecordedCall
import io.mockk.StackElement
import io.mockk.StackTracesAlignment
import io.mockk.impl.InternalPlatform
import io.mockk.impl.stub.StubRepository

object VerificationHelpers {
    fun formatCalls(calls: List<Invocation>, verifiedCalls: List<Invocation> = listOf()): String =
        calls.mapIndexed { idx, call ->
            val plusSign = (if (verifiedCalls.contains(call)) "+" else "")

            "${idx + 1}) $plusSign$call"
        }.joinToString("\n")

    fun stackTraces(calls: List<Invocation>): String =
        calls.mapIndexed { idx, call ->
            val prefix = "${idx + 1})"
            "$prefix ${stackTrace(prefix.length + 1, call.callStack())}"
        }.joinToString("\n\n")

    fun stackTrace(prefix: Int, stackTrace: List<StackElement>): String {
        @Suppress("DEPRECATION_ERROR")
        fun columnSize(block: StackElement.() -> String) =
            stackTrace.map(block).map { it.length }.maxOfOrNull { it } ?: 0

        fun StackElement.fileLine() =
            "($fileName:$line)${if (nativeMethod) "N" else ""}"

        fun spaces(n: Int) = if (n < 0) "" else (1..n).joinToString("") { " " }
        fun columnRight(s: String, sz: Int) = spaces(sz - s.length) + s
        fun columnLeft(s: String, sz: Int) = s + spaces(sz - s.length)


        val maxClassNameLen = columnSize { className }
        val maxMethodLen = columnSize { methodName }
        val maxThirdColumn = columnSize { fileLine() }

        val lineFormatter: (StackElement) -> String = if(MockKSettings.stackTracesAlignment == StackTracesAlignment.CENTER) {
            {
                spaces(prefix) +
                    columnRight(it.className, maxClassNameLen) + "." +
                    columnLeft(it.methodName, maxMethodLen) + " " +
                    columnLeft(it.fileLine(), maxThirdColumn)
            }
        } else {
            {
                spaces(prefix) +
                    it.className + "." +
                    it.methodName + " " +
                    it.fileLine()
            }
        }

        return stackTrace.joinToString("\n") {
            lineFormatter(it)
        }.substring(prefix)
    }


    fun List<RecordedCall>.allInvocations(stubRepo: StubRepository) =
        this.map { InternalPlatform.ref(it.matcher.self) }
            .distinct()
            .map { it.value }
            .flatMap { stubRepo.stubFor(it).allRecordedCalls() }
            .sortedBy { it.timestamp }

    fun reportCalls(
        matchers: List<RecordedCall>,
        allCalls: List<Invocation>,
        lcs: LCSMatchingAlgo = LCSMatchingAlgo(allCalls, matchers).apply { lcs() }
    ): String =
        "\n\nMatchers: \n" + formatMatchers(matchers, lcs.verifiedMatchers) +
        "\n\nCalls:\n" +
        formatCalls(allCalls, lcs.verifiedCalls) +
        "\n" +
        if (MockKSettings.stackTracesOnVerify)
            "\nStack traces:\n" + stackTraces(allCalls)
        else
            ""

    private fun formatMatchers(matchers: List<RecordedCall>, verifiedMatchers: List<RecordedCall>) =
        matchers.joinToString("\n") {
            (if (verifiedMatchers.contains(it)) "+" else "") + it.matcher.toString()
        }
}

