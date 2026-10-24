package com.lagando.deslize

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lagando.deslize.adapter.KeptAdapter
import com.lagando.deslize.data.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment displaying all kept photos/videos in a grid.
 * Supports multi-selection via long press to move items to trash.
 */
class KeptFragment : Fragment() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: KeptAdapter

    private lateinit var selectionBar: LinearLayout
    private lateinit var tvSelectionCount: TextView
    private lateinit var btnCancelSelection: MaterialButton
    private lateinit var btnTrashSelected: MaterialButton
    private lateinit var btnUnreviewSelected: MaterialButton
    private lateinit var keptHeader: LinearLayout
    private lateinit var tvKeptCount: TextView
    private lateinit var recyclerKept: RecyclerView
    private lateinit var emptyKeptView: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_kept, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        bindViews(view)
        setupRecyclerView()
        setupButtons()
        loadKeptItems()
    }

    private fun bindViews(view: View) {
        selectionBar = view.findViewById(R.id.selectionBar)
        tvSelectionCount = view.findViewById(R.id.tvSelectionCount)
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection)
        btnTrashSelected = view.findViewById(R.id.btnTrashSelected)
        btnUnreviewSelected = view.findViewById(R.id.btnUnreviewSelected)
        keptHeader = view.findViewById(R.id.keptHeader)
        tvKeptCount = view.findViewById(R.id.tvKeptCount)
        recyclerKept = view.findViewById(R.id.recyclerKept)
        emptyKeptView = view.findViewById(R.id.emptyKeptView)
    }

    private fun setupRecyclerView() {
        adapter = KeptAdapter(mutableListOf()) { selectedCount ->
            updateSelectionUI(selectedCount)
        }
        recyclerKept.layoutManager = GridLayoutManager(requireContext(), 3)
        recyclerKept.adapter = adapter
    }

    private fun setupButtons() {
        btnCancelSelection.setOnClickListener {
            adapter.clearSelection()
        }
        btnTrashSelected.setOnClickListener {
            val selected = adapter.getSelectedItems()
            if (selected.isNotEmpty()) {
                showTrashConfirmation(selected.size)
            }
        }
        btnUnreviewSelected.setOnClickListener {
            val selected = adapter.getSelectedItems()
            if (selected.isNotEmpty()) {
                showUnreviewConfirmation(selected.size)
            }
        }
    }

    private fun updateSelectionUI(count: Int) {
        if (count > 0) {
            selectionBar.visibility = View.VISIBLE
            keptHeader.visibility = View.GONE
            tvSelectionCount.text = resources.getQuantityString(R.plurals.selection_count, count, count)
        } else {
            selectionBar.visibility = View.GONE
            keptHeader.visibility = View.VISIBLE
        }
    }

    private fun loadKeptItems() {
        lifecycleScope.launch(Dispatchers.IO) {
            val items = db.mediaDao().getKeptItems()

            withContext(Dispatchers.Main) {
                if (items.isEmpty()) {
                    keptHeader.visibility = View.GONE
                    recyclerKept.visibility = View.GONE
                    emptyKeptView.visibility = View.VISIBLE
                } else {
                    keptHeader.visibility = View.VISIBLE
                    recyclerKept.visibility = View.VISIBLE
                    emptyKeptView.visibility = View.GONE
                    tvKeptCount.text = resources.getQuantityString(R.plurals.kept_count, items.size, items.size)
                    adapter.updateItems(items)
                }
            }
        }
    }

    private fun showTrashConfirmation(count: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_title)
            .setMessage(resources.getQuantityString(R.plurals.confirm_trash_selected, count, count))
            .setPositiveButton(R.string.btn_trash_selected) { _, _ ->
                trashSelectedItems()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun trashSelectedItems() {
        val selected = adapter.getSelectedItems()
        lifecycleScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            for (item in selected) {
                db.mediaDao().updateStatus(item.mediaId, "trashed", now)
            }
            withContext(Dispatchers.Main) {
                adapter.clearSelection()
                loadKeptItems()
                // Notify MainActivity to update trash badge
                (activity as? MainActivity)?.updateTrashBadge()
            }
        }
    }

    private fun showUnreviewConfirmation(count: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirm_title)
            .setMessage(resources.getQuantityString(R.plurals.confirm_unreview_selected, count, count))
            .setPositiveButton(R.string.btn_unreview_selected) { _, _ ->
                unreviewSelectedItems()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /**
     * Removes selected items from the database entirely, so they reappear
     * as unreviewed in the swipe screen for the user to decide again.
     */
    private fun unreviewSelectedItems() {
        val selected = adapter.getSelectedItems()
        lifecycleScope.launch(Dispatchers.IO) {
            for (item in selected) {
                db.mediaDao().delete(item.mediaId)
            }
            withContext(Dispatchers.Main) {
                adapter.clearSelection()
                loadKeptItems()
            }
        }
    }
}
