import androidx.room.gradle.RoomSchemaCopyTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    id("androidx.room")
}

android {
    namespace = "com.neon.ascent.core.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 31
    }
}

room {
    schemaDirectory(layout.projectDirectory.dir("schemas"))
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    ksp(libs.androidx.room.compiler)
    implementation(libs.gson)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}

// Workaround for Room Gradle Plugin Always-Run task warning
tasks.withType<RoomSchemaCopyTask>().configureEach {
    outputs.dir(layout.projectDirectory.dir("schemas")).withPropertyName("schemaDirectory")
}

