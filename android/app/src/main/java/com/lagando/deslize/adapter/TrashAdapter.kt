package com.lagando.deslize.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.lagando.deslize.R
import com.lagando.deslize.data.MediaItemEntity
import com.lagando.deslize.util.buildMediaUri
import java.util.concurrent.TimeUnit

/**
 * RecyclerView adapter para exibir itens na lixeira.
 * Suporta seleção múltipla por long-press ou toque no modo de seleção.
 */
class TrashAdapter(
    private var items: MutableList<MediaItemEntity>,
    private val onRestore: (MediaItemEntity) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    private val selectedIds = mutableSetOf<Long>()
    var isSelectionMode = false
        private set

    inner class TrashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardTrashItem)
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemSize: TextView = view.findViewById(R.id.tvItemSize)
        val tvItemExpiry: TextView = view.findViewById(R.id.tvItemExpiry)
        val ivCheckmark: ImageView = view.findViewById(R.id.ivCheckmark)
        val btnRestore: ImageButton = view.findViewById(R.id.btnRestore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrashViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trash, parent, false)
        return TrashViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrashViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        // Carrega miniatura
        val contentUri = buildMediaUri(item.mediaId, item.mediaType)
        Glide.with(context)
            .load(contentUri)
            .centerCrop()
            .placeholder(R.color.surface_dark)
            .error(R.color.surface_dark)
            .into(holder.ivThumbnail)

        // Textos
        holder.tvItemName.text = item.displayName
        holder.tvItemSize.text = Formatter.formatFileSize(context, item.fileSize)

        // Contagem regressiva de expiração
        val trashedDate = item.trashedDate ?: System.currentTimeMillis()
        val expiryDate = trashedDate + TimeUnit.DAYS.toMillis(30)
        val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())
        holder.tvItemExpiry.text = context.resources.getQuantityString(
            R.plurals.trash_expires_in, daysLeft.toInt(), daysLeft.toInt()
        )

        // Estado de seleção
        val isSelected = selectedIds.contains(item.mediaId)
        if (isSelected) {
            holder.card.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()
            holder.card.strokeColor = ContextCompat.getColor(context, R.color.delete_red)
            holder.ivCheckmark.visibility = View.VISIBLE
            holder.btnRestore.visibility = View.GONE
        } else {
            holder.card.strokeWidth = 0
            holder.ivCheckmark.visibility = View.GONE
            holder.btnRestore.visibility = if (isSelectionMode) View.INVISIBLE else View.VISIBLE
        }

        // Interações
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(item)
            true
        }

        holder.btnRestore.setOnClickListener {
            if (!isSelectionMode) {
                onRestore(item)
            }
        }
    }

    private fun toggleSelection(item: MediaItemEntity) {
        if (selectedIds.contains(item.mediaId)) {
            selectedIds.remove(item.mediaId)
            if (selectedIds.isEmpty()) {
                isSelectionMode = false
            }
        } else {
            selectedIds.add(item.mediaId)
            isSelectionMode = true
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedIds.size)
    }

    fun getSelectedItems(): List<MediaItemEntity> {
        return items.filter { it.mediaId in selectedIds }
    }

    fun clearSelection() {
        selectedIds.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    override fun getItemCount(): Int = items.size

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
        diffResult.dispatchUpdatesTo(this)
    }
}
