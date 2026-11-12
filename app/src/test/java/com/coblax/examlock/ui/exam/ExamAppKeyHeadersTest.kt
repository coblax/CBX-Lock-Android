package com.coblax.examlock.ui.exam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamAppKeyHeadersTest {
    @Test
    fun blankKeySendsNoHeader() {
        assertNull(buildExamHostHeaders("https://cbt.sekolah.sch.id/", ""))
        assertNull(buildExamHostHeaders("https://cbt.sekolah.sch.id/", "   "))
    }

    @Test
    fun nonWebExamUrlSendsNoHeader() {
        assertNull(buildExamHostHeaders("ftp://cbt.sekolah.sch.id/", "223611"))
        assertNull(buildExamHostHeaders("not a url", "223611"))
    }

    @Test
    fun headerGoesOnlyToTheExamHost() {
        val headers = buildExamHostHeaders("https://CBT.sekolah.sch.id/login", "223611")!!

        assertEquals(mapOf("X-CBX-App-Key" to "223611"), headers.headers)
        assertTrue(headers.appliesTo("https://cbt.sekolah.sch.id/ujian/1"))
        assertTrue(headers.appliesTo("http://cbt.sekolah.sch.id/"))
        assertFalse(headers.appliesTo("https://docs.google.com/forms/x"))
        assertFalse(headers.appliesTo("https://evil.example/?h=cbt.sekolah.sch.id"))
        assertFalse(headers.appliesTo("javascript:alert(1)"))
        assertFalse(headers.appliesTo("about:blank"))
        assertFalse(headers.appliesTo(null))
    }

    @Test
    fun localServerWithPortKeepsItsOrigin() {
        val headers = buildExamHostHeaders("http://192.168.1.10:8080", "k")!!

        assertTrue(headers.appliesTo("http://192.168.1.10:8080/soal"))
        assertTrue("http://192.168.1.10:8080" in headers.originRules())
        assertTrue("https://192.168.1.10" in headers.originRules())
    }

    @Test
    fun existingHeaderIsRecognisedInAnyCase() {
        assertTrue(mapOf("x-cbx-app-key" to "1").hasHeader("X-CBX-App-Key"))
        assertFalse(mapOf("Accept" to "*/*").hasHeader("X-CBX-App-Key"))
        assertFalse((null as Map<String, String>?).hasHeader("X-CBX-App-Key"))
    }

    @Test
    fun scriptCarriesTheKeyAsAnEscapedLiteral() {
        val script = buildExamAppKeyScript(buildExamHostHeaders("https://cbt.test/", "a'b\\c</script>")!!)

        assertTrue(script.contains("'X-CBX-App-Key'"))
        assertTrue(script.contains("'a\\'b\\\\c\\x3c/script>'"))
        assertFalse(script.contains("</script>"))
        assertTrue(script.contains("XMLHttpRequest"))
        assertTrue(script.contains("window.fetch"))
    }
}
