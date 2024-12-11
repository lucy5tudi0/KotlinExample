// [🍔] | 플러그인 버전 변경시 수정해야 할 파일
// [🍔] | build.gradle.kts | plugin.yml | KotlinExample.kt
// [🍔] | 및 https://plugin-api.itskimlot.kr 에서 플러그인 버전 정보를 업데이트해야 합니다.

import org.gradle.configurationcache.extensions.capitalized

plugins {
    kotlin("jvm") version "2.0.20"
}

group = "kr.itskimlot.kotlinexample"
version = "1.0"

val pluginName = rootProject.name.capitalized()
val testServerDir: String by project

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    testImplementation(kotlin("test"))

    compileOnly("io.papermc.paper:paper-api:1.17-R0.1-SNAPSHOT")
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
}

kotlin {
    jvmToolchain(17)
}

task("fatJar", type = Jar::class) {
    dependsOn(tasks.jar)
    archiveBaseName.set(pluginName)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

task("testJar") {
    val fatJar = tasks.getByName("fatJar") as Jar
    dependsOn(fatJar)

    if(!fatJar.archiveFile.get().asFile.exists()) return@task
    val destFile = File("$testServerDir/plugins/")
    doLast {
        copy {
            from(fatJar.archiveFile.get().asFile.absolutePath)
            into(destFile.absolutePath)
        }
    }
}