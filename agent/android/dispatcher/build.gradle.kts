plugins {
    id("com.android.application")
}

android {
    compileSdkVersion(30)

    android {
        lintOptions {
            disable("InvalidPackage")
            warning("NewApi")
        }

        packagingOptions {
            exclude("META-INF/main.kotlin_module")
        }
    }

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(28)
        applicationId = "com.android.dexmaker.mockito.inline.dispatcher"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
