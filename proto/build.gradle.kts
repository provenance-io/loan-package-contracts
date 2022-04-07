import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    }
}

sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
            srcDir("build/generated/source/proto/main/grpc")
        }
    }
}

plugins {
    id("java")
    id("com.google.protobuf") version "0.8.13"
}

dependencies {
    compileOnly("io.provenance.scope:contract-base:0.4.0")
    compileOnly("com.google.protobuf:protobuf-java:3.6.+")
    compileOnly("com.google.protobuf:protobuf-java-util:3.6.+")

    implementation("io.grpc", "grpc-stub", "1.39.0")
    implementation("io.grpc", "grpc-protobuf", "1.39.0") {
        exclude("com.google.protobuf")
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.+"
    }
}