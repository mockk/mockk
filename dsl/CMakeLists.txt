cmake_minimum_required(VERSION 3.8)

set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_LIST_DIR}/KotlinCMakeModule)

project(mockk-dsl-native Kotlin)

konanc_library(
        NAME mockk-dsl-native
        SOURCES common/src/main/kotlin/io/mockk/Answers.kt
        SOURCES common/src/main/kotlin/io/mockk/API.kt
        SOURCES common/src/main/kotlin/io/mockk/GatewayAPI.kt
        SOURCES common/src/main/kotlin/io/mockk/Matchers.kt
        SOURCES native/src/main/kotlin/io/mockk/InternalPlatformDsl.kt)

konanc_executable(
        NAME mockk-dsl-native-test
        SOURCES native/src/main/kotlin/io/mockk/Run.kt
        LIBRARIES mockk-dsl-native
)
