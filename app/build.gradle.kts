import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper.srcDir
import java.io.Writer
import java.io.StringWriter
import org.gradle.api.tasks.SourceSet
import org.gradle.api.plugins.JavaPluginConvention
import com.sun.javafx.scene.CameraHelper.project
import org.codehaus.groovy.vmplugin.VMPluginFactory.getPlugin
import org.gradle.internal.deployment.RunApplication
import org.gradle.internal.impldep.aQute.bnd.build.Run
import java.net.URI


buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.31"
    var serializationVersion: String by extra
    serializationVersion = "0.4.2"

    repositories {
        mavenCentral()
        jcenter()
        maven("https://kotlin.bintray.com/kotlinx")
        maven("http://objectbox.net/beta-repo")
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serializationVersion")
    }
}

plugins {
    application
    java
    kotlin("jvm") version "1.2.31"
}

apply {
    plugin("kotlinx-serialization")
//    plugin("kotlin-kapt")  see https://github.com/Kotlin/kotlinx.serialization/issues/76
}

val kotlinVersion: String by extra
val serializationVersion: String by extra

dependencies {
    compile(kotlin("stdlib", kotlinVersion))
    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))
    compile(":jep-3.7.1:")
    compile("org.slf4j:slf4j-api:1.7.25")
    compile("org.apache.logging.log4j:log4j-api:2.11.0")
    compile("org.apache.logging.log4j:log4j-core:2.11.0")
    compile("org.apache.logging.log4j:log4j-slf4j-impl:2.11.0")
    compile("com.squareup.moshi:moshi:1.5.0")
    compile("com.squareup.moshi:moshi-kotlin:1.5.0")
    compile("com.squareup.retrofit2:retrofit:2.3.0")
    compile("com.squareup.retrofit2:converter-jackson:2.3.0")
    compile("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-experimental-adapter:1.0.0")
    compile("com.squareup.okhttp3:logging-interceptor:3.9.1")
    compile("commons-codec:commons-codec:1.10")
    compile("org.apache.commons:commons-lang3:3.6")
    compile("io.reactivex.rxjava2:rxkotlin:2.2.0")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:0.22.5")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-nio:0.22.5")
    compile("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.0")
    compile("com.fasterxml.jackson.core:jackson-core:2.9.0")
    compile("com.fasterxml.jackson.core:jackson-annotations:2.9.0")
    compile("com.fasterxml.jackson.core:jackson-databind:2.9.0")
    compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.0")
    compile("org.xerial:sqlite-jdbc:3.21.0.1")
    compile("com.google.guava:guava:24.0-jre")
    compile("org.jetbrains.exposed:exposed:0.9.1")
    compile("com.github.kittinunf.fuel:fuel:1.12.0")
//    compile("org.nd4j:nd4j-cuda-8.0-platform:0.9.1")
//    compile("org.nd4j:nd4j-cuda-8.0-platform:0.9.1:windows-x86_64")
//    compile("org.deeplearning4j:deeplearning4j-core:0.9.1")
//    compile("org.deeplearning4j:deeplearning4j-cuda-8.0:0.9.1")
//    compile("org.deeplearning4j:rl4j-core:0.9.1")
//    compile("org.nd4j:nd4j-cuda-8.0:0.9.1")
//    compile("org.nd4j:nd4j-cuda-8.0:0.9.1:windows-x86_64")
//    compile("org.nd4j:nd4j-native:0.9.1")
//    compile("org.nd4j:nd4j-native:0.9.1:windows-x86_64")
    compile("com.sleepycat:je:5.0.73")
    compile("org.deephacks.lmdbjni:lmdbjni:0.4.6")
    compile("org.deephacks.lmdbjni:lmdbjni-win64:0.4.6")
    compile("org.lmdbjava:lmdbjava:0.6.0")
    compile("org.fusesource.leveldbjni:leveldbjni-all:1.8")
    compile("org.jetbrains.xodus:xodus-openAPI:1.2.0")
    compile("org.jetbrains.xodus:xodus-environment:1.2.0")
    compile("org.rocksdb:rocksdbjni:5.5.1")
    compile("org.apache.commons:commons-math3:3.6.1")
    compile("io.arrow-kt:arrow-typeclasses:0.6.1")
    compile("io.arrow-kt:arrow-data:0.6.1")
    compile("io.arrow-kt:arrow-instances:0.6.1")
    compile("io.arrow-kt:arrow-syntax:0.6.1")
    compile("org.knowm.xchart:xchart:3.5.1")
    compile("org.apache.commons:commons-lang3:3.7")
    compile("commons-io:commons-io:2.6")
//    kapt("io.arrow-kt:arrow-annotations-processor:0.6.1")
    testCompile("io.kotlintest:kotlintest:2.0.7")
    testCompile("com.google.jimfs:jimfs:1.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

val libraryPath = "D:/Development/Projects/cointrader/src/lib/native/cp36-win_amd64"
val pythonHome = "E:/Distr/Portable/Dev/Anaconda3/envs/coin_predict"

application {
    mainClassName = "com.dmi.cointrader.MainKt"
    applicationDefaultJvmArgs = listOf("-Djava.library.path=$libraryPath")
}

val startScripts = (tasks["startScripts"] as CreateStartScripts)
val original = startScripts.windowsStartScriptGenerator
startScripts.windowsStartScriptGenerator = object : ScriptGenerator {
    override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
        val header = "@if \"%DEBUG%\" == \"\" @echo off"
        val classPathOld = Regex("set CLASSPATH=.*\r\n")
        val classPathNew = "set CLASSPATH=%APP_HOME%/lib/*\r\n"
        val additional = "set PYTHONHOME=$pythonHome"

        val midStr = StringWriter()
        original.generateScript(details, midStr)
        val text = midStr.toString()
                .replace(header, header + "\n" + additional)
                .replace(classPathOld, classPathNew)

        destination.write(text)
    }
}

configure<ApplicationPluginConvention> {
    applicationDistribution
            .from("$rootDir")
            .include("data/**")
            .exclude("data/cache/**")
            .exclude("data/log/**")
            .include("python/**")
            .include("lib/native/**")
            .into("bin")
}

tasks.withType(JavaExec::class.java) {
    systemProperty("java.library.path", libraryPath)
    environment("PYTHONHOME", pythonHome)
    workingDir(rootDir)
}

tasks.withType(Test::class.java) {
    systemProperty("java.library.path", libraryPath)
    environment("PYTHONHOME", pythonHome)
    workingDir(rootDir)
}

fun mainTask(name: String, vararg args: String = arrayOf(name)) = task(name, JavaExec::class) {
    group = "application"
    val javaPlugin = the<JavaPluginConvention>()
    val applicationPlugin = the<ApplicationPluginConvention>()
    classpath = javaPlugin.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath
    main = applicationPlugin.mainClassName
    jvmArgs = applicationPlugin.applicationDefaultJvmArgs.toList() +
            listOf("-Djava.library.path=$libraryPath")

    environment = environment + mapOf("PYTHONHOME" to pythonHome)
    workingDir = rootDir
    standardInput = System.`in`
    this.args = args.toList()
}

mainTask("archive")
mainTask("train")
mainTask("trainMulti")
mainTask("backtest10", "backtest", 10.toString())
mainTask("realTrades")
mainTask("topcoins")
mainTask("bestnets20", "bestnets", 20.toString())