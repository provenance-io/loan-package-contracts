import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.provenance.p8e.plugin.P8eLocationExtension
import io.provenance.p8e.plugin.P8ePartyExtension

buildscript {
    dependencies {
        classpathSpecs(
            Dependencies.SemVer,
            Dependencies.GitHubRelease,
        )
    }
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

    tasks.withType<KotlinCompile>().all {
        kotlinOptions {
            jvmTarget = "11"
        }
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
    "figure" to p8eParty("0A410417D6AC56BA61105E3D53F1FF92FAE7EC6E2E965C7047F09D48D621202400CB9864A11288F9AF353E0D8DDC60899122E93534FF96F579EC84DF2A096ABE0ABB96"),
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