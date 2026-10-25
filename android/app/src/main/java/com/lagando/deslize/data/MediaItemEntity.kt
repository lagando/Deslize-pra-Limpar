package com.lagando.deslize.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lagando.deslize.model.MediaType

/**
 * Represents a media item (photo or video) that has been reviewed by the user.
 * Tracks whether the user chose to keep or trash the item.
 */
@Entity(tableName = "media_items")
data class MediaItemEntity(
    @PrimaryKey val mediaId: Long,            // MediaStore _ID
    val mediaUri: String,                      // Full content URI string
    val status: String,                        // "kept" or "trashed" (see MediaStatus)
    val trashedDate: Long? = null,             // Timestamp when moved to trash
    val fileSize: Long = 0,                    // Size in bytes
    val mediaType: String = MediaType.IMAGE,   // "image" or "video" (see MediaType)
    val displayName: String = "",              // Original filename
    val reviewedDate: Long = System.currentTimeMillis()
)
