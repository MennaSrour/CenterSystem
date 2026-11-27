plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
}
kotlin {
    jvm("desktop")
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":core:ui"))
            implementation(compose.material3)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
