import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.kapt") version "2.3.0"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

group = "com.gg.example"
version = "1.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly(files("libs/api-0.1.0-dev20.jar"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    kapt(files("libs/api-0.1.0-dev20.jar"))
    compileOnly("org.pf4j:pf4j:3.12.0")
    kapt("org.pf4j:pf4j:3.12.0")
}

val pluginClass = "com.gg.example.MainPlugin"
val pluginId = "com.gg.example.fixedshuffle"
val pluginName = "FixedShuffleQueue"
val pluginDescription = "Deterministically shuffle the visible playback queue in Salt Player for Windows"
val pluginVersion = "1.0.1"
val pluginProvider = "Augustu"
val pluginRepository = "https://github.com/Moriafly/spw-workshop-api/tree/main/example"
val workshopPluginsDir = file(System.getenv("APPDATA") + "/Salt Player for Windows/workshop/plugins/")
val pluginInstallDir = workshopPluginsDir.resolve("$pluginName-$pluginVersion")

fun jarContents() = zipTree(tasks.named<Jar>("jar").get().archiveFile.get())

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Plugin-Class" to pluginClass,
            "Plugin-Id" to pluginId,
            "Plugin-Name" to pluginName,
            "Plugin-Description" to pluginDescription,
            "Plugin-Version" to pluginVersion,
            "Plugin-Provider" to pluginProvider,
            "Plugin-Has-Config" to "true",
            "Plugin-Open-Source-Url" to pluginRepository,
        )
    }
}

tasks.register<Zip>("pluginZip") {
    destinationDirectory.set(workshopPluginsDir)
    archiveFileName.set("$pluginName-$pluginVersion.zip")
    archiveExtension.set("zip")

    dependsOn(tasks.named("jar"), configurations.runtimeClasspath)

    from({ jarContents() }) {
        into("classes")
    }
    from(configurations.runtimeClasspath) {
        into("lib")
        include("*.jar")
    }
}

tasks.register<Sync>("deployPlugin") {
    into(pluginInstallDir)
    dependsOn(tasks.named("jar"), configurations.runtimeClasspath)

    from({ jarContents() }) {
        into("classes")
    }
    from(configurations.runtimeClasspath) {
        into("lib")
        include("*.jar")
    }
}

tasks.register("plugin") {
    dependsOn("pluginZip", "deployPlugin")
}
