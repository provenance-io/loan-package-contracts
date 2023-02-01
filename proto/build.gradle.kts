import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

buildscript {
    repositories {
        mavenCentral()
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    alias(libs.plugins.protobuf.gradle)
    alias(libs.plugins.protocGen.krotoPlus)
    `maven-publish`
    `java-library`
}

dependencies {
    listOf(
        libs.bundles.protocGen,
        libs.grpc.protobuf,
        libs.grpc.stub,
        libs.metadataAssetModel,
        libs.p8eScopeSdk.contractBase,
    ).forEach(::implementation)

    listOf(
        libs.protobuf.java,
        libs.protobuf.java.util,
    ).forEach(::api)
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.20.1"
    }
}
