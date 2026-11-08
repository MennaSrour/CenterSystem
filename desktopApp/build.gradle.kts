import org.jetbrains.compose.desktop.application.dsl.TargetFormat

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
            implementation(libs.koin)
            implementation(libs.decompose)
            implementation(compose.material3)
            implementation(compose.foundation)
            implementation(project(":feature:reception"))
            implementation(project(":feature:billing"))
            implementation(project(":feature:students"))
            implementation(project(":feature:reports"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(project(":core:utils"))
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
compose.desktop {
    application {
        mainClass = "center.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "CenterSystem"
            packageVersion = "1.0.0"
            modules("java.sql", "jdk.unsupported")
            windows {
                menuGroup = "CenterSystem"
                shortcut = true
            }
        }
    }
}
