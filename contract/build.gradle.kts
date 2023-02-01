buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("kotlin")
    `java-library`
    `maven-publish`
}

dependencies {
    api(project(":proto"))

    listOf(
        libs.metadataAssetModel,
        libs.p8eScopeSdk.contractBase,
        libs.p8eScopeSdk.util,
    ).forEach(::implementation)

    listOf(
        libs.bundles.jackson,
        libs.bundles.kotest,
    ).forEach(::testImplementation)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
