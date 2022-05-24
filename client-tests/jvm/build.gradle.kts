buildscript {
    ext {
        kotlin_version = "1.6.21"
    }
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("org.jetbrains.kotlin:kotlin-allopen:$kotlin_version")
    }
}

plugins {
    kotlin("jvm") version "1.6.21"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib") {
        version {
            strictly("$kotlin_version")
        }
    }
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common") {
        version {
            strictly("$kotlin_version")
        }
    }
    implementation("org.jetbrains.kotlin:kotlin-reflect") {
        version {
            strictly("$kotlin_version")
        }
    }
    testImplementation(project(":mockk-jvm"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version") {
        exclude(group = "junit", module = "junit")
    }

    testImplementation("org.slf4j:slf4j-api:1.7.36")
    testImplementation("ch.qos.logback:logback-classic:1.2.11")

    compileOnly("org.junit.jupiter:junit-jupiter-api:$junit_jupiter_version")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junit_jupiter_version")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junit_jupiter_version")
    testImplementation("org.junit.vintage:junit-vintage-engine:$junit_vintage_version")
}
