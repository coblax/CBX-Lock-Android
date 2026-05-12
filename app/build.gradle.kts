import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

fun String.xorBase64Obfuscate(key: Int): String {
    if (isEmpty()) return ""
    val bytes = toByteArray()
    for (index in bytes.indices) {
        bytes[index] = (bytes[index].toInt() xor key).toByte()
    }
    return Base64.getEncoder().encodeToString(bytes)
}

fun releaseSigningValue(propertyName: String, environmentName: String): String =
    (localProperties.getProperty(propertyName) ?: System.getenv(environmentName) ?: "").trim()

fun releaseKeystoreFile(path: String): File =
    File(path).let { candidate ->
        if (candidate.isAbsolute) candidate else rootProject.file(path)
    }

abstract class GenerateDexHashAssetTask : DefaultTask() {
    @get:InputDirectory
    abstract val dexRoot: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDir.get().asFile
        output.mkdirs()

        val resolvedDexFiles = dexRoot.get().asFile
            .walkTopDown()
            .filter { it.isFile && it.extension.equals("dex", true) }
            .sortedBy { it.path }
            .toList()

        require(resolvedDexFiles.isNotEmpty()) {
            "No dex files found under ${dexRoot.get().asFile}"
        }

        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8 * 1024)
        resolvedDexFiles.forEach { dexFile ->
            dexFile.inputStream().use { input ->
                var read = input.read(buffer)
                while (read > 0) {
                    digest.update(buffer, 0, read)
                    read = input.read(buffer)
                }
            }
        }

        val hash = digest.digest().joinToString("") { byte -> String.format(Locale.US, "%02X", byte) }
        File(output, "dex.sha256").writeText(hash)
    }
}


val telegramBotToken = (localProperties.getProperty("telegram.bot.token") ?: "").trim()
val telegramBugChatId = (localProperties.getProperty("telegram.bug.chat.id") ?: "").trim()
val signingSha256Release = (localProperties.getProperty("signing.sha256.release") ?: "").trim()
val signingSha256Debug = (localProperties.getProperty("signing.sha256.debug") ?: "").trim()
val mapsApiKey = (localProperties.getProperty("maps.api.key") ?: "").trim()
val stringObfuscationKey = 115
val releaseKeystorePath = releaseSigningValue("release.keystore.path", "CBX_RELEASE_KEYSTORE")
val releaseKeystorePassword = releaseSigningValue(
    "release.keystore.password",
    "CBX_RELEASE_KEYSTORE_PASSWORD"
)
val releaseKeyAlias = releaseSigningValue("release.key.alias", "CBX_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("release.key.password", "CBX_RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isNotBlank() }
val telegramBotTokenObf = telegramBotToken.xorBase64Obfuscate(stringObfuscationKey)
val telegramBugChatIdObf = telegramBugChatId.xorBase64Obfuscate(stringObfuscationKey)
val signingSha256ReleaseObf = signingSha256Release.xorBase64Obfuscate(stringObfuscationKey)
val signingSha256DebugObf = signingSha256Debug.xorBase64Obfuscate(stringObfuscationKey)
val mapsApiKeyObf = mapsApiKey.xorBase64Obfuscate(stringObfuscationKey)

android {
    namespace = "com.example.coblaxexamlock"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.coblaxexamlock"
        minSdk = 24
        targetSdk = 36
        versionCode = 308
        versionName = "3.0.8"
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
            }
        }
        buildConfigField("String", "TELEGRAM_BOT_TOKEN_OBF", telegramBotTokenObf.asBuildConfigString())
        buildConfigField("String", "TELEGRAM_BUG_CHAT_ID_OBF", telegramBugChatIdObf.asBuildConfigString())
        buildConfigField("String", "MAPS_API_KEY_OBF", mapsApiKeyObf.asBuildConfigString())
        buildConfigField(
            "String",
            "SIGNING_SHA256_RELEASE_OBF",
            signingSha256ReleaseObf.asBuildConfigString()
        )
        buildConfigField(
            "String",
            "SIGNING_SHA256_DEBUG_OBF",
            signingSha256DebugObf.asBuildConfigString()
        )
        manifestPlaceholders["mapsApiKey"] = mapsApiKey

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = releaseKeystoreFile(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-g")
                }
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-O3", "-fvisibility=hidden")
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("lowRamQa") {
            initWith(getByName("release"))
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isProfileable = true
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            externalNativeBuild {
                cmake {
                    cppFlags += listOf("-O3", "-fvisibility=hidden")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
    lint {
        lintConfig = file("lint.xml")
    }
}

composeCompiler {
    includeSourceInformation = false
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.webkit)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.security.crypto)
    implementation(libs.zxing.android.embedded)
    implementation(libs.zxing.core)
    implementation(libs.play.services.maps)
    implementation(libs.google.places)
    implementation(libs.google.material)
    implementation(libs.androidx.profileinstaller)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    baselineProfile(project(":baselineprofile"))
}

baselineProfile {
    mergeIntoMain = true
    automaticGenerationDuringBuild = false
}

androidComponents {
    onVariants { variant ->
        if (variant.buildType !in setOf("release", "lowRamQa")) {
            return@onVariants
        }
        val variantName = variant.name
        val capitalized = variantName.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.US) else char.toString()
        }
        val dexRootProvider = layout.buildDirectory.dir("intermediates/dex/$variantName")
        val outputDirProvider = layout.buildDirectory.dir("generated/dexhash/$variantName/assets")

        val taskProvider =
            tasks.register<GenerateDexHashAssetTask>("generateDexHashAsset$capitalized") {
                dexRoot.set(dexRootProvider)
                outputDir.set(outputDirProvider)
                dependsOn(tasks.matching { it.name == "minify${capitalized}WithR8" })
            }

        variant.sources.assets?.addGeneratedSourceDirectory(
            taskProvider,
            GenerateDexHashAssetTask::outputDir
        )
    }
}

gradle.taskGraph.whenReady {
    val releasePackageRequested = allTasks.any { task ->
        val name = task.name
        name.contains("Release") &&
            (name.startsWith("assemble") ||
                name.startsWith("bundle") ||
                name.startsWith("package"))
    }
    if (releasePackageRequested && !releaseSigningConfigured) {
        throw GradleException(
            "Release signing is not configured. Add release.keystore.path, " +
                "release.keystore.password, release.key.alias, and release.key.password " +
                "to local.properties, or set CBX_RELEASE_KEYSTORE, " +
                "CBX_RELEASE_KEYSTORE_PASSWORD, CBX_RELEASE_KEY_ALIAS, and " +
                "CBX_RELEASE_KEY_PASSWORD environment variables."
        )
    }
}
