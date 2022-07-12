buildscript {
    ext {
        kotlin_version = "1.7.10"
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:_")
        classpath("org.jetbrains.kotlin:kotlin-allopen:_")
    }
}

plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

configurations.all({
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
})

repositories {
    mavenLocal()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    mavenCentral()
}

dependencies {
    implementation(Kotlin.stdlib)
    implementation(Kotlin.stdlib.common)
    implementation("org.jetbrains.kotlin:kotlin-reflect:_")
    testImplementation(project(":mockk-jvm"))

    testImplementation(Kotlin.test.junit) {
        exclude(group = "junit", module = "junit")
    }

    testImplementation("org.slf4j:slf4j-api:_")
    testImplementation("ch.qos.logback:logback-classic:_")

    compileOnly(Testing.junit.jupiter.api)
    testImplementation(Testing.junit.jupiter.api)
    testImplementation(Testing.junit.jupiter.engine)
    testImplementation("org.junit.vintage:junit-vintage-engine:_")
}
