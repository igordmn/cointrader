group = "dmi"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        flatDir {
            dirs(File(rootProject.projectDir, "lib"))
        }
        maven("https://dl.bintray.com/kotlin/exposed")
    }
}