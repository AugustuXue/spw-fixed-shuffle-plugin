import com.xuncorp.spw.workshop.gradle.PluginPermission
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.kotlin.kapt") version "2.4.20"
    id("com.xuncorp.spw.workshop") version "0.1.0-dev22"
}

group = "com.gg.example"
version = "1.1.1"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("com.github.Moriafly:spw-workshop-api:0.1.0-dev22")
    kapt("com.github.Moriafly:spw-workshop-api:0.1.0-dev22")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    compileOnly("org.pf4j:pf4j:3.12.0")
}

spmod {
    PluginClass = "com.gg.example.MainPlugin"
    PluginId = "com.gg.example.fixedshuffle"
    PluginName = "FixedShuffleQueue"
    PluginDescription = "Deterministically shuffle the visible playback queue in Salt Player for Windows"
    PluginVersion = project.version.toString()
    PluginProvider = "Augustu"
    PluginHasConfig = true
    PluginPermissions = emptyList<PluginPermission>()
    PluginOpenSourceUrl = "https://github.com/AugustuXue/spw-fixed-shuffle-plugin"
}

val workshopPluginsDir = file(System.getenv("APPDATA") + "/Salt Player for Windows/workshop/plugins/")
val pluginInstallDir = workshopPluginsDir.resolve("FixedShuffleQueue-${project.version}")

tasks.register<Sync>("deployPlugin") {
    dependsOn(tasks.named("plugin"))
    into(pluginInstallDir)
    from({ zipTree(tasks.named<Zip>("plugin").get().archiveFile.get()) })
}
