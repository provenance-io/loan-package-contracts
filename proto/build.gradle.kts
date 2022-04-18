import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.18")
    }
}

plugins {
    id("kotlin")
    id("com.google.protobuf") version "0.8.18"
}

dependencies {
    implementation("io.provenance.scope:contract-base:0.4.9") {
        exclude("com.google.protobuf", "protobuf-java")
    }

    api("com.google.protobuf:protobuf-java:3.20.0")
    api("com.google.protobuf:protobuf-java-util:3.20.0") {
        exclude("com.google.protobuf", "protobuf-java")
    }

    implementation("io.grpc", "grpc-stub", "1.39.0")
    implementation("io.grpc", "grpc-protobuf", "1.39.0") {
        exclude("com.google.protobuf")
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.20.0"
    }
}