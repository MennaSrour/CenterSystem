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
            implementation(compose.components.resources)
            implementation(project(":core:ui"))
            implementation(project(":feature:dashboard"))
            implementation(project(":feature:financials"))
            implementation(project(":feature:reception"))
            implementation(project(":feature:billing"))
            implementation(project(":feature:students"))
            implementation(project(":feature:courses"))
            implementation(project(":feature:teachers"))
            implementation(project(":feature:reports"))
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(compose.materialIconsExtended)
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(project(":core:utils"))
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("desktopTest").dependencies { implementation(libs.coroutines.test) }
    }
}
compose.desktop {
    application {
        mainClass = "center.app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb)
            packageName = "CenterSystem"
            packageVersion = "1.0.0"
            modules("java.sql", "jdk.unsupported")
            linux {
                iconFile.set(project.file("packaging/center-one.png"))
            }
            windows {
                iconFile.set(project.file("packaging/center-one.ico"))
                menuGroup = "CenterSystem"
                shortcut = true
            }
        }
    }
}

compose.resources { packageOfResClass = "center.app.resources" }
