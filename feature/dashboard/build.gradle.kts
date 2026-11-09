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
            implementation(project(":core:ui"))
            implementation(compose.components.resources)
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
compose.resources {
    packageOfResClass = "center.feature.dashboard.resources"
}
