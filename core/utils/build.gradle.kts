plugins { alias(libs.plugins.kmp) }
kotlin {
    jvm("desktop")
    jvmToolchain(21)
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
