import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

buildscript {
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    }
}

plugins {
    id("java")
    id("com.google.protobuf") version "0.8.13"
}

dependencies {
    compileOnly("com.google.protobuf:protobuf-java:3.6.+")
    compileOnly("com.google.protobuf:protobuf-java-util:3.6.+")
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.+"
    }
}