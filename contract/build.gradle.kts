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

    implementationSpecs(
        Dependencies.Provenance.ContractBase,
        Dependencies.Provenance.ScopeUtil,
        Dependencies.Provenance.MetadataAssetModel,
    )

    testImplementationSpecs(
        Dependencies.Kotest.Framework,
        Dependencies.Kotest.Assertions,
        Dependencies.Kotest.Property,
    )
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
