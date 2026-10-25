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
 * Fragment para gerenciar itens na lixeira.
 * Permite restaurar individualmente, esvaziar tudo ou fazer seleção múltipla
 * para restaurar ou excluir permanentemente itens selecionados.
 */
class TrashFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TrashAdapter

    // Selection Bar
    private lateinit var trashSelectionBar: LinearLayout
    private lateinit var btnCancelTrashSelection: MaterialButton
    private lateinit var tvTrashSelectionCount: TextView
    private lateinit var btnRestoreSelected: MaterialButton
    private lateinit var btnDeleteSelected: MaterialButton

    // Header & List
    private lateinit var trashHeader: LinearLayout
    private lateinit var tvTrashCount: TextView
    private lateinit var tvTrashSize: TextView
    private lateinit var btnEmptyTrash: MaterialButton
    private lateinit var recyclerTrash: RecyclerView
    private lateinit var emptyTrashView: LinearLayout

    private var pendingDeleteItems: List<MediaItemEntity>? = null

    private val deleteRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch(Dispatchers.IO) {
                val itemsToRemove = pendingDeleteItems ?: db.mediaDao().getTrashedItems()
                var totalFreed = 0L
                for (item in itemsToRemove) {
                    totalFreed += item.fileSize
                    db.mediaDao().delete(item.mediaId)
                }
                pendingDeleteItems = null

                if (totalFreed > 0) {
                    val prefs = requireContext().getSharedPreferences(PrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    val currentFreed = prefs.getLong(PrefsKeys.TOTAL_SPACE_FREED, 0L)
                    prefs.edit { putLong(PrefsKeys.TOTAL_SPACE_FREED, currentFreed + totalFreed) }
                }
                withContext(Dispatchers.Main) {
                    adapter.clearSelection()
                    loadTrashItems()
                    (activity as? MainActivity)?.updateTrashBadge()
                }
            }
        } else {
            pendingDeleteItems = null
        }
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
        trashSelectionBar = view.findViewById(R.id.trashSelectionBar)
        btnCancelTrashSelection = view.findViewById(R.id.btnCancelTrashSelection)
        tvTrashSelectionCount = view.findViewById(R.id.tvTrashSelectionCount)
        btnRestoreSelected = view.findViewById(R.id.btnRestoreSelected)
        btnDeleteSelected = view.findViewById(R.id.btnDeleteSelected)

        trashHeader = view.findViewById(R.id.trashHeader)
        tvTrashCount = view.findViewById(R.id.tvTrashCount)
        tvTrashSize = view.findViewById(R.id.tvTrashSize)
        btnEmptyTrash = view.findViewById(R.id.btnEmptyTrash)
        recyclerTrash = view.findViewById(R.id.recyclerTrash)
        emptyTrashView = view.findViewById(R.id.emptyTrashView)
    }

    private fun setupRecyclerView() {
        adapter = TrashAdapter(
            items = mutableListOf(),
            onRestore = { item -> restoreItem(item) },
            onSelectionChanged = { count -> updateSelectionUI(count) }
        )
        recyclerTrash.layoutManager = LinearLayoutManager(requireContext())
        recyclerTrash.adapter = adapter
    }

    private fun setupButtons() {
        btnEmptyTrash.setOnClickListener {
            showEmptyTrashConfirmation()
        }

        btnCancelTrashSelection.setOnClickListener {
            adapter.clearSelection()
        }

        btnRestoreSelected.setOnClickListener {
            val selected = adapter.getSelectedItems()
            if (selected.isNotEmpty()) {
                showRestoreConfirmation(selected)
            }
        }

        btnDeleteSelected.setOnClickListener {
            val selected = adapter.getSelectedItems()
            if (selected.isNotEmpty()) {
                showDeleteSelectedConfirmation(selected)
            }
        }
    }

    private fun updateSelectionUI(count: Int) {
        if (count > 0) {
            trashSelectionBar.visibility = View.VISIBLE
            trashHeader.visibility = View.GONE
            tvTrashSelectionCount.text = resources.getQuantityString(R.plurals.selection_count, count, count)
        } else {
            trashSelectionBar.visibility = View.GONE
            if (adapter.itemCount > 0) {
                trashHeader.visibility = View.VISIBLE
            }
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
                    trashSelectionBar.visibility = View.GONE
                    recyclerTrash.visibility = View.GONE
                    emptyTrashView.visibility = View.VISIBLE
                } else {
                    if (!adapter.isSelectionMode) {
                        trashHeader.visibility = View.VISIBLE
                    }
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

    private fun showRestoreConfirmation(selected: List<MediaItemEntity>) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_title)
            .setMessage(resources.getQuantityString(R.plurals.confirm_restore_selected, selected.size, selected.size))
            .setPositiveButton(R.string.btn_restore) { _, _ ->
                restoreSelectedItems(selected)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun restoreSelectedItems(selected: List<MediaItemEntity>) {
        lifecycleScope.launch(Dispatchers.IO) {
            for (item in selected) {
                db.mediaDao().updateStatus(item.mediaId, MediaStatus.KEPT, null)
            }
            withContext(Dispatchers.Main) {
                adapter.clearSelection()
                loadTrashItems()
                (activity as? MainActivity)?.updateTrashBadge()
            }
        }
    }

    private fun showDeleteSelectedConfirmation(selected: List<MediaItemEntity>) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_title)
            .setMessage(resources.getQuantityString(R.plurals.confirm_trash_selected, selected.size, selected.size))
            .setPositiveButton(R.string.btn_delete_permanent) { _, _ ->
                executePermanentDeletion(selected)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEmptyTrashConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_title)
            .setMessage(R.string.confirm_empty_trash)
            .setPositiveButton(R.string.btn_delete_permanent) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val allTrashed = db.mediaDao().getTrashedItems()
                    withContext(Dispatchers.Main) {
                        executePermanentDeletion(allTrashed)
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun executePermanentDeletion(itemsToDelete: List<MediaItemEntity>) {
        lifecycleScope.launch(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pendingDeleteItems = itemsToDelete
                val uris = itemsToDelete.map { item ->
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
                        lifecycleScope.launch(Dispatchers.IO) {
                            for (item in itemsToDelete) {
                                db.mediaDao().delete(item.mediaId)
                            }
                            pendingDeleteItems = null
                            withContext(Dispatchers.Main) {
                                adapter.clearSelection()
                                loadTrashItems()
                                (activity as? MainActivity)?.updateTrashBadge()
                            }
                        }
                    }
                }
            } else {
                var totalFreed = 0L
                for (item in itemsToDelete) {
                    try {
                        val uri = buildMediaUri(item.mediaId, item.mediaType)
                        requireContext().contentResolver.delete(uri, null, null)
                        totalFreed += item.fileSize
                    } catch (e: Exception) {
                        // Arquivo pode já ter sido removido
                    }
                    db.mediaDao().delete(item.mediaId)
                }
                if (totalFreed > 0) {
                    val prefs = requireContext().getSharedPreferences(PrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
                    val currentFreed = prefs.getLong(PrefsKeys.TOTAL_SPACE_FREED, 0L)
                    prefs.edit { putLong(PrefsKeys.TOTAL_SPACE_FREED, currentFreed + totalFreed) }
                }
                withContext(Dispatchers.Main) {
                    adapter.clearSelection()
                    loadTrashItems()
                    (activity as? MainActivity)?.updateTrashBadge()
                }
            }
        }
    }
}
