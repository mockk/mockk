cmake_minimum_required(VERSION 3.8)

set(CMAKE_Kotlin_FLAGS "-Xmulti-platform")
set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_LIST_DIR}/KotlinCMakeModule)

project(mockk-dsl-native Kotlin)

#konanc_library(
#        NAME mockk-dsl-native
#        SOURCES dsl/common/src/main/kotlin
#        SOURCES dsl/native/src/main/kotlin)
#
#konanc_library(
#        NAME mockk-native
#        SOURCES common/src/main/kotlin
#        SOURCES mockk-native/src/main/kotlin
#        LIBRARIES mockk-dsl-native)

konanc_executable(
        NAME mockk-native-test
        SOURCES mockk-native/src/main/kotlin/io/mockk/impl/interception
        SOURCES mockk-native/src/test/kotlin/io/mockk/Run.kt
#        LIBRARIES mockk-native
)
