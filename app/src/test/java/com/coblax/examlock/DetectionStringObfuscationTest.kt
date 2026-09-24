package com.coblax.examlock

import java.io.File
import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

/**
 * Root/hook detection inputs must not ship as plaintext: in a release DEX,
 * `strings classes.dex | grep xposed` used to return this detector's exact shopping
 * list, which is all anyone needs to locate the check and patch it out.
 *
 * Two directions are guarded here, because getting either wrong fails silently:
 *  - a bad encoding would fill the lists with garbage and disable detection entirely;
 *  - a new plaintext entry would quietly re-open the leak.
 *
 * The decoder is reimplemented independently so a regression in the production one
 * cannot make this test agree with it.
 */
class DetectionStringObfuscationTest {
    private val sourceRoot = listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }

    private val guardedFiles = listOf(
        "java/com/coblax/examlock/config/SecurityRules.kt",
        "java/com/coblax/examlock/IntegrityGuard.kt",
        "java/com/coblax/examlock/ReverseEngineeringGuard.kt",
        "java/com/coblax/examlock/runtime/SecurityDiagnosticsRuntime.kt"
    )

    /** Mirrors RuntimeStringDecoder's scheme: base64, then XOR with the assembled key. */
    private fun decode(value: String): String {
        val key = 0x12 xor 0x34 xor 0x41 xor 0x14
        val bytes = Base64.getDecoder().decode(value)
        for (index in bytes.indices) {
            bytes[index] = (bytes[index].toInt() xor key).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun readGuarded(): List<Pair<String, String>> =
        guardedFiles.map { relative ->
            val file = File(sourceRoot, relative)
            assertTrue("missing guarded source: $relative", file.isFile)
            relative to file.readText()
        }

    @Test
    fun everyObfuscatedConstantDecodesToTheValueItsCommentClaims() {
        // Horizontal whitespace only: a plain token followed by a comment on the NEXT
        // line is not an annotated constant, and treating it as one made this test
        // try to base64-decode ordinary source text.
        val annotated = Regex(
            "\"([A-Za-z0-9+/=]{8,})\",?[ \\t]*//[ \\t]*(\\S.*?)[ \\t]*$",
            RegexOption.MULTILINE
        )
        var decoded = 0
        var mirrored = 0
        for ((relative, source) in readGuarded()) {
            for (match in annotated.findAll(source)) {
                val encoded = match.groupValues[1]
                val comment = match.groupValues[2]
                val actual = runCatching { decode(encoded) }.getOrElse {
                    fail("$relative: $encoded is not decodable"); return
                }
                // Every constant must survive the round trip as usable text; garbage
                // here would disable detection without anything else noticing.
                assertTrue("$relative: $encoded decoded to blank", actual.isNotBlank())
                assertTrue(
                    "$relative: $encoded decoded to non-printable text",
                    actual.all { it.code in 0x20..0x7E }
                )
                decoded++
                // A comment shaped like a package name, class name or path is a
                // plaintext mirror of the constant; the rest are descriptive labels.
                if (comment.matches(Regex("[A-Za-z0-9_.$/-]+")) &&
                    ('.' in comment || '/' in comment)
                ) {
                    assertEquals("$relative: constant does not match its comment", comment, actual)
                    mirrored++
                }
            }
        }
        assertEquals("every obfuscated detection constant should decode", 107, decoded)
        assertTrue("expected many mirrored constants, found $mirrored", mirrored >= 60)
    }

    @Test
    fun noDetectionKeywordRemainsInAPlaintextStringLiteral() {
        val keywords = listOf(
            "magisk", "xposed", "lsposed", "lspatch", "edxposed", "frida",
            "substrate", "kernelsu", "apatch", "zygisk", "riru", "supersu",
            "superuser", "rootcloak", "hidemyroot", "objection", "lsplant", "shamiko"
        )
        val literal = Regex("\"([^\"\\\\]*)\"")
        val offenders = mutableListOf<String>()
        for ((relative, source) in readGuarded()) {
            source.lineSequence().forEachIndexed { index, rawLine ->
                // Comments deliberately keep the plaintext for maintenance; they are
                // stripped by the compiler and never reach the APK.
                val code = rawLine.substringBefore("//")
                for (match in literal.findAll(code)) {
                    val raw = match.groupValues[1]
                    // Detection inputs are package names, class names or paths, never
                    // prose. Admin-facing messages are allowed to name what they found.
                    if (raw.isBlank() || raw.any(Char::isWhitespace)) continue
                    val hit = keywords.firstOrNull { it in raw.lowercase() } ?: continue
                    offenders += "$relative:${index + 1} [$hit] $raw"
                }
            }
        }
        assertTrue(
            "detection inputs leaked as plaintext:\n" + offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }
}
