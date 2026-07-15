package com.coblax.examlock

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coblax.examlock.nativebridge.NativeBridgeBackendMode
import com.coblax.examlock.nativebridge.NativeBridgeTestControl
import com.coblax.examlock.nativebridge.NativeSecurityBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeRuntimeStringParityTest {
    @After
    fun tearDown() {
        NativeBridgeTestControl.resetBackendModeForTests()
    }

    @Test
    fun nativeLibraryIsAvailableForRuntimeStringParity() {
        assertTrue(NativeSecurityBridge.isNativeAvailableForTests())
    }

    @Test
    fun nativeDecoderMatchesKotlinFallbackAcrossRuntimeStringCorpus() {
        runtimeStringCorpus().forEachIndexed { index, obfuscated ->
            val nativeDecoded = RuntimeStringDecoderParityAccess.decodeWithBackend(
                obfuscated,
                NativeBridgeBackendMode.ForceNative
            )
            val kotlinDecoded = RuntimeStringDecoderParityAccess.decodeReference(obfuscated)

            assertEquals("Runtime string parity mismatch for case $index", kotlinDecoded, nativeDecoded)
        }
    }

    @Test
    fun gateResolverPermutationRemainsStableAcrossRepeatedReads() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val first = GateKeys.all.map { GateResolver.indexFor(context, it) }
        repeat(5) {
            val next = GateKeys.all.map { GateResolver.indexFor(context, it) }
            assertEquals(first, next)
        }
    }

    private fun runtimeStringCorpus(): List<String> {
        return listOf(
            SecureStrings.OBF_FAST_EXAM_URL,
            BuildConfig.TELEGRAM_BOT_TOKEN_OBF,
            BuildConfig.TELEGRAM_BUG_CHAT_ID_OBF,
            BuildConfig.SIGNING_SHA256_RELEASE_OBF,
            BuildConfig.SIGNING_SHA256_DEBUG_OBF,
            BuildConfig.MAPS_API_KEY_OBF,
            "HxwQGCwHHCwSAwMsFh0SER8WFw==",
            "IDAhNjY9LCM6PT06PTQsMjAnOiU2",
            "MQoDEgAAFhc=",
            "PxwQGFMHEgAYUwJHEgcWUx8cAAdTFwYBGh0UUxYLEh4="
        )
    }
}
