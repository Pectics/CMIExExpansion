plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.gmazzo.buildconfig") version "4.1.2"
    id("com.gradleup.shadow") version "8.3.0"
}

val projectVersion: String by extra("1.0.1")

group = "me.pectics.papi.expansion"
version = projectVersion

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://repo.extendedclip.com/releases/") {
        name = "extentedclip-repo"
    }
    maven("https://jitpack.io") {
        name = "jitpack-repo"
    }
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.spigotmc:spigot-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.github.Zrips:CMI-API:9.7.14.3")
}

kotlin {
    jvmToolchain(21)
}

buildConfig {
    packageName("me.pectics.papi.expansion.cmiex")
    buildConfigField("String", "VERSION", "\"$projectVersion\"")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}