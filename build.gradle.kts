plugins {
    // Kotlin version
    kotlin("multiplatform").version("1.9.22").apply(false)
    kotlin("android").version("1.9.22").apply(false)

    // Android
    id("com.android.application").version("8.1.4").apply(false)
    id("com.android.library").version("8.1.4").apply(false)

    // Compose
    id("org.jetbrains.compose").version("1.6.0").apply(false)

    // Kotlin serialization
    kotlin("plugin.serialization").version("1.9.22").apply(false)

    // Google services for Firebase
    id("com.google.gms.google-services").version("4.4.0").apply(false)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
