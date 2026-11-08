plugins { alias(libs.plugins.kmp) }
kotlin {
    jvm("desktop")
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies { api(libs.ktor.core) }
        getByName("desktopMain").dependencies { implementation(libs.ktor.cio) }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
