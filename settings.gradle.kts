pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "CenterSystem"
include(":desktopApp", ":domain", ":core:database", ":core:network", ":core:utils")
include(":feature:reception", ":feature:billing", ":feature:students", ":feature:reports")

include(":core:ui", ":feature:dashboard")

include(":feature:financials")

include(":feature:courses")
