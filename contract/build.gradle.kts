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
    implementation("io.provenance.p8e:p8e-contract-base:0.8.+")
    implementation("io.provenance.model:metadata-asset-model:0.1.2")
}