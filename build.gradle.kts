// Build-tool classpath only (AGP's transitive deps; none of these ship in the APK). They resolve to
// older versions than the Dependabot advisories require, so force the patched ones. Drop an entry
// once the AGP in use resolves to at least that version on its own.
buildscript {
    val patched = mapOf(
        "org.bouncycastle:bcprov-jdk18on" to "1.85",
        "org.bouncycastle:bcpkix-jdk18on" to "1.85",
        "com.google.guava:guava" to "33.7.2-jre",
        "org.bitbucket.b_c:jose4j" to "0.9.6",
        "org.jdom:jdom2" to "2.0.6.1",
        "org.apache.commons:commons-lang3" to "3.18.0",
        "com.google.protobuf:protobuf-java" to "3.25.5",
        "com.google.protobuf:protobuf-kotlin" to "3.25.5",
    )
    val nettyVersion = "4.1.140.Final"
    configurations.classpath {
        resolutionStrategy.eachDependency {
            if (requested.group == "io.netty") {
                useVersion(nettyVersion)
            } else {
                patched["${requested.group}:${requested.name}"]?.let { useVersion(it) }
            }
        }
    }
}

plugins {
    id("com.android.application") version "9.5.1" apply false
}
