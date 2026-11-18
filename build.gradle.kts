// Patched versions for the build-tool classpath are defined in settings.gradle.kts.
buildscript {
    @Suppress("UNCHECKED_CAST")
    val force = gradle.extra["forcePatchedBuildTools"] as Action<DependencyResolveDetails>
    configurations.classpath {
        resolutionStrategy.eachDependency(force)
    }
}

plugins {
    id("com.android.application") version "9.5.1" apply false
}
