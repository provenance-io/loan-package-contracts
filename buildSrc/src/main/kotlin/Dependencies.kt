import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.ScriptHandlerScope
import org.gradle.kotlin.dsl.exclude
import org.gradle.plugin.use.PluginDependenciesSpec

object Versions {
    const val Kotlin = "1.6.21"
    const val GitHubRelease = "2.2.12"
    const val Kotest = "5.2.3"
    const val KrotoPlus = "0.6.1"
    object Plugins {
        const val NexusPublishing = "1.1.0"
        const val P8ePublishing = "0.6.4"
        const val Protobuf = "0.8.18"
        const val SemVer = "0.3.10"
    }
    object Dependencies {
        const val Grpc = "1.39.0"
        const val ProtocGenValidate = "0.6.7"
        const val Protobuf = "3.20.+"
        const val SemVer = "1.1.2"
        object Provenance {
            const val Scope = "0.4.9"
            const val MetadataAssetModel = "0.1.6"
        }
    }
}

object Plugins {
    val Kotlin = PluginSpec("kotlin")
    val KotlinJvm = PluginSpec("org.jetbrains.kotlin.jvm", Versions.Kotlin)
    val NexusPublishing = PluginSpec("io.github.gradle-nexus.publish-plugin", Versions.Plugins.NexusPublishing)
    val GitHubRelease = PluginSpec("com.github.breadmoirai.github-release", Versions.GitHubRelease)
    val P8ePublishing = PluginSpec("io.provenance.p8e.p8e-publish", Versions.Plugins.P8ePublishing)
    val SemVer = PluginSpec("io.github.nefilim.gradle.semver-plugin", Versions.Plugins.SemVer)
    val Protobuf = PluginSpec("com.google.protobuf", Versions.Plugins.Protobuf)
    val KrotoPlus = PluginSpec("com.github.marcoferrer.kroto-plus", Versions.KrotoPlus)
}

object Dependencies {
    // Build
    val GitHubRelease = DependencySpec(
        name = "com.github.breadmoirai:github-release",
        version = Versions.GitHubRelease,
    )
    val SemVer = DependencySpec(
        name = "net.swiftzer.semver:semver",
        version = Versions.Dependencies.SemVer,
    )
    // Testing
    object Kotest {
        val Framework = DependencySpec(
            name = "io.kotest:kotest-runner-junit5",
            version = Versions.Kotest,
        )
        val Assertions = DependencySpec(
            name = "io.kotest:kotest-assertions-core",
            version = Versions.Kotest,
        )
        val Property = DependencySpec(
            name = "io.kotest:kotest-property",
            version = Versions.Kotest,
        )
    }

    // Dependencies
    object Grpc {
        val Stub = DependencySpec(
            name = "io.grpc:grpc-stub",
            version = Versions.Dependencies.Grpc,
        )
        val Protobuf = DependencySpec(
            name = "io.grpc:grpc-protobuf",
            version = Versions.Dependencies.Grpc,
            exclude = setOf(
                "com.google.protobuf:protobuf-java",
            )
        )
    }
    object Protobuf {
        val Java = DependencySpec(
            name = "com.google.protobuf:protobuf-java",
            version = Versions.Dependencies.Protobuf,
        )
        val JavaUtil = DependencySpec(
            name = "com.google.protobuf:protobuf-java-util",
            version = Versions.Dependencies.Protobuf,
            exclude = setOf(
                "com.google.protobuf:protobuf-java",
            ),
        )
        val Protoc = DependencySpec(
            name = "com.google.protobuf:protoc",
            version = Versions.Dependencies.Protobuf,
        )
    }
    object ProtocGen {
        val ValidateBase = DependencySpec(
            name = "io.envoyproxy.protoc-gen-validate:protoc-gen-validate",
            version = Versions.Dependencies.ProtocGenValidate,
        )
        val ValidateJavaStub = DependencySpec(
            name = "io.envoyproxy.protoc-gen-validate:pgv-java-stub",
            version = Versions.Dependencies.ProtocGenValidate,
        )
        val KrotoPlus = DependencySpec(
            name = "com.github.marcoferrer.krotoplus:protoc-gen-kroto-plus",
            version = Versions.KrotoPlus,
        )
    }
    object Provenance {
        val ContractBase = DependencySpec(
            name = "io.provenance.scope:contract-base",
            version = Versions.Dependencies.Provenance.Scope,
            exclude = setOf(
                "com.google.protobuf:protobuf-java",
            ),
        )
        val ScopeUtil = DependencySpec(
            name = "io.provenance.scope:util",
            version = Versions.Dependencies.Provenance.Scope,
        )
        val MetadataAssetModel = DependencySpec(
            name = "io.provenance.model:metadata-asset-model",
            version = Versions.Dependencies.Provenance.MetadataAssetModel,
            exclude = setOf(
                "com.google.protobuf:protobuf-java",
                "com.google.protobuf:protobuf-java-util",
                "io.grpc:grpc-protobuf",
            ),
        )
    }
}

