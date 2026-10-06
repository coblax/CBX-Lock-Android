package com.coblax.examlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExamUrlValidationTest {
    @Test
    fun acceptsHttpsUrlWithPath() {
        val result = validateExamUrl("https://example.com/ujian")

        assertTrue(result.isValid)
        assertEquals("https://example.com/ujian", result.normalizedUrl)
    }

    /** A school's own http server: the exam opens, with a warning to the student. */
    @Test
    fun acceptsHttpExamUrl() {
        val result = validateExamUrl("http://192.168.1.100/ujian")

        assertTrue(result.isValid)
        assertEquals("http://192.168.1.100/ujian", result.normalizedUrl)
    }

    /** An APK fetched over http could be swapped on the way and installed as CBX Lock. */
    @Test
    fun downloadLinksStillNeedHttps() {
        val result = validateExamUrl("http://example.com/cbx.apk", allowCleartext = false)

        assertFalse(result.isValid)
        assertEquals(ExamUrlValidationError.Invalid, result.error)
        assertTrue(validateExamUrl("https://example.com/cbx.apk", allowCleartext = false).isValid)
    }

    @Test
    fun rejectsUrlWithoutScheme() {
        val result = validateExamUrl("example.com/ujian")

        assertFalse(result.isValid)
        assertEquals(ExamUrlValidationError.Invalid, result.error)
    }

    @Test
    fun rejectsBlankUrl() {
        val result = validateExamUrl("   ")

        assertFalse(result.isValid)
        assertEquals(ExamUrlValidationError.Blank, result.error)
    }

    @Test
    fun rejectsUnsupportedScheme() {
        val result = validateExamUrl("ftp://example.com/ujian")

        assertFalse(result.isValid)
        assertEquals(ExamUrlValidationError.Invalid, result.error)
    }
}
