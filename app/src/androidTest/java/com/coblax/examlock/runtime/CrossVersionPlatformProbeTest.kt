package com.coblax.examlock.runtime

import android.os.Build
import android.net.http.X509TrustManagerExtensions
import android.util.Log
import com.coblax.examlock.SecureStrings
import com.coblax.examlock.buildRootSecurityStatus
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.net.URL
import java.security.KeyStore
import java.security.cert.CertPathValidatorException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLException
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Reads the platform signals that change between Android releases from inside the app's
 * own SELinux domain, which is where the real detectors run. Run it on every emulator
 * image (Android 7, 9, 11, 16) to catch readings that only go wrong on some versions.
 */
@RunWith(AndroidJUnit4::class)
class CrossVersionPlatformProbeTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun selinuxReadingNeverReportsPermissiveOnAnEnforcingDevice() {
        val enabled = readSelinuxEnabled()
        val enforced = readSelinuxEnforced()
        Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} selinuxEnabled=$enabled selinuxEnforced=$enforced")
        // Every emulator image is enforcing. Apps are refused a read of the enforce flag
        // on newer releases, and that refusal must not read as "permissive".
        assertNotEquals(false, enforced)
    }

    @Test
    fun genuineInstallIsNotReportedAsRunningInsideAContainer() {
        val diagnostics = getVirtualEnvironmentDiagnostics(context, forceRefresh = true)
        Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} virtualIndicators=${diagnostics.indicators}")
        assertTrue(
            diagnostics.indicators.toString(),
            diagnostics.indicators.none { it.startsWith("virtual_container:") }
        )
    }

    @Test
    fun rootDetailsCollectWithoutThrowing() {
        val details = getRootDetectionDetails(context)
        val status = buildRootSecurityStatus(details)
        Log.i(
            TAG,
            "sdk=${Build.VERSION.SDK_INT} root=${status.detected} " +
                "indicator=${status.primaryIndicatorLabel} evidence=${status.evidenceSummary}"
        )
        assertEquals(details.selinuxEnforced == false && details.selinuxEnabled == true, status.selinuxPermissive)
    }

    @Test
    fun diagnosticsSummariesResolveOnThisRelease() {
        val summaries = listOf(
            "patch" to getSecurityPatchLevel(),
            "webview" to getCurrentWebViewPackageSummary(context),
            "memory" to getMemorySummary(context),
            "storage" to getStorageSummary(context),
            "locale" to getLocaleSummary(context),
            "installer" to getInstallSourceSummary(context)
        )
        summaries.forEach { (name, value) ->
            Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} $name=$value")
            assertTrue("$name is blank", value.isNotBlank())
        }
    }

    @Test
    fun appTrustStoreIncludesTheLetsEncryptRoots() {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        val trustManager = factory.trustManagers.filterIsInstance<X509TrustManager>().first()
        val subjects = trustManager.acceptedIssuers.map { it.subjectX500Principal.name }
        Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} trustAnchors=${subjects.size}")
        assertTrue(subjects.any { "CN=ISRG Root X1" in it })
        assertTrue(subjects.any { "CN=ISRG Root X2" in it })
    }

    @Test
    fun defaultExamServerCertificateIsTrusted() {
        // Goes through the app's network security config, as the exam WebView does.
        val url = URL(SecureStrings.fastExamUrl)
        val connection = url.openConnection() as HttpsURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.requestMethod = "HEAD"
        try {
            connection.connect()
            Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} tls=trusted host=${url.host} code=${connection.responseCode}")
            // Would the phone's own CA list have been enough without the bundled roots?
            val chain = connection.serverCertificates.filterIsInstance<X509Certificate>()
            val systemOnly = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                .apply { init(KeyStore.getInstance("AndroidCAStore").apply { load(null) }) }
                .trustManagers.filterIsInstance<X509TrustManager>().first()
            val systemVerdict = runCatching {
                X509TrustManagerExtensions(systemOnly)
                    .checkServerTrusted(chain.toTypedArray(), "ECDHE_ECDSA", url.host)
            }.fold({ "trusted" }, { "untrusted (${it.javaClass.simpleName})" })
            Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} systemStoreOnly=$systemVerdict")
            chain.forEachIndexed { index, cert ->
                Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} chain[$index] ${cert.subjectX500Principal.name} <- ${cert.issuerX500Principal.name} until ${cert.notAfter}")
            }
        } catch (error: SSLException) {
            val trustFailure = generateSequence(error as Throwable) { it.cause }
                .any { it is CertificateException || it is CertPathValidatorException }
            Log.i(TAG, "sdk=${Build.VERSION.SDK_INT} tls=${if (trustFailure) "untrusted" else "unsupported"} host=${url.host} error=$error")
            if (trustFailure) {
                throw AssertionError("Certificate of ${url.host} untrusted on API ${Build.VERSION.SDK_INT}", error)
            }
            // The platform TLS stack cannot negotiate with this server at all (Android
            // 7.0 and P-384 certificates); the WebView ships its own and is unaffected.
            assumeTrue("platform TLS cannot negotiate: $error", false)
        } catch (error: IOException) {
            assumeTrue("network unavailable: $error", false)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val TAG = "CBX_PROBE"
    }
}
