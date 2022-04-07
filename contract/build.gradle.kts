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
    implementation("io.provenance.scope:contract-base:0.4.0")
}