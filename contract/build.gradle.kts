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
}
