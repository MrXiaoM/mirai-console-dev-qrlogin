plugins {
    val kotlinVersion = "1.8.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.15.0-RC"
    id("com.github.gmazzo.buildconfig") version "3.1.0"
}

group = "top.mrxiaom"
version = "0.2.0"

buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.qrlogin")
    useKotlinOutput()

    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

repositories {
    maven("https://repo.huaweicloud.com/repository/maven/")
    mavenCentral()
    maven("https://repo.mirai.mamoe.net/snapshots")
}
dependencies {
    compileOnly("net.mamoe.yamlkt:yamlkt:0.12.0")
}