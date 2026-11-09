plugins {
    alias(libs.plugins.kmp)
    alias(libs.plugins.sqldelight)
}
kotlin {
    jvm("desktop")
    jvmToolchain(21)
    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            api(libs.coroutines.core)
            implementation(libs.sqldelight.coroutines)
        }
        getByName("desktopMain").dependencies {
            api(libs.sqlite)
            implementation(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}
sqldelight { databases { create("CenterDatabase") { packageName.set("center.database") } } }
