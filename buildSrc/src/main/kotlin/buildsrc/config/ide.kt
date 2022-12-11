package buildsrc.config

import org.gradle.api.file.ProjectLayout
import org.gradle.plugins.ide.idea.model.IdeaModule


/**
 * Exclude generated Gradle accessors code, so it doesn't clog up IntelliJ search results.
 *
 * This doesn't affect the build.
 */
fun IdeaModule.excludeGeneratedGradleDslAccessors(layout: ProjectLayout) {
    excludeDirs.addAll(
        layout.files(
            "buildSrc/build/generated-sources/kotlin-dsl-accessors",
            "buildSrc/build/generated-sources/kotlin-dsl-accessors/kotlin",
            "buildSrc/build/generated-sources/kotlin-dsl-accessors/kotlin/gradle",
            "buildSrc/build/generated-sources/kotlin-dsl-external-plugin-spec-builders",
            "buildSrc/build/generated-sources/kotlin-dsl-external-plugin-spec-builders/kotlin",
            "buildSrc/build/generated-sources/kotlin-dsl-external-plugin-spec-builders/kotlin/gradle",
            "buildSrc/build/generated-sources/kotlin-dsl-plugins",
            "buildSrc/build/generated-sources/kotlin-dsl-plugins/kotlin",
            "buildSrc/build/generated-sources/kotlin-dsl-plugins/kotlin/buildsrc",
            "buildSrc/build/pluginUnderTestMetadata",
        )
    )
}
