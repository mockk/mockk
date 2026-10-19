package io.mockk.proxy.jvm.transformation

import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.description.type.TypeDefinition
import net.bytebuddy.description.type.TypeDescription
import net.bytebuddy.dynamic.scaffold.MethodGraph
import net.bytebuddy.matcher.ElementMatchers.isBridge
import net.bytebuddy.matcher.ElementMatchers.isVirtual

/**
 * The default method graph merges a bridge method into the node of the method it bridges, so a
 * redefined class never hands its bridges to the advice visitors and calls through them are not
 * intercepted. That is usually harmless because a bridge forwards to the method it bridges, but for
 * a `final` property with a backing field Kotlin reads the field in the bridge itself: a call
 * through the supertype then returns the field of the mock instead of the stubbed value.
 *
 * This compiler lists the bridges the class declares as nodes of their own, next to the nodes of
 * the graph built by [delegate]. The advice records calls to them against the method they bridge,
 * see [io.mockk.proxy.jvm.advice.BridgeMethodResolver].
 */
internal class BridgeIncludingMethodGraphCompiler(
    private val delegate: MethodGraph.Compiler,
) : MethodGraph.Compiler.AbstractBase() {
    override fun compile(
        typeDefinition: TypeDefinition,
        viewPoint: TypeDescription,
    ): MethodGraph.Linked {
        val graph = delegate.compile(typeDefinition, viewPoint)
        val bridges =
            typeDefinition.declaredMethods
                .filter(isBridge<MethodDescription>().and(isVirtual()))
                .map { MethodGraph.Node.Simple(it) }

        return if (bridges.isEmpty()) graph else WithBridges(graph, bridges)
    }

    private class WithBridges(
        private val graph: MethodGraph.Linked,
        private val bridges: List<MethodGraph.Node>,
    ) : MethodGraph.Linked by graph {
        override fun listNodes(): MethodGraph.NodeList = MethodGraph.NodeList(graph.listNodes() + bridges)
    }
}
