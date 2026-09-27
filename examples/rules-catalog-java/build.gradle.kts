plugins {
    `java-library`
    alias(libs.plugins.spotless)
}

description = "Rules written in Java against the engine's rule catalog seams."

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    // `release` also validates the JDK API surface, so a call added after 21
    // fails the build here instead of at a consumer's runtime.
    options.release = 21
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

dependencies {
    api(project(":engine"))

    // Rule metadata is data on disk; Jackson binds it to records and stays an
    // implementation detail of this module. It never reaches the engine's API.
    implementation(libs.jackson.dataformat.yaml)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat(libs.versions.googleJavaFormat.get())
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