data class PluginSpec(
    val id: String,
    val version: String? = ""
) {
    fun addTo(scope: PluginDependenciesSpec) {
        scope.apply {
            id(id).also { spec ->
                version?.takeIf { it.isNotBlank() }?.let { versionString ->
                    spec.version(versionString)
                }
            }
        }
    }

    fun addTo(action: ObjectConfigurationAction) {
        action.plugin(this.id)
    }
}

data class DependencySpec(
    val name: String,
    val version: String = "",
    val isChanging: Boolean = false,
    val exclude: Collection<String> = emptySet()
) {
    fun plugin(scope: PluginDependenciesSpec) {
        scope.apply {
            id(name).also { spec ->
                version.takeIf { it.isNotBlank() }?.let { versionString ->
                    spec.version(versionString)
                }
            }
        }
    }

    fun classpath(scope: ScriptHandlerScope) {
        val spec = this
        with(scope) {
            dependencies {
                classpath(spec.toDependencyNotation())
            }
        }
    }

    private fun excludeTransitivesFrom(dependency: ExternalModuleDependency) =
        exclude.forEach { exclusionDependencyNotation ->
            val (group, module) = exclusionDependencyNotation.split(":", limit = 2)
            dependency.exclude(group = group, module = module)
        }

    fun api(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "api".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                excludeTransitivesFrom(this)
            }
        }
    }

    fun implementation(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "implementation".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                excludeTransitivesFrom(this)
            }
        }
    }

    fun testImplementation(handler: DependencyHandlerScope) {
        val spec = this
        with(handler) {
            "testImplementation".invoke(spec.toDependencyNotation()) {
                isChanging = spec.isChanging
                excludeTransitivesFrom(this)
            }
        }
    }

    fun toDependencyNotation(): String =
        listOfNotNull(
            name,
            version.takeIf { it.isNotEmpty() }
        ).joinToString(":")
}

fun DependencyHandlerScope.apiSpecs(vararg specs: DependencySpec) = specs.forEach { spec ->
    spec.api(this)
}

fun DependencyHandlerScope.implementationSpecs(vararg specs: DependencySpec) = specs.forEach { spec ->
    spec.implementation(this)
}

fun DependencyHandlerScope.testImplementationSpecs(vararg specs: DependencySpec) = specs.forEach { spec ->
    spec.testImplementation(this)
}

fun ObjectConfigurationAction.pluginSpecs(vararg specs: PluginSpec) = specs.forEach { spec ->
    spec.addTo(this)
}

fun PluginDependenciesSpec.pluginSpecs(vararg specs: PluginSpec) = specs.forEach { spec ->
    spec.addTo(this)
}

fun ScriptHandlerScope.classpathSpecs(vararg specs: DependencySpec) = specs.forEach { spec ->
    spec.classpath(this)
}
