pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// Build-tool classpath only (AGP's/plugins' transitive deps; none of these ship in the APK).
// They resolve to older versions than the Dependabot advisories require, so force the patched ones.
// Drop an entry once the plugins in use resolve to at least that version on their own.
// Defined here (settings scope) and reused by the root build.gradle.kts, because the Gradle
// dependency graph submitted to GitHub is attributed to settings.gradle.kts.
buildscript {
    val patched = mapOf(
        "org.bouncycastle:bcprov-jdk18on" to "1.85",
        "org.bouncycastle:bcpkix-jdk18on" to "1.85",
        "com.google.guava:guava" to "33.7.2-jre",
        "org.bitbucket.b_c:jose4j" to "0.9.6",
        "org.jdom:jdom2" to "2.0.6.1",
        "org.apache.commons:commons-lang3" to "3.18.0",
        "org.apache.httpcomponents:httpclient" to "4.5.14",
        "com.google.protobuf:protobuf-java" to "3.25.5",
        "com.google.protobuf:protobuf-kotlin" to "3.25.5",
    )
    val nettyVersion = "4.1.140.Final"
    val kotlinVersion = "2.4.21"
    val force = Action<DependencyResolveDetails> {
        if (requested.group == "io.netty") {
            useVersion(nettyVersion)
        } else if (requested.group == "org.jetbrains.kotlin" && requested.name.startsWith("kotlin-gradle-plugin")) {
            useVersion(kotlinVersion)
        } else {
            patched["${requested.group}:${requested.name}"]?.let { useVersion(it) }
        }
    }
    gradle.extra["forcePatchedBuildTools"] = force
    configurations.classpath {
        resolutionStrategy.eachDependency(force)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PDF Viewer"
include(":app")
