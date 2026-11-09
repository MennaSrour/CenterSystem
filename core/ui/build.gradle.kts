plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
}
kotlin {
    jvm("desktop")
    jvmToolchain(21)
    sourceSets.commonMain.dependencies {
        api(compose.material3)
        api(compose.foundation)
        implementation(compose.components.resources)
    }
}
compose.resources {
    packageOfResClass = "center.ui.resources"
}
