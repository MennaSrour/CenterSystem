import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kmp) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint) apply false
}
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.5.0")
        filter {
            exclude { it.file.toPath().startsWith(layout.buildDirectory.get().asFile.toPath()) }
        }
    }
    tasks.withType<Test>().configureEach {
        // Skiko extracts native rendering libraries into this directory. CI homes may be
        // read-only or absent, so keep screenshot tests independent of user.home.
        val skikoDataDirectory = layout.buildDirectory.dir("tmp/skiko").get().asFile
        doFirst { skikoDataDirectory.mkdirs() }
        jvmArgs("-Dskiko.data.path=${skikoDataDirectory.absolutePath}")
    }
}
