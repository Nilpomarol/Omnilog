plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

fun providerCredential(name: String): String {
    val value = providers.gradleProperty(name).orNull
        ?.takeIf { it.isNotBlank() }
        ?: providers.environmentVariable(name).orNull.orEmpty()
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

val tmdbApiKey = providerCredential("TMDB_API_KEY")
val googleBooksApiKey = providerCredential("GOOGLE_BOOKS_API_KEY")
val rawgApiKey = providerCredential("RAWG_API_KEY")
val igdbClientId = providerCredential("IGDB_CLIENT_ID")
val igdbClientSecret = providerCredential("IGDB_CLIENT_SECRET")
val omdbApiKey = providerCredential("OMDB_API_KEY")
val malClientId = providerCredential("MAL_CLIENT_ID")
val releaseKeystorePath = providers.environmentVariable("OMNILOG_RELEASE_KEYSTORE").orNull
val releaseStorePassword = providers.environmentVariable("OMNILOG_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("OMNILOG_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("OMNILOG_RELEASE_KEY_PASSWORD").orNull

android {
    namespace = "com.nilpo.contenttracker"
    compileSdk = 36

    val localReleaseSigning = if (
        releaseKeystorePath != null && releaseStorePassword != null &&
        releaseKeyAlias != null && releaseKeyPassword != null
    ) {
        signingConfigs.create("localRelease") {
            storeFile = file(releaseKeystorePath)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
            storeType = "PKCS12"
        }
    } else {
        null
    }

    defaultConfig {
        applicationId = "com.nilpo.contenttracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbApiKey\"")
        buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")
        buildConfigField("String", "RAWG_API_KEY", "\"$rawgApiKey\"")
        buildConfigField("String", "IGDB_CLIENT_ID", "\"$igdbClientId\"")
        buildConfigField("String", "IGDB_CLIENT_SECRET", "\"$igdbClientSecret\"")
        buildConfigField("String", "OMDB_API_KEY", "\"$omdbApiKey\"")
        buildConfigField("String", "MAL_CLIENT_ID", "\"$malClientId\"")
        buildConfigField("String", "MAL_REDIRECT_URI", "\"omnilog://mal-oauth\"")
    }

    buildTypes {
        getByName("debug") {
            // Keep development installs separate from the release-signed app already on a device.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            // Supplied only for the lifetime of tools/update-release.ps1. Secrets never touch disk.
            localReleaseSigning?.let { signingConfig = it }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation("io.coil-kt.coil3:coil-compose:3.2.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.2.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.compose.ui:ui:1.11.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.11.2")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.navigation3:navigation3-runtime:1.1.4")
    implementation("androidx.navigation3:navigation3-ui:1.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.11.2")
    ksp("androidx.room:room-compiler:2.8.4")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.11.2")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
