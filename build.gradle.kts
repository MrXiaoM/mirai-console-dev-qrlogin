plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0-M1"
}

group = "top.mrxiaom"
version = "0.1.1"

repositories {
    maven("https://repo.huaweicloud.com/repository/maven/")
    mavenCentral()
    maven("https://repo.mirai.mamoe.net/snapshots")
}
dependencies {
    compileOnly("net.mamoe.yamlkt:yamlkt:0.12.0")
}