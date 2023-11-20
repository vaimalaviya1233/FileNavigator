plugins {
    alias(libs.plugins.filenavigator.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    id("kotlin-parcelize")
}

room {
    schemaDirectory("$projectDir/schemas/")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":domain"))

    implementation(libs.androidx.core)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidutils)
    implementation(libs.slimber)

    // Hilt
    implementation(libs.google.hilt)
    ksp(libs.google.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.simplestorage)

    // ---------------
    // Test

    testImplementation(project(":test"))
}