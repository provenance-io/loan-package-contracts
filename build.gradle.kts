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
    "local1" to p8eParty("0A41046C57E9E25101D5E553AE003E2F79025E389B51495607C796B4E95C0A94001FBC24D84CD0780819612529B803E8AD0A397F474C965D957D33DD64E642B756FBC4"),
    "local2" to p8eParty("0A4104D630032378D56229DD20D08DBCC6D31F44A07D98175966F5D32CD2189FD748831FCB49266124362E56CC1FAF2AA0D3F362BF84CACBC1C0C74945041EB7327D54"),
    "local3" to p8eParty("0A4104CD5F4ACFFE72D323CCCB2D784847089BBD80EC6D4F68608773E55B3FEADC812E4E2D7C4C647C8C30352141D2926130D10DFC28ACA5CA8A33B7BD7A09C77072CE"),
    "local4" to p8eParty("0A41045E4B322ED16CD22465433B0427A4366B9695D7E15DD798526F703035848ACC8D2D002C1F25190454C9B61AB7B243E31E83BA2B48B8A4441F922A08AC3D0A3268"),
    "local5" to p8eParty("0A4104A37653602DA20D27936AF541084869B2F751953CB0F0D25D320788EDA54FB4BC9FB96A281BFFD97E64B749D78C85871A8E14AFD48048537E45E16F3D2FDDB44B"),
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