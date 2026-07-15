package com.coblax.examlock

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest
import java.util.Locale


data class SignatureIntegrityResult(
    val isMatch: Boolean,
    val actualFingerprint: String,
    val expectedFingerprints: List<String>,
    val reason: String
)

object PackageResolver {
    fun resolve(context: Context): PackageInfo? {
        val packageManager = context.packageManager
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }
        }.getOrNull()
    }
}

object CertificateReader {
    fun readPrimaryCertificate(packageInfo: PackageInfo): ByteArray? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return null
            val signatures = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
            signatures?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures?.firstOrNull()?.toByteArray()
        }
    }
}

object FingerprintGenerator {
    fun sha256Fingerprint(certificateBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(certificateBytes)
        return digest.joinToString(":") { byte -> "%02X".format(byte) }
    }
}

object IntegrityEvaluator {
    fun evaluate(actualFingerprint: String?, expectedFingerprints: List<String>): SignatureIntegrityResult {
        val normalizedExpected = expectedFingerprints
            .map(::normalizeFingerprint)
            .filter { it.isNotBlank() }
            .distinct()
        if (normalizedExpected.isEmpty()) {
            return SignatureIntegrityResult(
                isMatch = false,
                actualFingerprint = actualFingerprint.orEmpty().ifBlank { "-" },
                expectedFingerprints = expectedFingerprints,
                reason = "expected_fingerprint_missing"
            )
        }
        if (actualFingerprint.isNullOrBlank()) {
            return SignatureIntegrityResult(
                isMatch = false,
                actualFingerprint = "-",
                expectedFingerprints = expectedFingerprints,
                reason = "certificate_unavailable"
            )
        }
        val normalizedActual = normalizeFingerprint(actualFingerprint)
        val isMatch = normalizedExpected.any { it == normalizedActual }
        return SignatureIntegrityResult(
            isMatch = isMatch,
            actualFingerprint = actualFingerprint,
            expectedFingerprints = expectedFingerprints,
            reason = if (isMatch) "match" else "fingerprint_mismatch"
        )
    }
}

object ExamPolicyEngine {
    fun shouldBlock(result: SignatureIntegrityResult): Boolean = !result.isMatch
}

object SignatureIntegrity {
    fun check(context: Context, expectedFingerprints: List<String>): SignatureIntegrityResult {
        val packageInfo = PackageResolver.resolve(context)
        val certificateBytes = packageInfo?.let { CertificateReader.readPrimaryCertificate(it) }
        val actualFingerprint = certificateBytes?.let { FingerprintGenerator.sha256Fingerprint(it) }
        return IntegrityEvaluator.evaluate(actualFingerprint, expectedFingerprints)
    }
}

internal fun normalizeFingerprint(value: String): String {
    return value.replace(":", "")
        .replace(" ", "")
        .trim()
        .uppercase(Locale.US)
}

internal fun resolveExpectedSigningFingerprints(
    isDebugBuild: Boolean,
    releaseFingerprint: String,
    debugFingerprint: String
): List<String> {
    val release = releaseFingerprint.trim()
    val debug = debugFingerprint.trim()
    return if (isDebugBuild) {
        listOf(release, debug).filter { it.isNotBlank() }
    } else {
        listOf(release).filter { it.isNotBlank() }
    }
}
