plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

group = "top.mrxiaom"
version = "0.1.0"

repositories {
    mavenCentral()
}
dependencies {
    compileOnly(fileTree("libs"))
}
tasks {
    jar {
        archiveExtension.set("mirai2.jar")
    }
}