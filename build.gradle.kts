import io.provenance.p8e.plugin.P8eLocationExtension
import io.provenance.p8e.plugin.P8ePartyExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Build setup */

buildscript {
    repositories {
        mavenCentral()
        maven { url = uri(RepositoryLocations.JitPack) }
    }
}

plugins {
    pluginSpecs(
        Plugins.KotlinJvm,
        Plugins.Kover,
        Plugins.GitHubRelease,
        Plugins.NexusPublishing,
        Plugins.P8ePublishing,
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
    Dependencies.Provenance.BouncyCastleProvider
    Dependencies.Provenance.BouncyCastle
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
    args = listOf("-F", "*/src/**/*.kt") + ktlintExcludeSyntax(lintingExclusions)
}

allprojects {
    group = "io.provenance.loan-package"

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<KotlinCompile>().all {
        sourceCompatibility = "11"
        targetCompatibility = "11"
        kotlinOptions {
            apiVersion = "1.6"
            languageVersion = "1.6"
            jvmTarget = "11"
            freeCompilerArgs = listOf(
                "-Xcontext-receivers"
            )
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
            if (JavaVersion.current().isJava9Compatible) {
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

fun p8eParty(publicKey: String): P8ePartyExtension = P8ePartyExtension().also { it.publicKey = publicKey }

/* ktlint-disable max-line-length */
val localAudience = mapOf(
    "local-originator" to p8eParty("0A4104CCB0868FE11FA3A1B079652BAD1C062B29E16630FD50A06EEB3CDA2B15B86AE330B86FEEF85CB4BDAB9F8E6F8CE0969446B8BB326875A329CB73BF2EC5D060DE"),
    "local-servicer" to p8eParty("0A41049CE9928539154A24ED5BEAFAB2A76BA20CAAFEF3AB1B7657C75D6577E6C03FE98D20A1A240C6CDB06826CDF1C0C8E8794918787C28DCF4D8EE1E83A5F0B90EE9"),
    "local-dart" to p8eParty("0A4104C51E49E4F0ABA2FD5B8CF99445D6D6C385164DBC8F35E7374CAC241D4155ADC48EF9B199F799DC865EC24AF54376CF5DD29A1287F1FD3410709A62F5DDE49349"),
    "local-portfolio-manager" to p8eParty("0A41042C52EB79307D248B6CFB2A4AF562E403D4826BB0F540F024BBC3937528F6EB0B7FFA7A6585B751DBA25C173E658F3FEAAB0F05980C76E985CE0D55294F3600D7"),
    "local-controller-a" to p8eParty("0A4104D7820B3244C3F72A1D2631E089E6C40D7D8C88221E771ED631402AC025E59D9CFF82078F4492E231691A6C4D1D36F085CD7B3ED699C35C685E462E4106C13A1C"),
    "local-controller-b" to p8eParty("0A41042CBCF196465B44C6F4245526118E0EBBB13EDC028F07B7A0DD695524001F03BF868D0AAA42462A662253EA39CEA5913508458A9BF008039844D05CCFEAB3A3F0"),
    "local-validator" to p8eParty("0A4104D01052F8C39AED352D9656D5B10DD6D4ADCDD015374B02474C0F0485BE376DDF87D1C54ADF4001D1CE6FF4C65BF6149C90D67BAADA035E1935DA194A128863FD"),
    "local-investor" to p8eParty("0A41044F0D7BD99105FFA7119D49BE2F939CAFAACBFB34010399D442A43B45CD38B985B5A80E258B8B7E3B79EE9F937FE6F1383BFD6B46E7B5CFDFFADC4D52ECB6EB13"),
)

val testAudience = mapOf(
    "homebridge" to p8eParty("0A41045D4C8AF091A5013A9C953CFF585A8CF78B1F4292DB8FD954D0D35CE941D5F0630C45E4CC6F139054C7F864C253F91FB188AFEC6C6EB780959C281CE7DA7BAE56"),
    "figure-lending" to p8eParty("0A4104E3065E5AA8BDEEA1BA4FACB5179ACD40F8DC035D7A15D06DEC0D1F0FE6BFCBF3456B2BC4AFC823ED30BA564A1B4F38E7CCA45278521C72A98AFE04C984FC5F33"),
    "staircase" to p8eParty("0A41044CD14740D696ECD3D1597A6BBB9E42B94B8E93F9331162B3BF93F43CBB5CFD63B960508892CF370523228AA0597573376A58E2D1A34F1F8429B08C1835868D4C"),
    "figure_servicing" to p8eParty("0A41046DAF83881A4DD0302C16214449E5B0BFA4CDC83D39C8B3F281899768733D43B959750BB5EF96A6A115C54FBAA08F1EEE7212EDEE46A5A00EB18E2A98D28351B5"),
    "leahs_lending_org_demo" to p8eParty("0A4104A9F4E074055E5BFF60FBCEAE03FE11E84F33E0647364F6F772613C9A8D2A0690EB4AA13A84F1F4322CB90B7934A96C8CC246820087E7A03E7D9CA8E1781B2CC0"),
    "guaranteed_rate" to p8eParty("0A410450E9830F0F27DF8D9056D0123F0551552C92CB0D2E4A7007E25DA10DB4C19E89CAE81092875399228B316D5022047923190074CCE89E43467E5509D95597EDEB"),
    "escrowtab" to p8eParty("0A4104AFE00649C3530F2FE7083DACC9EDDD6CD556C0DD0A33B62676DF7F949EEC4EAB217CB03BED222697796477567C18C7969EE8ACC1434CA29115C2E2780B09D0D3")
)

val prodAudience = mapOf(
    "figure_lending" to p8eParty("0A410417D207BF9381E4288CFA970E9708C13EE047882113D342E2F62C74D391811AF7059A36687BC2A2F3A33DBD0A689F0C8F937FB8301EE5416BC025C512D484A44D"),
    "figure_servicing" to p8eParty("0A410480031287B7BCBAEF26A81A408D167F948A386FCB1EE56B3BD477D5AE4179D9D7B59C6E7E791A0BB94A11BFF7EA0A5D2CAECFA8F2DDC566C83012AEE256670FF9"),
    "figure_servicing_old" to p8eParty("0A410457C9A540DCC947F2715C4D22437C127EC0A67A3A4F77533F5D0A94BAF6BE93CC7D870EAECBC212601428A7194D4F0182C89AF0665899F11004D839205858B9B9"),
)
/* ktlint-enable max-line-length */

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
