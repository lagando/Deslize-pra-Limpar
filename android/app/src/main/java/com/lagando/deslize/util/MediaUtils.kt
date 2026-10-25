package com.lagando.deslize.util

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import com.lagando.deslize.model.MediaType

/**
 * Retorna a URI de conteúdo correspondente para uma imagem ou vídeo no MediaStore.
 */
fun buildMediaUri(mediaId: Long, mediaType: String): Uri {
    val collection = if (mediaType == MediaType.VIDEO) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    return ContentUris.withAppendedId(collection, mediaId)
}
