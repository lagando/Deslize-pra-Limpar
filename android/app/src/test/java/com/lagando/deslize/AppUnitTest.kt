package com.lagando.deslize

import com.lagando.deslize.data.PrefsKeys
import com.lagando.deslize.model.MediaItem
import com.lagando.deslize.model.MediaStatus
import com.lagando.deslize.model.MediaType
import com.lagando.deslize.model.UndoAction
import com.lagando.deslize.worker.TrashCleanupWorker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Testes unitários para validar contratos de constantes, lógica de modelos
 * e cálculo temporal do Worker de limpeza.
 */
class AppUnitTest {

    @Test
    fun testMediaStatusConstants() {
        assertEquals("trashed", MediaStatus.TRASHED)
        assertEquals("kept", MediaStatus.KEPT)
        assertNotEquals(MediaStatus.TRASHED, MediaStatus.KEPT)
    }

    @Test
    fun testMediaTypeConstants() {
        assertEquals("image", MediaType.IMAGE)
        assertEquals("video", MediaType.VIDEO)
    }

    @Test
    fun testPrefsKeysConstants() {
        assertEquals("deslize_prefs", PrefsKeys.PREFS_NAME)
        assertEquals("tutorial_shown", PrefsKeys.TUTORIAL_SHOWN)
        assertEquals("permission_requested", PrefsKeys.PERMISSION_REQUESTED)
        assertEquals("total_space_freed", PrefsKeys.TOTAL_SPACE_FREED)
    }

    @Test
    fun testTrashCleanupWorkerExpirationCalculation() {
        val thirtyDaysMs = TrashCleanupWorker.THIRTY_DAYS_MS
        val expectedThirtyDays = TimeUnit.DAYS.toMillis(30)
        assertEquals(expectedThirtyDays, thirtyDaysMs)

        val now = 1700000000000L
        val cutoff = now - thirtyDaysMs
        assertTrue(now > cutoff)
        assertEquals(30L, TimeUnit.MILLISECONDS.toDays(now - cutoff))
    }

    @Test
    fun testUndoActionModel() {
        val uri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val item = MediaItem(
            id = 123L,
            contentUri = uri,
            displayName = "photo.jpg",
            size = 2048L,
            dateAdded = 1600000000L,
            mediaType = MediaType.IMAGE,
            bucketName = "Camera"
        )
        val undo = UndoAction(item, MediaStatus.TRASHED)

        assertEquals(123L, undo.item.id)
        assertEquals(MediaStatus.TRASHED, undo.status)
        assertEquals("Camera", undo.item.bucketName)
    }

    @Test
    fun testSortBySizeOrdering() {
        val uri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val small = MediaItem(1L, uri, "small.jpg", 100L, 1000L, MediaType.IMAGE)
        val medium = MediaItem(2L, uri, "med.jpg", 500L, 2000L, MediaType.IMAGE)
        val large = MediaItem(3L, uri, "large.jpg", 9999L, 1500L, MediaType.IMAGE)

        val list = mutableListOf(small, medium, large)
        list.sortByDescending { it.size }

        assertEquals(large.id, list[0].id)
        assertEquals(medium.id, list[1].id)
        assertEquals(small.id, list[2].id)
    }

    @Test
    fun testSortByDateOrdering() {
        val uri = org.mockito.Mockito.mock(android.net.Uri::class.java)
        val old = MediaItem(1L, uri, "old.jpg", 9999L, 1000L, MediaType.IMAGE)
        val mid = MediaItem(2L, uri, "mid.jpg", 500L, 2000L, MediaType.IMAGE)
        val recent = MediaItem(3L, uri, "recent.jpg", 100L, 3000L, MediaType.IMAGE)

        val list = mutableListOf(old, mid, recent)
        list.sortByDescending { it.dateAdded }

        assertEquals(recent.id, list[0].id)
        assertEquals(mid.id, list[1].id)
        assertEquals(old.id, list[2].id)
    }
}
