package com.lagando.deslize.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Data Access Object for media items.
 * Handles all database operations for tracking reviewed and trashed media.
 */
@Dao
interface MediaDao {

    /** Get all reviewed media IDs (both kept and trashed) to filter them out */
    @Query("SELECT mediaId FROM media_items")
    suspend fun getReviewedIds(): List<Long>

    /** Get all items currently in the trash */
    @Query("SELECT * FROM media_items WHERE status = 'trashed' ORDER BY trashedDate DESC")
    suspend fun getTrashedItems(): List<MediaItemEntity>

    /** Get trash items that have expired (older than cutoff date) */
    @Query("SELECT * FROM media_items WHERE status = 'trashed' AND trashedDate IS NOT NULL AND trashedDate < :cutoffDate")
    suspend fun getExpiredTrashItems(cutoffDate: Long): List<MediaItemEntity>

    /** Insert or update a media item */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity)

    /** Update the status of a media item */
    @Query("UPDATE media_items SET status = :status, trashedDate = :trashedDate WHERE mediaId = :mediaId")
    suspend fun updateStatus(mediaId: Long, status: String, trashedDate: Long?)

    /** Delete a media item record from the database */
    @Query("DELETE FROM media_items WHERE mediaId = :mediaId")
    suspend fun delete(mediaId: Long)

    /** Get total size of all trashed items in bytes */
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM media_items WHERE status = 'trashed'")
    suspend fun getTotalTrashSize(): Long

    /** Get count of trashed items */
    @Query("SELECT COUNT(*) FROM media_items WHERE status = 'trashed'")
    suspend fun getTrashCount(): Int

    /** Get all items marked as kept */
    @Query("SELECT * FROM media_items WHERE status = 'kept' ORDER BY mediaId DESC")
    suspend fun getKeptItems(): List<MediaItemEntity>
}
