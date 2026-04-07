import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.kapt") version "2.3.0"
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
}

group = "com.gg.example"
version = "1.0.0-fixed-shuffle"

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
    kapt(files("libs/api-0.1.0-dev20.jar"))
    compileOnly("org.pf4j:pf4j:3.12.0")
    kapt("org.pf4j:pf4j:3.12.0")
}

val pluginClass = "com.gg.example.MainPlugin"
val pluginId = "com.gg.example.fixedshuffle"
val pluginName = "FixedShuffleQueue"
val pluginDescription = "Deterministically shuffle the visible playback queue in Salt Player for Windows"       
val pluginVersion = "1.0.0-fixed-shuffle"
val pluginProvider = "Augustu + Claude"
val pluginRepository = "https://github.com/Moriafly/spw-workshop-api/tree/main/example"

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



