@file:Suppress("UnstableApiUsage", "Property_Name")

import dev.deftu.gradle.utils.GameSide
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.tasks.Jar

plugins {
    java
    val dgtVersion = "2.35.0"
    id("dev.deftu.gradle.tools") version(dgtVersion)
    id("dev.deftu.gradle.tools.resources") version(dgtVersion)
    id("dev.deftu.gradle.tools.bloom") version(dgtVersion)
    id("dev.deftu.gradle.tools.shadow") version(dgtVersion)
    id("dev.deftu.gradle.tools.minecraft.loom") version(dgtVersion)
    id("dev.deftu.gradle.tools.minecraft.releases") version(dgtVersion)
}

val modName = providers.gradleProperty("mod.name").get()
val modId = providers.gradleProperty("mod.id").get()
val modVersion = providers.gradleProperty("mod.version").get()
val modGroup = providers.gradleProperty("mod.group").get()

group = modGroup
version = modVersion

toolkitLoomHelper {
    // OneConfig V0 (cc.polyfrost) is declared manually in `dependencies` below.
    // Do NOT call useOneConfig {} here: that is the V1 integration and requests
    // org.polyfrost.oneconfig:<mc>-forge:<ver>, which is not published for legacy Forge.

    useDevAuth("1.2.1")

    disableRunConfigs(GameSide.SERVER)

    useMixinRefMap(modData.id)

    if (mcData.isForge) {
        useForgeMixin(modData.id)
    }
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    // Mixin API for compiling the mixin classes. V0 OneConfig ships Mixin at runtime only,
    // and PolyMixin (which used to supply this) was removed along with useOneConfig.
    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")

    // OneConfig V0: compile against it only. The launchwrapper downloads/loads the real OneConfig at runtime.
    compileOnly("cc.polyfrost:oneconfig-1.12.2-forge:0.2.2-alpha223")

    // Launchwrapper tweaker: on the dev classpath (so --tweakClass resolves) and bundled into the jar.
    implementation("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")
    include("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.encoding = "UTF-8"
}

tasks.withType<JavaExec>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(8))
    })
}

tasks.withType<Jar>().configureEach {
    manifest.attributes["MixinConfigs"] = "mixins.hitspan.json"
    manifest.attributes["ModSide"] = "CLIENT"
    manifest.attributes["TweakOrder"] = 0
    manifest.attributes["ForceLoadAsMod"] = true
    manifest.attributes["TweakClass"] = "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker"
    manifest.attributes.remove("Class-Path")
}

configurations.all {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")

    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
}

tasks.named<JavaExec>("runClient") {
    args("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")
    jvmArgs(
        "-Ddevauth.enabled=true",
        "-Ddevauth.account=main"
    )
}

/* Version sync: gradle.properties -> generated HitSpan.java + mcmod.info */

val generatedDir = layout.buildDirectory.dir("generated/sources/versionedHitSpan")

val generateVersionedHitSpan = tasks.register<Copy>("generateVersionedHitSpan") {
    from("src/main/java/me/bewf/hitspan/HitSpan.java")
    into(generatedDir.map { it.dir("me/bewf/hitspan") })

    filteringCharset = "UTF-8"

    filter { line: String ->
        line
            .replace("@MOD_NAME@", modName)
            .replace("@MOD_ID@", modId)
            .replace("@MOD_VERSION@", modVersion)
    }
}

val mainJavaWithoutHitSpan = fileTree("src/main/java") {
    include("**/*.java")
    exclude("me/bewf/hitspan/HitSpan.java")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(generateVersionedHitSpan)
    setSource(mainJavaWithoutHitSpan + fileTree(generatedDir))
}

tasks.processResources {
    inputs.property("mod_name", modName)
    inputs.property("mod_id", modId)
    inputs.property("mod_version", modVersion)

    filesMatching("mcmod.info") {
        expand(
            mapOf(
                "mod_name" to modName,
                "mod_id" to modId,
                "mod_version" to modVersion
            )
        )
    }
}
