import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import java.util.Properties
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/*
 * CBX Installer: a tiny app that carries the CBX release APK, checks the phone first, installs
 * CBX through PackageInstaller and reports the exact failure to the admins. The system
 * installer only ever tells a student "there was a problem parsing the package".
 */
plugins {
    alias(libs.plugins.android.application)
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

fun signingValue(propertyName: String, environmentName: String): String =
    (localProperties.getProperty(propertyName) ?: System.getenv(environmentName) ?: "").trim()

// The installer always carries (and is versioned as) the CBX release it installs.
val appBuildScript = rootProject.file("app/build.gradle.kts").readText()
val cbxVersionCode = Regex("""versionCode\s*=\s*(\d+)""").find(appBuildScript)
    ?.groupValues?.get(1)?.toInt() ?: error("versionCode not found in app/build.gradle.kts")
val cbxVersionName = Regex("""versionName\s*=\s*"([^"]+)"""").find(appBuildScript)
    ?.groupValues?.get(1) ?: error("versionName not found in app/build.gradle.kts")
val cbxMinSdk = Regex("""minSdk\s*=\s*(\d+)""").find(appBuildScript)
    ?.groupValues?.get(1)?.toInt() ?: error("minSdk not found in app/build.gradle.kts")

val telegramBotToken = (localProperties.getProperty("telegram.bot.token") ?: "").trim()
val telegramBugChatId = (localProperties.getProperty("telegram.bug.chat.id") ?: "").trim()
// The official CBX signer. The bundled APK must match it before it is ever installed.
val officialCertSha256 = (localProperties.getProperty("signing.sha256.release") ?: "")
    .uppercase(Locale.US).filter { it in '0'..'9' || it in 'A'..'F' }
val releaseRemoteDiagnosticsEnabled = signingValue(
    "telegram.release.diagnostics.enabled",
    "CBX_RELEASE_REMOTE_DIAGNOSTICS_ENABLED"
).equals("true", ignoreCase = true)
val releaseKeystorePath = signingValue("release.keystore.path", "CBX_RELEASE_KEYSTORE")
val releaseKeystorePassword = signingValue("release.keystore.password", "CBX_RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("release.key.alias", "CBX_RELEASE_KEY_ALIAS")
val releaseKeyPassword = signingValue("release.key.password", "CBX_RELEASE_KEY_PASSWORD")
val releaseSigningConfigured = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isNotBlank() }
val stringObfuscationKey = 115

/** Copies the CBX release APK in as an asset, with its size and SHA-256 next to it. */
abstract class BundleCbxApkTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val cbxApk: RegularFileProperty

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val minSdk: Property<Int>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun bundle() {
        val output = outputDir.get().asFile
        output.deleteRecursively()
        output.mkdirs()
        val source = cbxApk.get().asFile
        val target = File(output, "cbx.apk")
        source.copyTo(target, overwrite = true)
        val digest = MessageDigest.getInstance("SHA-256")
        target.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        val sha256 = digest.digest().joinToString("") { String.format(Locale.US, "%02X", it) }
        // The CPU types CBX ships native code for, read from the APK itself.
        val abis = ZipFile(target).use { zip ->
            zip.entries().asSequence()
                .map { it.name.split('/') }
                .filter { it.size == 3 && it[0] == "lib" }
                .map { it[1] }
                .toSortedSet()
        }
        File(output, "cbx.properties").writeText(
            "size=${target.length()}\n" +
                "sha256=$sha256\n" +
                "versionCode=${versionCode.get()}\n" +
                "versionName=${versionName.get()}\n" +
                "minSdk=${minSdk.get()}\n" +
                "abis=${abis.joinToString(",")}\n"
        )
    }
}

val bundleCbxApk = tasks.register<BundleCbxApkTask>("bundleCbxApk") {
    dependsOn(":app:assembleRelease")
    cbxApk.set(rootProject.layout.projectDirectory.file("app/build/outputs/apk/release/app-release.apk"))
    versionCode.set(cbxVersionCode)
    versionName.set(cbxVersionName)
    minSdk.set(cbxMinSdk)
    outputDir.set(layout.buildDirectory.dir("generated/cbxApk"))
}

android {
    namespace = "com.coblax.examlock.installer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coblax.examlock.installer"
        // Far below CBX's own minimum on purpose: on a phone too old for CBX the installer
        // still opens and says so, instead of failing the same way CBX does.
        minSdk = 21
        targetSdk = 36
        versionCode = cbxVersionCode
        versionName = cbxVersionName
        buildConfigField("String", "CBX_OFFICIAL_CERT_SHA256", officialCertSha256.asBuildConfigString())
        buildConfigField("String", "TELEGRAM_BOT_TOKEN_OBF", "".asBuildConfigString())
        buildConfigField("String", "TELEGRAM_CHAT_ID_OBF", "".asBuildConfigString())
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = File(releaseKeystorePath).let {
                    if (it.isAbsolute) it else rootProject.file(releaseKeystorePath)
                }
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // v1 as well, so even old OEM installers and scanners can read it.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        // Debug builds never report: testing must not post into the admins' Telegram group.
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            if (releaseRemoteDiagnosticsEnabled) {
                buildConfigField(
                    "String",
                    "TELEGRAM_BOT_TOKEN_OBF",
                    telegramBotToken.xorBase64Obfuscate(stringObfuscationKey).asBuildConfigString()
                )
                buildConfigField(
                    "String",
                    "TELEGRAM_CHAT_ID_OBF",
                    telegramBugChatId.xorBase64Obfuscate(stringObfuscationKey).asBuildConfigString()
                )
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        // The bundled APK is already compressed; storing it keeps it streamable as is.
        noCompress += "apk"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(bundleCbxApk, BundleCbxApkTask::outputDir)
    }
}

dependencies {
    testImplementation(libs.junit)
}
