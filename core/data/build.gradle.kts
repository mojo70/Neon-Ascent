plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.neon.ascent.core.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 31
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.gson)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
