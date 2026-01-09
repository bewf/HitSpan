@file:Suppress("UnstableApiUsage", "PropertyName")

import dev.deftu.gradle.utils.GameSide
import org.gradle.jvm.toolchain.JavaLanguageVersion

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
    modCompileOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    // This prevents missing classes like org/polyfrost/oneconfig/api/config/v1/Node.
    // Make OneConfig available at runtime when running from Gradle so the mod can load.
    modRuntimeOnly("cc.polyfrost:oneconfig-1.8.9-forge:0.2.2-alpha+")
    // Provide the launchwrapper wrapper/artifact which contains internal runtime classes
    modRuntimeOnly("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta17")
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
