plugins {
    buildsrc.convention.`android-library`
    id("com.android.application")
}

android {
//    compileSdkVersion = "android-32"

//    lint {
//        abortOnError = false
//        disable += "InvalidPackage"
//        warning += "NewApi"
//    }
//
//    packagingOptions {
//        exclude("META-INF/main.kotlin_module")
//    }

    defaultConfig {
//        minSdk = 26
//        targetSdk = 32
        applicationId = "com.android.dexmaker.mockito.inline.dispatcher"
    }

//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_1_8
//        targetCompatibility = JavaVersion.VERSION_1_8
//    }
}
