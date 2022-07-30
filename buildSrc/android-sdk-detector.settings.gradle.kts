val sdkDirProperty = "sdk.dir"

val props = java.util.Properties()

val propsPath = rootProject.projectDir.resolve("local.properties")
if (propsPath.isFile) {
    props.load(java.io.FileReader(propsPath))
}

val sdkDirFile: java.io.File? = listOf(
    providers.provider { props.getProperty(sdkDirProperty) },
    providers.environmentVariable("ANDROID_HOME"),
    providers.environmentVariable("ANDROID_SDK_ROOT"),
    providers.environmentVariable("HOME").map { "$it/Android/Sdk" },
).asSequence()
    .mapNotNull { pathProvider: Provider<String> ->
        pathProvider.orNull
            .takeIf { !it.isNullOrBlank() }
            ?.let { File(it) }
            ?.takeIf { it.exists() }
    }.find { it.isDirectory }

var androidSdkDetected: Boolean by extra(sdkDirFile != null)

if (androidSdkDetected) {
    logger.lifecycle("[android-sdk-detector] Android SDK detected: ${sdkDirFile.canonicalPath}")

    if (
        sdkDirFile.canonicalPath != props.getProperty(sdkDirProperty)
        && props[sdkDirProperty] != null
        && sdkDirFile.isDirectory
    ) {
        val path = sdkDirFile.canonicalPath
        props.setProperty(sdkDirProperty, path)
    }
} else {
    logger.lifecycle(
        """
           | [WARNING] Skipping build of Android related modules because Android SDK has not been found!
           |           Define Android SDK location in 'local.properties' file or with ANDROID_HOME
           |           environment variable to build Android modules
        """.trimMargin()
    )
}