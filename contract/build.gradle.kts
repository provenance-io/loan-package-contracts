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
    implementation("io.provenance.scope:contract-base:0.4.9")
    implementation("io.provenance.scope:util:0.4.9")
    implementation("io.provenance.model:metadata-asset-model:0.1.6") {
        exclude("com.google.protobuf", "protobuf-java")
        exclude("com.google.protobuf", "protobuf-java-util")
        exclude("io.grpc", "grpc-protobuf")
    }
}