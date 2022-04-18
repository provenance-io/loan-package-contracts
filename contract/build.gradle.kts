buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("kotlin")
}

dependencies {
    api(project(":proto"))
    implementation("io.provenance.scope:contract-base:0.4.9")
    implementation("io.provenance.scope:util:0.4.9")
    implementation("io.provenance.model:metadata-asset-model:loan-wrapper") { // TODO: Change from prerelease artifact
        exclude("com.google.protobuf", "protobuf-java")
        exclude("com.google.protobuf", "protobuf-java-util")
        exclude("io.grpc", "grpc-protobuf")
    }
}