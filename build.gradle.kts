import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.android.AndroidGradleWrapper.srcDir

group = "dmi"
version = "1.0-SNAPSHOT"

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


repositories {
    mavenCentral()
    jcenter()
    flatDir {
        dirs("lib")
    }
    maven("https://dl.bintray.com/kotlin/exposed")
}

dependencies {
    compile(kotlin("stdlib-jdk8", kotlinVersion))
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

//
//project.extensions.create("greeting", CreateStartScripts::class.java)
//
configure<org.gradle.jvm.application.tasks.CreateStartScripts> {
//    optsEnvironmentVar="PYTHONHOME=E:\\Distr\\Portable\\Dev\\Anaconda3\\envs\\coin_predict"
}