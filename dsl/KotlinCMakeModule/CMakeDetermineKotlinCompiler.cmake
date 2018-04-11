if (NOT CMAKE_Kotlin_COMPILER)
    #TODO determine Kotlin compiler in not hardcoded way
    set(Kotlin_BIN_PATH
            $ENV{HOME}/.konan/kotlin-native-macos-0.3/bin
            $ENV{HOME}/.konan/kotlin-native-linux-0.3/bin
            $ENV{HOME}/.konan/kotlin-native-macos-0.3.1/bin
            $ENV{HOME}/.konan/kotlin-native-linux-0.3.1/bin
            $ENV{HOME}/.konan/kotlin-native-macos-0.3.2/bin
            $ENV{HOME}/.konan/kotlin-native-linux-0.3.2/bin
            )


    if (CMAKE_Kotlin_COMPILER_INIT)
        set(CMAKE_Kotlin_COMPILER ${CMAKE_Kotlin_COMPILER_INIT} CACHE PATH "Kotlin Compiler")
    else ()
        find_program(CMAKE_Kotlin_COMPILER
                NAMES konanc
                PATHS ${Kotlin_BIN_PATH}
                )
    endif ()


    if (CMAKE_Kotlin_CINTEROP_INIT)
        set(CMAKE_Kotlin_CINTEROP ${CMAKE_Kotlin_CINTEROP_INIT} CACHE PATH "Kotlin Cinterop")
    else ()
        find_program(CMAKE_Kotlin_CINTEROP
                NAMES cinterop
                PATHS ${Kotlin_BIN_PATH}
                )
    endif ()
endif ()

mark_as_advanced(CMAKE_Kotlin_COMPILER)

configure_file(${CMAKE_CURRENT_LIST_DIR}/CMakeKotlinCompiler.cmake.in
        ${CMAKE_PLATFORM_INFO_DIR}/CMakeKotlinCompiler.cmake @ONLY)

set(CMAKE_Kotlin_COMPILER_ENV_VAR "KOTLIN_COMPILER")