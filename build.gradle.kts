import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.provenance.p8e.plugin.P8eLocationExtension
import io.provenance.p8e.plugin.P8ePartyExtension

/** Build setup */

buildscript {
    classpathSpecs(
        Dependencies.SemVer,
        Dependencies.GitHubRelease,
    )
    repositories {
        mavenCentral()
        maven { url = uri(RepositoryLocations.JitPack) }
    }
}

plugins {
    pluginSpecs(
        Plugins.KotlinJvm,
        Plugins.GitHubRelease,
        Plugins.NexusPublishing,
        Plugins.P8ePublishing,
        Plugins.SemVer,
    )
    signing
}

/** Ktlint */

val ktlint by configurations.creating

dependencies {
    ktlint(Dependencies.Ktlint.toDependencyNotation()) {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
    // ktlint(project(":custom-ktlint-ruleset")) // in case of custom ruleset
}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))
val lintingExclusions = listOf(
    "**/ContractHash*.kt",
    "**/ProtoHash*.kt",
)

val ktlintExcludeSyntax: (Collection<String>) -> Collection<String> = { exclusions ->
    exclusions.map { exclusion ->
        "!$exclusion"
    }
}

val ktlintCheck by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("*/src/**/*.kt") + ktlintExcludeSyntax(lintingExclusions)
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "*/src/**/*.kt")
}

/** Project Setup & Releasing */

semver {
    tagPrefix("v")
    initialVersion("0.1.0")
    findProperty("semver.overrideVersion")?.toString()?.let { overrideVersion(it) }
    val semVerModifier = findProperty("semver.modifier")?.toString()?.let { buildVersionModifier(it) } ?: { nextPatch() }
    versionModifier(semVerModifier)
}

val semVersion = semver.version
allprojects {
    group = "io.provenance.loan-package"
    version = semVersion

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile>().all {
        sourceCompatibility = "11"
        sourceCompatibility = "11"
        kotlinOptions {
            languageVersion = "1.6"
            jvmTarget = "11"
        }
    }
}

subprojects {
    val subProjectName = name

    apply {
        plugin("signing")
    }
    java {
        withJavadocJar()
        withSourcesJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "io.provenance.loan-package"
                artifactId = subProjectName

                from(components["java"])

                pom {
                    name.set("Provenance Loan Package Contracts")
                    description.set("P8e Loan Package Contracts for use with p8e-cee-api.")
                    url.set("https://provenance.io")

                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }

                    developers {
                        developer {
                            id.set("cworsnop-figure")
                            name.set("Cody Worsnop")
                            email.set("cworsnop@figure.com")
                        }
                    }

                    scm {
                        connection.set("git@github.com:provenance-io/loan-package-contracts.git")
                        developerConnection.set("git@github.com:provenance-io/loan-package-contracts.git")
                        url.set("https://github.com/provenance-io/loan-package-contracts")
                    }
                }
            }
        }

        signing {
            sign(publishing.publications["maven"])
        }

        tasks.javadoc {
            if(JavaVersion.current().isJava9Compatible) {
                (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri(RepositoryLocations.Sonatype))
            snapshotRepositoryUrl.set(uri(RepositoryLocations.SonatypeSnapshot))
            username.set(findProject("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME"))
            password.set(findProject("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD"))
            stagingProfileId.set("3180ca260b82a7") // prevents querying for the staging profile id, performance optimization
        }
    }
}

val githubTokenValue = findProperty("githubToken")?.toString() ?: System.getenv("GITHUB_TOKEN")

githubRelease {
    token(githubTokenValue)
    owner("provenance-io")
    targetCommitish("main")
    draft(false)
    prerelease(false)
    repo("loan-package-contracts")
    tagName(semver.versionTagName)
    body(changelog())

    overwrite(false)
    dryRun(false)
    apiEndpoint("https://api.github.com")
    client
}

fun p8eParty(publicKey: String): P8ePartyExtension = P8ePartyExtension().also { it.publicKey = publicKey }

val localAudience = mapOf(
    "local1" to p8eParty("0A41042C52EB79307D248B6CFB2A4AF562E403D4826BB0F540F024BBC3937528F6EB0B7FFA7A6585B751DBA25C173E658F3FEAAB0F05980C76E985CE0D55294F3600D7"),
)

val testAudience = mapOf(
    "homebridge" to p8eParty("0A41045D4C8AF091A5013A9C953CFF585A8CF78B1F4292DB8FD954D0D35CE941D5F0630C45E4CC6F139054C7F864C253F91FB188AFEC6C6EB780959C281CE7DA7BAE56"),
    "figure-lending" to p8eParty("0A4104E3065E5AA8BDEEA1BA4FACB5179ACD40F8DC035D7A15D06DEC0D1F0FE6BFCBF3456B2BC4AFC823ED30BA564A1B4F38E7CCA45278521C72A98AFE04C984FC5F33")
)

val prodAudience = mapOf(
    "figure_lending" to p8eParty("0A410417D207BF9381E4288CFA970E9708C13EE047882113D342E2F62C74D391811AF7059A36687BC2A2F3A33DBD0A689F0C8F937FB8301EE5416BC025C512D484A44D"),
)

val localLocations = mapOf(
    "local" to P8eLocationExtension().also {
        it.osUrl = System.getenv("OS_GRPC_URL")
        it.provenanceUrl = System.getenv("PROVENANCE_GRPC_URL")
        it.encryptionPrivateKey = System.getenv("ENCRYPTION_PRIVATE_KEY")
        it.signingPrivateKey = System.getenv("SIGNING_PRIVATE_KEY")
        it.chainId = System.getenv("CHAIN_ID")
        it.txFeeAdjustment = "2.0"

        it.audience = localAudience
    },
)

val prLocations = mapOf(
    "test" to P8eLocationExtension().also {
        it.osUrl = System.getenv("OS_GRPC_URL")
        it.provenanceUrl = System.getenv("PROVENANCE_GRPC_URL")
        it.encryptionPrivateKey = System.getenv("ENCRYPTION_PRIVATE_KEY")
        it.signingPrivateKey = System.getenv("SIGNING_PRIVATE_KEY")
        it.chainId = "pio-testnet-1"
        it.txFeeAdjustment = "2.0"
        it.txBatchSize = "3"
        it.osHeaders = mapOf(
            "apikey" to System.getenv("OS_GRPC_APIKEY")
        )

        it.audience = testAudience
    },
)

val releaseLocations = mapOf(
    "production" to P8eLocationExtension().also {
        it.osUrl = System.getenv("OS_GRPC_URL")
        it.provenanceUrl = System.getenv("PROVENANCE_GRPC_URL")
        it.encryptionPrivateKey = System.getenv("ENCRYPTION_PRIVATE_KEY")
        it.signingPrivateKey = System.getenv("SIGNING_PRIVATE_KEY")
        it.chainId = "pio-mainnet-1"
        it.mainNet = true
        it.txFeeAdjustment = "2.0"
        it.txBatchSize = "1"
        it.osHeaders = mapOf(
            "apikey" to System.getenv("OS_GRPC_APIKEY")
        )

        it.audience = prodAudience
    },
)

p8e {
    // Package locations that the ContractHash and ProtoHash source files will be written to.
    language = "kt" // defaults to "java"
    contractHashPackage = "io.provenance.scope.loan.contracts"
    protoHashPackage = "io.provenance.scope.loan.proto"
    includePackages = arrayOf("io", "com", "tech.figure")

    locations = if (System.getenv("IS_TEST") == "false") {
        releaseLocations
    } else if (System.getenv("IS_TEST") == "true") {
        prLocations
    } else {
        localLocations
    }
}
