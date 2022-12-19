package io.mockk.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope.JAVA_FILE
import com.android.tools.lint.detector.api.Scope.TEST_SOURCES
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import com.android.tools.lint.detector.api.isString
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.TypeConversionUtil.isPrimitive
import com.intellij.psi.util.TypeConversionUtil.isPrimitiveWrapper
import org.jetbrains.uast.UCallExpression
import java.util.*


internal class MockkUnnecessaryUsageDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("mockk")

    private fun PsiMethod.isInPackageName() = (containingFile as? PsiJavaFile)?.packageName == "io.mockk"

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (!method.isInPackageName()) return
        val returnType by lazy { node.returnType }
        val returnTypeText by lazy { returnType?.canonicalText }
        val returnTypeClass by lazy { PsiTypesUtil.getPsiClass(returnType) }
        val issue = when {
            // Primitives (+ String)
            isPrimitive(returnTypeText.orEmpty())
                    || isPrimitiveWrapper(returnType)
                    || returnType?.let(::isString) == true -> ISSUE_PRIMITIVE
            // Data classes
            context.evaluator.isData(returnTypeClass) -> ISSUE_DATA_CLASS
            // Interfaces
            returnTypeClass?.isInterface == true -> ISSUE_INTERFACE
            // Enums
            returnTypeClass?.isEnum == true -> ISSUE_ENUM
            else -> return
        }
        context.report(
            issue = issue,
            scope = node,
            location = context.getLocation(node),
            message = issue.getExplanation(TextFormat.TEXT),
        )
    }

    companion object {

        val ISSUE_DATA_CLASS = issue(id = "MockkDataClass", "data class")
        val ISSUE_ENUM = issue(id = "MockkEnum", "enum")
        val ISSUE_INTERFACE = issue(id = "MockkInterface", "interface")
        val ISSUE_PRIMITIVE = issue(id = "MockkPrimitive", "primitive type")

        private fun issue(id: String, what: String): Issue = Issue.create(
            id = id,
            briefDescription = "Detect unnecessary usages of mockk().",
            explanation = "Mock of $what is unnecessary and should be replaced with a simpler instantiation.",
            category = Category.TESTING,
            priority = 5,
            severity = Severity.WARNING,
            enabledByDefault = false,
            implementation = Implementation(MockkUnnecessaryUsageDetector::class.java, EnumSet.of(JAVA_FILE, TEST_SOURCES)),
        )

    }

}
