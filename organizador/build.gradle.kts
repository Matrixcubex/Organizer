// build.gradle.kts (nivel raíz)
buildscript {
    repositories {
        google()  // Asegúrate que está presente
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.4.2")  // Versión compatible
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}