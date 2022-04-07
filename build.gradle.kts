import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

allprojects {
    group = "io.provenance.loan-package-contracts"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

subprojects {
    val subProjectName = name

    apply {
        plugin("maven-publish")
        plugin("signing")
        plugin("java-library")
        plugin("org.jetbrains.kotlin.jvm")
    }

    repositories {
        mavenCentral()
    }
}
