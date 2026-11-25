// Keep the build plugin classpath on patched transitive dependency versions.
buildscript {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            when {
                requested.group == "io.netty" -> useVersion("4.1.137.Final")
                requested.group == "com.google.guava" && requested.name == "guava" -> useVersion("33.7.2-jre")
                requested.group == "com.google.protobuf" && requested.name in setOf("protobuf-java", "protobuf-java-util", "protobuf-kotlin") -> useVersion("3.25.5")
                requested.group == "org.bouncycastle" && requested.name in setOf("bcprov-jdk18on", "bcpkix-jdk18on", "bcutil-jdk18on") -> useVersion("1.86")
                requested.group == "org.apache.commons" && requested.name == "commons-compress" -> useVersion("1.26.0")
                requested.group == "org.bitbucket.b_c" && requested.name == "jose4j" -> useVersion("0.9.6")
                requested.group == "org.jdom" && requested.name == "jdom2" -> useVersion("2.0.6.1")
                requested.group == "org.apache.httpcomponents" && requested.name == "httpclient" -> useVersion("4.5.14")
            }
        }
    }
}

plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
