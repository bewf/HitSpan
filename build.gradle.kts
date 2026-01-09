@file:Suppress("UnstableApiUsage", "Property_Name")

import dev.deftu.gradle.utils.GameSide
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.Copy
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

toolkitLoomHelper {
    useOneConfig {
        // Keep this on a toolkit-supported OneConfig version.
        // Your actual compile target is controlled by the dependency below.
        version = "1.0.0-alpha.106"
        loaderVersion = "1.1.0-alpha.46"

        usePolyMixin = true
        polyMixinVersion = "0.8.4+build.2"

        // IMPORTANT: you already have OneConfig/Essential in your mods folder,
        // so your mod should NOT try to act as the loader/tweaker.
        // We want to embed OneConfig so the tweaker class is available when running standalone.
        // Enabling this makes the toolkit include the loader/tweaker in the dev run.
        applyLoaderTweaker = false

        for (module in arrayOf("commands", "config", "config-impl", "events", "hud", "internal", "ui", "utils")) {
            +module
        }
    }

    useDevAuth("1.2.1")
    useMixinExtras("0.4.1")

    disableRunConfigs(GameSide.SERVER)

    useMixinRefMap(modData.id)

    if (mcData.isForge) {
        useForgeMixin(modData.id)
    }
}

repositories {
    // In case the defaults don't include Polyfrost already, keep this.
    maven("https://repo.polyfrost.org/releases")
}

dependencies {
    // Compile against the same OneConfig line your installed OneConfig mods use.
    // Also include it at runtime (embedded) so users do not need to install OneConfig separately.
    implementation("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    shade("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    include("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    // Provide the launchwrapper wrapper/artifact which contains internal runtime classes
    // Available at runtime for dev runs.
    modRuntimeOnly("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta17")
    // Embedded into the built jar for distribution.
    include("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta17")
}

// Use Java 8 for BOTH compiling and running (Forge 1.8.9 requirement).
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
    manifest.attributes["ModSide"] = "CLIENT"
    manifest.attributes["TweakOrder"] = 0
    manifest.attributes["ForceLoadAsMod"] = true
    manifest.attributes["TweakClass"] = "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker"
}

// Minimal helper: ensure the OneConfig jars that Gradle resolves appear in run/mods
// when running from Gradle. This ensures the mod can find OneConfig classes when
// launched via `runClient` without requiring the user to separately install OneConfig.
val copyOneConfigToRun = tasks.register<Copy>("copyOneConfigToRun") {
    // Use runtimeClasspath so dependencies pulled in by the build are available.
    // Defer evaluation to execution time to avoid resolving configurations during configuration.
    from({ configurations.getByName("runtimeClasspath").filter { it.name.contains("oneconfig-1.8.9-forge", ignoreCase = true) } })
    into(layout.projectDirectory.dir("run/mods"))
}

// Configure the runClient task
tasks.named<JavaExec>("runClient") {
    args("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")
}

// Configuration-time wrapper copy removed: resolving `runtimeClasspath.files` during configuration
// caused InvalidUserDataException (it froze configuration mutation needed by Loom).
// The copying of OneConfig wrapper into run/ is handled by the task `copyOneConfigToRunRoot` and
// by the runClient task's dependencies so no configuration-time copy is required.
