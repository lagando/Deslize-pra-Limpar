package com.lagando.deslize.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a media item (photo or video) that has been reviewed by the user.
 * Tracks whether the user chose to keep or trash the item.
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val mediaId: Long,            // MediaStore _ID
    val mediaUri: String,                      // Full content URI string
    val status: String,                        // "kept" or "trashed"
    val trashedDate: Long? = null,             // Timestamp when moved to trash
    val fileSize: Long = 0,                    // Size in bytes
    val mediaType: String = "image",           // "image" or "video"
    val displayName: String = "",              // Original filename
    val reviewedDate: Long = System.currentTimeMillis()
)
