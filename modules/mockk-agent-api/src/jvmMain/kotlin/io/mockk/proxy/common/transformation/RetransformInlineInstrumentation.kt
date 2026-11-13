package io.mockk.proxy.common.transformation

import io.mockk.proxy.MockKAgentException
import io.mockk.proxy.MockKAgentLogger
import java.lang.instrument.UnmodifiableClassException

abstract class RetransformInlineInstrumentation(
    protected val log: MockKAgentLogger,
    private val specMap: ClassTransformationSpecMap,
) : InlineInstrumentation {
    protected abstract fun retransform(classesToTransform: Collection<Class<*>>)

    override fun execute(request: TransformationRequest): () -> Unit {
        var cancellation: (() -> Unit)? = null
        try {
            specMap.applyTransformation(request) {
                cancellation = { doCancel(request) }

                retransform(it.classes)
            }
        } catch (ex: java.lang.Exception) {
            log.warn(ex, "Failed to transform classes ${request.classes}")
            cancellation?.invoke()
            return {}
        } catch (ex: LinkageError) {
            throw failedInstrumentation(request, cancellation, ex)
        } catch (ex: InternalError) {
            throw failedInstrumentation(request, cancellation, ex)
        }

        return cancellation ?: {}
    }

    private fun failedInstrumentation(
        request: TransformationRequest,
        cancellation: (() -> Unit)?,
        error: Throwable,
    ): MockKAgentException {
        log.warn(error, "Failed to transform classes ${request.classes}")
        cancellation?.invoke()

        // The JVM redefines a class from its original bytes and refuses the redefinition with an
        // opaque `InternalError: class redefinition failed: invalid class` (or a bare
        // NoClassDefFoundError) when it cannot verify or link them, naming neither the class nor
        // the actual problem. Probing the members forces the same linking, so the real cause
        // reaches the caller (#1538).
        val cause =
            request.classes.firstNotNullOfOrNull { cls ->
                try {
                    cls.getDeclaredFields()
                    cls.getDeclaredMethods()
                    null
                } catch (linkage: LinkageError) {
                    linkage
                }
            } ?: error

        return MockKAgentException(
            "Failed to instrument classes ${request.classes}: the JVM rejected the redefinition " +
                "(${error.message}). Most likely a class referenced by them cannot be loaded or " +
                "initialized on this JVM.",
            cause,
        )
    }

    private fun doCancel(request: TransformationRequest) {
        try {
            specMap.applyTransformation(
                request.reverse(),
            ) {
                val classes = it.classes
                if (classes.isNotEmpty()) {
                    log.trace("Retransforming back ${it.classes}")
                    retransform(classes)
                }
            }
        } catch (ex: Throwable) {
            // Reverting is best-effort. A class the JVM refused to redefine in the first place,
            // for example one left in an erroneous state by a failing <clinit> (#1538), cannot be
            // reverted either; failing the cleanup would only mask the error that actually
            // happened.
            if (ex is UnmodifiableClassException || ex is LinkageError || ex is InternalError) {
                log.warn(ex, "Failed to revert class transformation ${request.classes}")
            } else {
                throw ex
            }
        }
    }
}
