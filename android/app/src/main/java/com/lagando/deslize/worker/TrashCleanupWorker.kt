package com.lagando.deslize.worker

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lagando.deslize.data.AppDatabase
import com.lagando.deslize.R

/**
 * Background worker that periodically cleans up expired trash items.
 * Items in the trash for more than 30 days are permanently deleted.
 * Runs every 24 hours via WorkManager.
 */
class TrashCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TrashCleanupWorker"
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getDatabase(applicationContext)
            val cutoffDate = System.currentTimeMillis() - THIRTY_DAYS_MS
            val expiredItems = db.mediaDao().getExpiredTrashItems(cutoffDate)

            if (expiredItems.isEmpty()) {
                Log.d(TAG, "No expired trash items to clean up")
                return Result.success()
            }

            Log.d(TAG, "Cleaning up ${expiredItems.size} expired trash items")
            var needsManualCleanup = false

            for (item in expiredItems) {
                try {
                    // Try to delete the actual file from device
                    val uri = if (item.mediaType == "video") {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, item.mediaId
                        )
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.mediaId
                        )
                    }

                    // On Android 10 and below, we can delete directly
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        applicationContext.contentResolver.delete(uri, null, null)
                        db.mediaDao().delete(item.mediaId)
                        val prefs = applicationContext.getSharedPreferences("deslize_prefs", Context.MODE_PRIVATE)
                        val currentFreed = prefs.getLong("total_space_freed", 0L)
                        prefs.edit { putLong("total_space_freed", currentFreed + item.fileSize) }
                        Log.d(TAG, "Cleaned up: ${item.displayName}")
                    } else {
                        // On Android 11+, background file deletion may fail silently or throw.
                        // We must NOT remove from our DB, so the user can delete it via the UI.
                        needsManualCleanup = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to clean up item ${item.mediaId}: ${e.message}")
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        db.mediaDao().delete(item.mediaId)
                    }
                }
            }

            if (needsManualCleanup) {
                showCleanupNotification(expiredItems.size)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Trash cleanup failed: ${e.message}")
            Result.retry()
        }
    }

    private fun showCleanupNotification(count: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "trash_cleanup",
                applicationContext.getString(R.string.notification_channel_name),
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = android.content.Intent(applicationContext, com.lagando.deslize.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            applicationContext, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, "trash_cleanup")
            .setSmallIcon(com.lagando.deslize.R.mipmap.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.resources.getQuantityString(R.plurals.notification_expired_text, count, count))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || 
            androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, notification)
        }
    }
}
