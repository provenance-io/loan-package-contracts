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
    pluginSpecs(
        Plugins.Kotlin,
        Plugins.Protobuf,
        Plugins.KrotoPlus,
    )
    `maven-publish`
    `java-library`
}

dependencies {
    implementationSpecs(
        Dependencies.Provenance.ContractBase,
        Dependencies.Provenance.MetadataAssetModel,
        Dependencies.ProtocGen.ValidateBase,
        Dependencies.ProtocGen.ValidateJavaStub,
        Dependencies.ProtocGen.KrotoPlus,
        Dependencies.Grpc.Stub,
        Dependencies.Grpc.Protobuf,
    )

    apiSpecs(
        Dependencies.Protobuf.Java,
        Dependencies.Protobuf.JavaUtil,
    )
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.20.0"
    }
}