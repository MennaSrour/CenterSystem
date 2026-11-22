import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.zip.ZipFile

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
            implementation(project(":feature:print"))
            implementation(compose.materialIconsExtended)
            implementation(project(":core:database"))
            implementation("com.google.firebase:firebase-admin:9.7.0")
            implementation("org.apache.commons:commons-csv:1.14.1")
            implementation("org.apache.poi:poi-ooxml:5.4.1")
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
            modules("java.sql", "java.naming", "java.prefs", "jdk.crypto.ec", "jdk.unsupported")
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

val bundledServicesDirectory = layout.buildDirectory.dir("generated/bundledServices")
val mergeBundledServices by tasks.registering {
    val libraries = configurations.named("desktopRuntimeClasspath")
    inputs.files(libraries)
    outputs.dir(bundledServicesDirectory)
    doLast {
        val output = bundledServicesDirectory.get().asFile
        output.deleteRecursively()
        val services = mutableMapOf<String, MutableSet<String>>()
        libraries.get().files.filter { it.extension == "jar" }.forEach { library ->
            ZipFile(library).use { archive ->
                archive.entries().asSequence().filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }.forEach { entry ->
                    val providers = services.getOrPut(entry.name) { linkedSetOf() }
                    archive.getInputStream(entry).bufferedReader().useLines { lines ->
                        lines.map { it.substringBefore('#').trim() }.filter(String::isNotEmpty).forEach(providers::add)
                    }
                }
            }
        }
        services.forEach { (path, providers) ->
            val file = output.resolve(path)
            file.parentFile.mkdirs()
            file.writeText(providers.joinToString("\n", postfix = "\n"))
        }
    }
}

// A bundled JAR needs merged service descriptors and cannot reuse dependency signatures.
tasks.withType<Zip>().configureEach {
    if (name == "packageUberJarForCurrentOS") {
        isZip64 = true
        dependsOn(mergeBundledServices)
        from(bundledServicesDirectory)
        exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.EC")
        filesMatching("META-INF/services/**") {
            if (!file.toPath().startsWith(bundledServicesDirectory.get().asFile.toPath())) exclude()
        }
    }
}
