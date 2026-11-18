import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Explicit opt-in for local optimized shell validation; never suitable for publishing.
val unsignedRelease = providers.gradleProperty("unsignedRelease").orNull == "true"
val signingApplicationId = "com.templesoftware.frenchverbs"
val keystorePropertiesFile = rootProject.file("keystore.properties")
val releaseAggregateTasks = setOf("assemble", "build", "bundle")
val releaseSigningRequested = gradle.startParameter.taskNames.any { taskPath ->
    val taskName = taskPath.substringAfterLast(':')
    taskName.contains("release", ignoreCase = true) || taskName.lowercase() in releaseAggregateTasks
}
// Split sentinel deliberately remains unchanged when bootstrap replaces the product ID.
val isTemplateIdentity = signingApplicationId == "com.templesoftware." + "educationtemplate"
if (releaseSigningRequested && !unsignedRelease && isTemplateIdentity) {
    throw GradleException("This template is not publishable. Bootstrap a product before authorizing release signing.")
}
val keystoreProperties = Properties()

if (keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
} else if (releaseSigningRequested && !unsignedRelease) {
    throw GradleException(
        "Release signing requires an ignored root-level keystore.properties file. " +
            "Copy keystore.properties.example and provide the local upload-key details.",
    )
}

val requiredSigningProperties = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
if (releaseSigningRequested && !unsignedRelease) {
    if (keystoreProperties.getProperty("applicationId") != signingApplicationId) {
        throw GradleException(
            "Release signing requires explicit authorization for com.templesoftware.frenchverbs: " +
                "set applicationId in the ignored keystore.properties only after confirming key ownership. " +
                "For unsigned local validation use -PunsignedRelease=true.",
        )
    }
    val missingProperties = requiredSigningProperties.filter { keystoreProperties.getProperty(it).isNullOrBlank() }
    if (missingProperties.isNotEmpty()) {
        throw GradleException(
            "Release signing configuration is missing required properties: ${missingProperties.joinToString()}.",
        )
    }

    val configuredStoreFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
    if (!configuredStoreFile.isFile) {
        throw GradleException("The upload keystore configured by keystore.properties does not exist.")
    }
}

android {
    namespace = "com.templesoftware.frenchverbs"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.templesoftware.frenchverbs"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (!unsignedRelease && !isTemplateIdentity && keystoreProperties.getProperty("applicationId") == signingApplicationId) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
            }
        }
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
