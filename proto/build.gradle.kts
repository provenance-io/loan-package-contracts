import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

buildscript {
    repositories {
        mavenCentral()
    }
}

@Suppress("DSL_SCOPE_VIOLATION") // https://github.com/gradle/gradle/issues/22797
plugins {
    id("kotlin")
    alias(libs.plugins.protobuf.gradle)
    alias(libs.plugins.protocGen.krotoPlus)
    `maven-publish`
    `java-library`
}

dependencies {
    listOf(
        libs.bundles.protocGen,
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
        artifact = libs.protoc.get().toString()
    }
}
