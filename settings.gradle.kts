pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
rootProject.name = "loan-package-contracts"
include("contract", "proto")
