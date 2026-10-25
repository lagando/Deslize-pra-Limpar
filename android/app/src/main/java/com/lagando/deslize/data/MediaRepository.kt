package com.lagando.deslize.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.lagando.deslize.R
import com.lagando.deslize.model.MediaItem
import com.lagando.deslize.model.MediaStatus
import com.lagando.deslize.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório responsável pelo acesso e filtragem de mídias locais (MediaStore)
 * e sincronização com o banco de dados Room.
 */
class MediaRepository(
    private val context: Context,
    private val dao: MediaDao
) {

    /**
     * Busca todas as mídias da galeria que ainda não foram revisadas pelo usuário.
     * Permite filtrar opcionalmente por álbum/pasta (ex: "Camera", "WhatsApp Images").
     */
    suspend fun getUnreviewedMedia(bucketFilter: String? = null): List<MediaItem> = withContext(Dispatchers.IO) {
        val reviewedIds = dao.getReviewedIds().toHashSet()
        val allMedia = queryMediaStore()
        
        allMedia.filter { item ->
            item.id !in reviewedIds && (bucketFilter.isNullOrBlank() || item.bucketName.equals(bucketFilter, ignoreCase = true))
        }
    }

    /**
     * Retorna a lista de nomes únicos de pastas/álbuns encontrados na galeria.
     */
    suspend fun getAvailableBuckets(): List<String> = withContext(Dispatchers.IO) {
        val reviewedIds = dao.getReviewedIds().toHashSet()
        queryMediaStore()
            .filter { it.id !in reviewedIds && it.bucketName.isNotBlank() }
            .map { it.bucketName }
            .distinct()
            .sorted()
    }

    /**
     * Marca uma mídia como descartada (enviada para a lixeira interna do app).
     */
    suspend fun markAsTrashed(item: MediaItem) = withContext(Dispatchers.IO) {
        dao.insert(
            MediaItemEntity(
                mediaId = item.id,
                mediaUri = item.contentUri.toString(),
                status = MediaStatus.TRASHED,
                trashedDate = System.currentTimeMillis(),
                fileSize = item.size,
                mediaType = item.mediaType,
                displayName = item.displayName
            )
        )
    }

    /**
     * Marca uma mídia como mantida (favorita/guardada).
     */
    suspend fun markAsKept(item: MediaItem) = withContext(Dispatchers.IO) {
        dao.insert(
            MediaItemEntity(
                mediaId = item.id,
                mediaUri = item.contentUri.toString(),
                status = MediaStatus.KEPT,
                fileSize = item.size,
                mediaType = item.mediaType,
                displayName = item.displayName
            )
        )
    }

    /**
     * Desfaz a ação anterior removendo o registro do Room.
     */
    suspend fun deleteReviewedRecord(mediaId: Long) = withContext(Dispatchers.IO) {
        dao.delete(mediaId)
    }

    private fun queryMediaStore(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        // 1. Consulta Imagens
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    try {
                        val id = if (idCol != -1) cursor.getLong(idCol) else continue
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        val name = if (nameCol != -1) cursor.getString(nameCol) else null
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""

                        mediaList.add(
                            MediaItem(
                                id = id,
                                contentUri = uri,
                                displayName = name ?: context.getString(R.string.unknown_filename),
                                size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                                dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis() / 1000,
                                mediaType = MediaType.IMAGE,
                                bucketName = bucket
                            )
                        )
                    } catch (e: Exception) {
                        // Ignora arquivo corrompido
                    }
                }
            }
        } catch (e: Exception) {
            // Ignora erro de permissão ou query
        }

        // 2. Consulta Vídeos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )

        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)
                val bucketCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                while (cursor.moveToNext()) {
                    try {
                        val id = if (idCol != -1) cursor.getLong(idCol) else continue
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        val name = if (nameCol != -1) cursor.getString(nameCol) else null
                        val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""

                        mediaList.add(
                            MediaItem(
                                id = id,
                                contentUri = uri,
                                displayName = name ?: context.getString(R.string.unknown_filename),
                                size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                                dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis() / 1000,
                                mediaType = MediaType.VIDEO,
                                duration = if (durCol != -1) cursor.getLong(durCol) else 0L,
                                bucketName = bucket
                            )
                        )
                    } catch (e: Exception) {
                        // Ignora arquivo corrompido
                    }
                }
            }
        } catch (e: Exception) {
            // Ignora erro de permissão ou query
        }

        // Ordena pela data mais recente
        mediaList.sortByDescending { it.dateAdded }
        return mediaList
    }
}
