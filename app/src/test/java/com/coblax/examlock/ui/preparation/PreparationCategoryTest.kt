package com.coblax.examlock.ui.preparation

import com.coblax.examlock.model.UiLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreparationCategoryTest {
    @Test
    fun fixOrderCoversEveryCategoryOnceWithScreenLockLast() {
        assertEquals(
            PreparationCategory.entries.toSet(),
            PreparationCategoryFixOrder.toSet()
        )
        assertEquals(PreparationCategory.entries.size, PreparationCategoryFixOrder.size)
        // Screen pinning blocks the Settings screens every other fix opens.
        assertEquals(PreparationCategory.DeviceLock, PreparationCategoryFixOrder.last())
    }

    @Test
    fun categoryKeysAreUniqueForLazyListItems() {
        val keys = PreparationCategory.entries.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
    }

    @Test
    fun categoryTitlesStayShortEnoughForTheTileGrid() {
        UiLanguage.entries.forEach { language ->
            PreparationCategory.entries.forEach { category ->
                val title = category.title(language)
                assertTrue("$category/$language title '$title' is too long", title.length <= 14)
                assertTrue(category.description(language).isNotBlank())
            }
        }
    }

    @Test
    fun loadingChecklistTextKeepsSheetResponsiveBeforeDetailsAreReady() {
        val english = loadingPreparationChecklistText(UiLanguage.English)
        val indonesian = loadingPreparationChecklistText(UiLanguage.Indonesian)

        assertEquals("Loading details...", english.networkValue)
        assertEquals("Checking", english.networkStatusLabel)
        assertEquals("Memuat detail...", indonesian.networkValue)
        assertEquals("Mengecek", indonesian.networkStatusLabel)
    }
}
