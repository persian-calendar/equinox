import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.4.20"
    `maven-publish`
}

group = (findProperty("group") as? String) ?: "io.github.persiancalendar"
version = (findProperty("version") as? String) ?: "3.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
        }
    }

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "20s"
                }
            }
        }
        browser {
            testTask {
                useMocha {
                    timeout = "20s"
                }
            }
        }
    }

    wasmJs {
        nodejs()
        browser()
    }

    linuxArm64()
    linuxX64()
    macosArm64()
    mingwX64()

    iosArm64()
    iosSimulatorArm64()
    tvosArm64()
    tvosSimulatorArm64()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<org.gradle.api.tasks.testing.AbstractTestTask>().configureEach {
    testLogging {
        showStandardStreams = true
    }
}

val generateEquinox = tasks.register<Exec>("generateEquinox") {
    group = "generation"
    description = "Regenerate src/commonMain/kotlin/io/github/persiancalendar/Equinox.kt"
    commandLine("python3", "python/generate_equinox.py")
}

val generateTests = tasks.register<Exec>("generateTests") {
    group = "generation"
    description = "Regenerate src/commonTest/kotlin/io/github/persiancalendar/De440Reference.kt"
    commandLine("python3", "python/generate_tests.py")
}

tasks.register("generateSources") {
    group = "generation"
    description = "Regenerate all generated files (Equinox.kt, De440Reference.kt, Tests.kt)"
    dependsOn(generateEquinox, generateTests)
}

val checkGeneratedSources = tasks.register<Exec>("checkGeneratedSources") {
    group = "verification"
    description = "Regenerate all sources and fail if they differ from what is committed"
    dependsOn("generateSources")
    commandLine("git", "diff", "--exit-code", "--", "src/")
}
