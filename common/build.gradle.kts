plugins {
    id("multiloader-base")
    id("java-library")

    id("net.fabricmc.fabric-loom-remap") version ("1.16.1")
}

base {
    archivesName = "sodium-common"
}

val configurationPreLaunch = configurations.create("preLaunchDeps") {
    isCanBeResolved = true
}

sourceSets {
    val main = getByName("main")
    val api = create("api")
    val boot = create("boot")

    api.apply {
        java {
            compileClasspath += main.compileClasspath
        }
    }

    boot.apply {
        java {
            compileClasspath += configurationPreLaunch
        }
    }

    main.apply {
        java {
            compileClasspath += api.output
            compileClasspath += boot.output
        }
    }

    create("desktop")
}

repositories {
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${BuildConfig.MINECRAFT_VERSION}")
    mappings(loom.layered {
        officialMojangMappings()

        if (BuildConfig.PARCHMENT_VERSION != null) {
            parchment("org.parchmentmc.data:parchment-${BuildConfig.MINECRAFT_VERSION}:${BuildConfig.PARCHMENT_VERSION}@zip")
        }
    })

    compileOnly("io.github.llamalad7:mixinextras-common:0.5.3")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.3")

    compileOnly("net.fabricmc:sponge-mixin:0.13.2+mixin.0.8.5")
    compileOnly("net.fabricmc:fabric-loader:${BuildConfig.FABRIC_LOADER_VERSION}")

    fun addDependentFabricModule(name: String) {
        modCompileOnly(fabricApi.module(name, BuildConfig.FABRIC_API_VERSION))
    }

    addDependentFabricModule("fabric-api-base")
    addDependentFabricModule("fabric-block-view-api-v2")
    addDependentFabricModule("fabric-renderer-api-v1")
    addDependentFabricModule("fabric-rendering-data-attachment-v1")

    modCompileOnly("net.fabricmc.fabric-api:fabric-renderer-api-v1:3.2.9+1172e897d7")

    // We need to be careful during pre-launch that we don't touch any Minecraft classes, since other mods
    // will not yet have an opportunity to apply transformations.
    configurationPreLaunch("org.lwjgl:lwjgl:3.3.3")
    configurationPreLaunch("org.lwjgl:lwjgl-opengl:3.3.3")
    configurationPreLaunch("net.java.dev.jna:jna:5.14.0")
    configurationPreLaunch("net.java.dev.jna:jna-platform:5.14.0")
    configurationPreLaunch("org.slf4j:slf4j-api:2.0.9")
    configurationPreLaunch("org.jetbrains:annotations:24.1.0")
}

loom {
    accessWidenerPath = file("src/main/resources/sodium-common.accesswidener")

    mixin {
        useLegacyMixinAp = false
    }
}

fun exportSourceSetJava(name: String, sourceSet: SourceSet) {
    val configuration = configurations.create("${name}Java") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }

    val compileTask = tasks.getByName<JavaCompile>(sourceSet.compileJavaTaskName)
    artifacts.add(configuration.name, compileTask.destinationDirectory) {
        builtBy(compileTask)
    }
}

fun exportSourceSetSources(name: String, sourceSet: SourceSet) {
    val configuration = configurations.create("${name}Sources") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }

    val compileTask = tasks.register<Copy>(sourceSet.getTaskName("process", "sources")) {
        from(sourceSet.allSource)
        into(file(project.layout.buildDirectory).resolve("sources").resolve(sourceSet.name))
    }.get()
    artifacts.add(configuration.name, compileTask.destinationDir) {
        builtBy(compileTask)
    }
}

fun exportSourceSetResources(name: String, sourceSet: SourceSet) {
    val configuration = configurations.create("${name}Resources") {
        isCanBeResolved = true
        isCanBeConsumed = true
    }

    val compileTask = tasks.getByName<ProcessResources>(sourceSet.processResourcesTaskName)
    compileTask.apply {
        exclude("**/README.txt")
        exclude("/*.accesswidener")
    }

    artifacts.add(configuration.name, compileTask.destinationDir) {
        builtBy(compileTask)
    }
}

// Exports the compiled output of the source set to the named configuration.
fun exportSourceSet(name: String, sourceSet: SourceSet) {
    exportSourceSetJava(name, sourceSet)
    exportSourceSetSources(name, sourceSet)
    exportSourceSetResources(name, sourceSet)
}

exportSourceSet("commonMain", sourceSets["main"])
exportSourceSet("commonApi", sourceSets["api"])
exportSourceSet("commonBoot", sourceSets["boot"])
exportSourceSet("commonDesktop", sourceSets["desktop"])

tasks.jar { enabled = false }
tasks.remapJar { enabled = false }

// Regression tests exercise emitted vertices without starting a Minecraft client.
sourceSets.named("test") {
    compileClasspath += sourceSets["api"].output
    compileClasspath += sourceSets["main"].compileClasspath
    runtimeClasspath += sourceSets["api"].output
    runtimeClasspath += sourceSets["main"].compileClasspath
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.15.2")
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch")
    val nativePlatform = if (os.contains("win")) "windows" else if (os.contains("mac")) "macos" else "linux"
    val nativeArch = if (arch == "aarch64") "-arm64" else ""
    testRuntimeOnly("org.lwjgl:lwjgl:3.3.3:natives-$nativePlatform$nativeArch")
}

tasks.test {
    useJUnitPlatform()
    workingDir = layout.buildDirectory.dir("test-run").get().asFile
    doFirst { workingDir.mkdirs() }
}
