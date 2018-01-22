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


buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.0"

    repositories {
        mavenCentral()
        jcenter()
    }
}

plugins {
    application
    java
    kotlin("jvm") version "1.2.0"
}

val kotlinVersion: String by extra

fun RepositoryHandler.maven(uri: String) = maven { setUrl(uri) }

dependencies {
    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile(kotlin("reflect", kotlinVersion))
    compile(":jpy-0.9-SNAPSHOT:")
    compile("org.slf4j:slf4j-api:1.7.25")
    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("com.squareup.moshi:moshi:1.5.0")
    compile("com.squareup.moshi:moshi-kotlin:1.5.0")
    compile("com.squareup.retrofit2:retrofit:2.3.0")
    compile("com.squareup.retrofit2:converter-jackson:2.3.0")
    compile("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-experimental-adapter:1.0.0")
    compile("com.squareup.okhttp3:logging-interceptor:3.9.1")
    compile("commons-codec:commons-codec:1.10")
    compile("org.apache.commons:commons-lang3:3.6")
    compile("io.reactivex.rxjava2:rxkotlin:2.2.0")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-android:0.21")
    compile("org.xerial:sqlite-jdbc:3.21.0.1")
    compile("org.jetbrains.exposed:exposed:0.9.1")
    compile("com.github.kittinunf.fuel:fuel:1.12.0")
    testCompile("io.kotlintest:kotlintest:2.0.7")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    sourceSets["test"].java.srcDir(file("src/inttest/kotlin"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

kotlin {
    experimental.coroutines = Coroutines.ENABLE
}

application {
    mainClassName = "main.MainKt"
}


val startScripts = (tasks["startScripts"] as CreateStartScripts)
val original = startScripts.windowsStartScriptGenerator
startScripts.windowsStartScriptGenerator = object : ScriptGenerator {
    override fun generateScript(details: JavaAppStartScriptGenerationDetails, destination: Writer) {
        val header = "@if \"%DEBUG%\" == \"\" @echo off"
        val additional = "set PYTHONHOME=E:\\Distr\\Portable\\Dev\\Anaconda3\\envs\\coin_predict"

        val midStr = StringWriter()
        original.generateScript(details, midStr)
        val text = midStr.toString().replace(header, header + "\n" + additional)

        destination.write(text)
    }
}

fun mainTask(name: String) = task(name, JavaExec::class) {
    group = "application"
    val javaPlugin = the<JavaPluginConvention>()
    val applicationPlugin = the<ApplicationPluginConvention>()
    classpath = javaPlugin.sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).runtimeClasspath
    main = applicationPlugin.mainClassName
    jvmArgs = applicationPlugin.applicationDefaultJvmArgs.toList()
    environment = environment + mapOf("PYTHONHOME" to "E:\\Distr\\Portable\\Dev\\Anaconda3\\envs\\coin_predict")
    workingDir = rootDir
    args = listOf(name)
}

mainTask("backTest")
mainTask("forwardTest")
mainTask("realTrade")
mainTask("printTopCoins")