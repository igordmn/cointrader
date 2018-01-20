group = "dmi"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        jcenter()
        flatDir {
            dirs(File(rootDir, "lib"))
        }
        maven("https://dl.bintray.com/kotlin/exposed")
    }
}