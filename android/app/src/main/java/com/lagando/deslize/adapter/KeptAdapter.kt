package com.lagando.deslize.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lagando.deslize.R
import com.lagando.deslize.data.MediaItemEntity
import com.lagando.deslize.model.MediaType
import com.lagando.deslize.util.buildMediaUri

/**
 * Grid adapter for displaying kept photos/videos.
 * Supports multi-selection via long press for batch operations.
 */
class KeptAdapter(
    private var items: MutableList<MediaItemEntity>,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<KeptAdapter.KeptViewHolder>() {

    private val selectedIds = mutableSetOf<Long>()
    var isSelectionMode = false
        private set

    inner class KeptViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivGridThumbnail)
        val tvVideoBadge: TextView = view.findViewById(R.id.tvGridVideoBadge)
        val selectionOverlay: View = view.findViewById(R.id.selectionOverlay)
        val ivCheckmark: ImageView = view.findViewById(R.id.ivCheckmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kept_grid, parent, false)
        // Make items square
        val size = parent.measuredWidth / 3
        view.layoutParams = RecyclerView.LayoutParams(size, size)
        return KeptViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeptViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        // Load thumbnail
        val contentUri = buildMediaUri(item.mediaId, item.mediaType)

        Glide.with(context)
            .load(contentUri)
            .override(holder.itemView.width.coerceAtLeast(360), holder.itemView.width.coerceAtLeast(360))
            .centerCrop()
            .placeholder(R.color.surface_elevated)
            .error(R.color.surface_elevated)
            .into(holder.ivThumbnail)

        // Video badge
        holder.tvVideoBadge.visibility = if (item.mediaType == MediaType.VIDEO) View.VISIBLE else View.GONE

        // Selection state
        val isSelected = selectedIds.contains(item.mediaId)
        holder.selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.ivCheckmark.visibility = if (isSelected) View.VISIBLE else View.GONE

        // Click handling
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(item.mediaId, position)
            } else {
                // Open in the in-app fullscreen viewer
                val intent = android.content.Intent(context, com.lagando.deslize.PhotoViewerActivity::class.java).apply {
                    putExtra(com.lagando.deslize.PhotoViewerActivity.EXTRA_MEDIA_URI, contentUri.toString())
                    putExtra(com.lagando.deslize.PhotoViewerActivity.EXTRA_MEDIA_TYPE, item.mediaType)
                    putExtra(com.lagando.deslize.PhotoViewerActivity.EXTRA_DISPLAY_NAME, item.displayName)
                }
                context.startActivity(intent)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
            }
            toggleSelection(item.mediaId, position)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    private fun toggleSelection(mediaId: Long, position: Int) {
        if (selectedIds.contains(mediaId)) {
            selectedIds.remove(mediaId)
        } else {
            selectedIds.add(mediaId)
        }
        notifyItemChanged(position)

        if (selectedIds.isEmpty()) {
            isSelectionMode = false
        }
        onSelectionChanged(selectedIds.size)
    }

    fun getSelectedItems(): List<MediaItemEntity> {
        return items.filter { selectedIds.contains(it.mediaId) }
    }

    fun clearSelection() {
        val previouslySelected = selectedIds.toList()
        selectedIds.clear()
        isSelectionMode = false
        previouslySelected.forEach { id ->
            val index = items.indexOfFirst { it.mediaId == id }
            if (index != -1) notifyItemChanged(index)
        }
        onSelectionChanged(0)
    }

    fun updateItems(newItems: List<MediaItemEntity>) {
        val diffCallback = object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) = items[oldPos].mediaId == newItems[newPos].mediaId
            override fun areContentsTheSame(oldPos: Int, newPos: Int) = items[oldPos] == newItems[newPos]
        }
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(diffCallback)
        
        items.clear()
        items.addAll(newItems)
        selectedIds.clear()
        isSelectionMode = false
        diffResult.dispatchUpdatesTo(this)
    }
}
