import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.gradle.CyclonedxPlugin

initscript {
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.cyclonedx.bom:org.cyclonedx.bom.gradle.plugin:3.4.0")
    }
}

rootProject {
    apply<CyclonedxPlugin>()
}

allprojects {
    tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
        includeBuildEnvironment = true
        testConfigs = listOf("(?i).*test.*")
    }
}
