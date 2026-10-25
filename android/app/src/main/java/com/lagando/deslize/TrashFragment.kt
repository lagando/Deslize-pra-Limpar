package com.lagando.deslize

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagando.deslize.adapter.TrashAdapter
import com.lagando.deslize.data.AppDatabase
import com.lagando.deslize.data.MediaItemEntity
import com.lagando.deslize.data.PrefsKeys
import com.lagando.deslize.model.MediaStatus
import com.lagando.deslize.util.buildMediaUri
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment for viewing and managing trashed media items.
 * Shows a list of items marked for deletion with options to restore or permanently delete.
 * Displays total space that can be freed and item count.
 */
class TrashFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TrashAdapter

    private lateinit var trashHeader: LinearLayout
    private lateinit var tvTrashCount: TextView
    private lateinit var tvTrashSize: TextView
    private lateinit var btnEmptyTrash: MaterialButton
    private lateinit var recyclerTrash: RecyclerView
    private lateinit var emptyTrashView: LinearLayout

    /**
     * Modern replacement for deprecated startIntentSenderForResult/onActivityResult.
     * Handles the result of MediaStore.createDeleteRequest() on Android 11+.
     */
    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // User confirmed deletion - remove items from our DB and track freed space
            lifecycleScope.launch(Dispatchers.IO) {
                val trashedItems = db.mediaDao().getTrashedItems()
                var totalFreed = 0L
                for (item in trashedItems) {
                    totalFreed += item.fileSize
                    db.mediaDao().delete(item.mediaId)
                }
                if (totalFreed > 0) {
                    val prefs = requireContext().getSharedPreferences(PrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    val currentFreed = prefs.getLong(PrefsKeys.TOTAL_SPACE_FREED, 0L)
                    prefs.edit { putLong(PrefsKeys.TOTAL_SPACE_FREED, currentFreed + totalFreed) }
                }
                withContext(Dispatchers.Main) {
                    loadTrashItems()
                    (activity as? MainActivity)?.updateTrashBadge()
                }
            }
        }
        // If user denied, items stay in trash
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        bindViews(view)
        setupRecyclerView()
        setupButtons()
        loadTrashItems()
    }

    private fun bindViews(view: View) {
        trashHeader = view.findViewById(R.id.trashHeader)
        tvTrashCount = view.findViewById(R.id.tvTrashCount)
        tvTrashSize = view.findViewById(R.id.tvTrashSize)
        btnEmptyTrash = view.findViewById(R.id.btnEmptyTrash)
        recyclerTrash = view.findViewById(R.id.recyclerTrash)
        emptyTrashView = view.findViewById(R.id.emptyTrashView)
    }

    private fun setupRecyclerView() {
        adapter = TrashAdapter(mutableListOf()) { item ->
            restoreItem(item)
        }
        recyclerTrash.layoutManager = LinearLayoutManager(requireContext())
        recyclerTrash.adapter = adapter
    }

    private fun setupButtons() {
        btnEmptyTrash.setOnClickListener {
            showEmptyTrashConfirmation()
        }
    }

    private fun loadTrashItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = db.mediaDao().getTrashedItems()
            val totalSize = db.mediaDao().getTotalTrashSize()
            val count = db.mediaDao().getTrashCount()

            withContext(Dispatchers.Main) {
                if (items.isEmpty()) {
                    trashHeader.visibility = View.GONE
                    recyclerTrash.visibility = View.GONE
                    emptyTrashView.visibility = View.VISIBLE
                } else {
                    trashHeader.visibility = View.VISIBLE
                    recyclerTrash.visibility = View.VISIBLE
                    emptyTrashView.visibility = View.GONE

                    tvTrashCount.text = resources.getQuantityString(R.plurals.trash_item_count, count, count)
                    tvTrashSize.text = getString(
                        R.string.trash_total_size,
                        Formatter.formatFileSize(requireContext(), totalSize)
                    )
                    adapter.updateItems(items)
                }
            }
        }
    }

    private fun restoreItem(item: MediaItemEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.mediaDao().updateStatus(item.mediaId, MediaStatus.KEPT, null)
            withContext(Dispatchers.Main) {
                loadTrashItems()
                (activity as? MainActivity)?.updateTrashBadge()
            }
        }
    }

    private fun showEmptyTrashConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_title)
            .setMessage(R.string.confirm_empty_trash)
            .setPositiveButton(R.string.btn_delete_permanent) { _, _ ->
                emptyTrash()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun emptyTrash() {
        lifecycleScope.launch(Dispatchers.IO) {
            val trashedItems = db.mediaDao().getTrashedItems()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+: Use createDeleteRequest for batch deletion
                val uris = trashedItems.map { item ->
                    buildMediaUri(item.mediaId, item.mediaType)
                }

                withContext(Dispatchers.Main) {
                    try {
                        val pendingIntent = MediaStore.createDeleteRequest(
                            requireContext().contentResolver, uris
                        )
                        val request = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        deleteRequestLauncher.launch(request)
                    } catch (e: Exception) {
                        // If createDeleteRequest fails, just remove from our DB
                        lifecycleScope.launch(Dispatchers.IO) {
                            for (item in trashedItems) {
                                db.mediaDao().delete(item.mediaId)
                            }
                            withContext(Dispatchers.Main) {
                                loadTrashItems()
                                (activity as? MainActivity)?.updateTrashBadge()
                            }
                        }
                    }
                }
            } else {
                // Android 10 and below: Delete directly
                var totalFreed = 0L
                for (item in trashedItems) {
                    try {
                        val uri = buildMediaUri(item.mediaId, item.mediaType)
                        requireContext().contentResolver.delete(uri, null, null)
                        totalFreed += item.fileSize
                    } catch (e: Exception) {
                        // File may already be deleted
                    }
                    db.mediaDao().delete(item.mediaId)
                }
                if (totalFreed > 0) {
                    val prefs = requireContext().getSharedPreferences(PrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    val currentFreed = prefs.getLong(PrefsKeys.TOTAL_SPACE_FREED, 0L)
                    prefs.edit { putLong(PrefsKeys.TOTAL_SPACE_FREED, currentFreed + totalFreed) }
                }
                withContext(Dispatchers.Main) {
                    loadTrashItems()
                    (activity as? MainActivity)?.updateTrashBadge()
                }
            }
        }
    }
}
