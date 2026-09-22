package com.coblax.examlock

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceTextEncodingTest {
    @Test
    fun applicationSourcesContainValidUtf8WithoutMojibake() {
        val root = listOf(File("src/main"), File("app/src/main")).first { it.isDirectory }
        val suspicious = Regex("[\u00c2\u00c3][\u0080-\u00bf]|\u00e2[\u0080-\u00bf\u20ac\u201d\u2020\u0161]|\u00f0\u0178|\ufffd")
        val problems = mutableListOf<String>()
        root.walkTopDown().filter { it.extension in setOf("kt", "java", "xml", "js", "html") }
            .forEach { file ->
                val decoder = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                val source = decoder.decode(ByteBuffer.wrap(file.readBytes())).toString()
                source.lineSequence().forEachIndexed { index, line ->
                    if (suspicious.containsMatchIn(line)) {
                        problems += "${file.relativeTo(root)}:${index + 1}"
                    }
                }
            }
        assertTrue("Broken source text: ${problems.joinToString()}", problems.isEmpty())
    }
}
