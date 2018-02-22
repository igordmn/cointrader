import java.net.URI

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
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://kotlin.bintray.com/kotlinx")
        maven("http://objectbox.net/beta-repo")
    }
}