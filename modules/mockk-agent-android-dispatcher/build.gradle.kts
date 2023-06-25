import buildsrc.config.androidClassesDexAttributes
import buildsrc.config.asProvider
import com.android.build.gradle.internal.tasks.DexMergingTask

plugins {
    buildsrc.convention.`android-application`
}

@Suppress("UnstableApiUsage")
android {
    namespace = "io.mockk.proxy.android.dispatcher"
    defaultConfig {
        applicationId = "com.android.dexmaker.mockito.inline.dispatcher"
        versionCode = 1
    }
}

val androidClassesDexProvider by configurations.registering {
    description = "Provide an Android classes.dex"
    asProvider()
    androidClassesDexAttributes()

    outgoing.artifact(
        tasks.provider<DexMergingTask>("mergeDexRelease")
            .map { task -> task.outputDir }
    )
}

// workaround for https://github.com/gradle/gradle/issues/16543
inline fun <reified T : Task> TaskContainer.provider(taskName: String): Provider<T> =
    providers.provider { taskName }
        .flatMap { named<T>(it) }
