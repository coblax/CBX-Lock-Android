package com.example.coblaxexamlock

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

    @Test
    fun rejectsHttpUrlBecauseCleartextIsBlocked() {
        val result = validateExamUrl("http://example.com/ujian")

        assertFalse(result.isValid)
        assertEquals(ExamUrlValidationError.Invalid, result.error)
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
