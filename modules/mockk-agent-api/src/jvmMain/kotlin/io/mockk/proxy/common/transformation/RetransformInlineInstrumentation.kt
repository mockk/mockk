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
        } catch (ex: UnsupportedOperationException) {
            // The JVM reports a rejected redefinition ("class redefinition failed: ...") as an
            // UnsupportedOperationException. Such a class can never be instrumented, so returning a
            // mock that silently does not intercept anything would hide the real problem
            // (#1451, #1422, #1487).
            throw failedInstrumentation(request, cancellation, ex)
        } catch (ex: LinkageError) {
            throw failedInstrumentation(request, cancellation, ex)
        } catch (ex: InternalError) {
            throw failedInstrumentation(request, cancellation, ex)
        } catch (ex: java.lang.Exception) {
            log.warn(ex, "Failed to transform classes ${request.classes}")
            cancellation?.invoke()
            return {}
        }

        return cancellation ?: {}
    }

    private fun failedInstrumentation(
        request: TransformationRequest,
        cancellation: (() -> Unit)?,
        error: Throwable,
    ): MockKAgentException {
        log.warn(error, "Failed to transform classes ${request.classes}")

        // Reverting is best-effort. A class the JVM refused to redefine cannot be reverted either,
        // and a failure here must not replace the rejection that actually happened.
        try {
            cancellation?.invoke()
        } catch (revert: Throwable) {
            log.warn(revert, "Failed to revert class transformation ${request.classes}")
        }

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

        val likelyCause =
            if (error is UnsupportedOperationException) {
                "A redefinition may only change method bodies, so the class or its members were " +
                    "modified after it was loaded. This is most often done by another tool that " +
                    "instruments classes, for example a debugger or a code-coverage agent, which " +
                    "then conflicts with the retransformation mockk needs to mock the class."
            } else {
                "Most likely a class referenced by them cannot be loaded or initialized on this JVM."
            }

        return MockKAgentException(
            "Failed to instrument classes ${request.classes}: the JVM rejected the redefinition " +
                "(${error.message}). $likelyCause",
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
