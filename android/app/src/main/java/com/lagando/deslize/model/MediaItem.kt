package com.lagando.deslize.model

import android.net.Uri

/**
 * Represents a media item from the device's MediaStore.
 * Used as an in-memory model for displaying photos and videos.
 */
data class MediaItem(
    val id: Long,                    // MediaStore _ID
    val contentUri: Uri,             // Content URI for loading
    val displayName: String,         // Original filename
    val size: Long,                  // File size in bytes
    val dateAdded: Long,             // Date added timestamp
    val mediaType: String,           // "image" or "video"
    val duration: Long = 0           // Duration in ms (for videos)
)
