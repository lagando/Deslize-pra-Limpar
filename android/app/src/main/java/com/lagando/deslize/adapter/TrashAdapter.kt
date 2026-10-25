package com.lagando.deslize.adapter

import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lagando.deslize.R
import com.lagando.deslize.data.MediaItemEntity
import com.lagando.deslize.util.buildMediaUri
import java.util.concurrent.TimeUnit

/**
 * RecyclerView adapter for displaying trashed media items.
 * Shows thumbnail, filename, size, and expiry countdown for each item.
 */
class TrashAdapter(
    private var items: MutableList<MediaItemEntity>,
    private val onRestore: (MediaItemEntity) -> Unit
) : RecyclerView.Adapter<TrashAdapter.TrashViewHolder>() {

    inner class TrashViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivThumbnail: ImageView = view.findViewById(R.id.ivThumbnail)
        val tvItemName: TextView = view.findViewById(R.id.tvItemName)
        val tvItemSize: TextView = view.findViewById(R.id.tvItemSize)
        val tvItemExpiry: TextView = view.findViewById(R.id.tvItemExpiry)
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

        // Load thumbnail
        val contentUri = buildMediaUri(item.mediaId, item.mediaType)

        Glide.with(context)
            .load(contentUri)
            .centerCrop()
            .placeholder(R.color.surface_dark)
            .error(R.color.surface_dark)
            .into(holder.ivThumbnail)

        // Set text info
        holder.tvItemName.text = item.displayName
        holder.tvItemSize.text = Formatter.formatFileSize(context, item.fileSize)

        // Calculate days until expiry
        val trashedDate = item.trashedDate ?: System.currentTimeMillis()
        val expiryDate = trashedDate + TimeUnit.DAYS.toMillis(30)
        val daysLeft = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())
        holder.tvItemExpiry.text = context.resources.getQuantityString(R.plurals.trash_expires_in, daysLeft.toInt(), daysLeft.toInt())

        // Restore button
        holder.btnRestore.setOnClickListener {
            onRestore(item)
        }
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

    fun removeItem(position: Int) {
        if (position in items.indices) {
            items.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
