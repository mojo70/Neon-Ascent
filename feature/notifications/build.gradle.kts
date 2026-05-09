plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.neon.ascent.feature.notifications"
    compileSdk = 35
    defaultConfig {
        minSdk = 31
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":feature:goals"))
    implementation(project(":feature:habits"))

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
