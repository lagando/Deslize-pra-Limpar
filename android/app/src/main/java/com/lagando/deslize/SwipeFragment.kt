package com.lagando.deslize

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.lagando.deslize.data.AppDatabase
import com.lagando.deslize.data.MediaItemEntity
import com.lagando.deslize.data.PrefsKeys
import com.lagando.deslize.model.MediaItem
import com.lagando.deslize.model.MediaStatus
import com.lagando.deslize.model.MediaType
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min

/**
 * Main fragment for the swipe-to-clean interface.
 * Displays photos/videos fullscreen and allows swiping:
 * - Left to mark for deletion (trash)
 * - Right to keep
 * Also provides Excluir/Manter buttons as an alternative to swiping.
 * Supports random or sequential ordering via toolbar toggle.
 */
class SwipeFragment : Fragment() {

    private lateinit var db: AppDatabase

    // Views
    private lateinit var swipeContent: LinearLayout
    private lateinit var mediaContainer: View
    private lateinit var imageView: ImageView
    private lateinit var videoThumbnail: ImageView
    private lateinit var playButton: ImageView
    private lateinit var overlayDelete: View
    private lateinit var overlayKeep: View
    private lateinit var iconOverlayDelete: ImageView
    private lateinit var iconOverlayKeep: ImageView
    private lateinit var tvCounter: TextView
    private lateinit var tvVideoBadge: TextView
    private lateinit var tvSwipeHint: TextView
    private lateinit var tvFileInfo: TextView
    private lateinit var tvMediaDate: TextView
    private lateinit var btnShareMedia: ImageButton
    private lateinit var btnDelete: MaterialButton
    private lateinit var btnKeep: MaterialButton
    private lateinit var btnDecideLater: MaterialButton
    private lateinit var allCleanView: LinearLayout
    private lateinit var noPermissionView: LinearLayout
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var progressBar: ProgressBar

    // Mode banner views
    private lateinit var modeBanner: LinearLayout
    private lateinit var tvBannerIcon: TextView
    private lateinit var tvBannerTitle: TextView
    private lateinit var tvBannerSubtitle: TextView
    private var bannerRunnable: Runnable? = null

    // All-clean permission hint views
    private lateinit var tvAllCleanPermissionHint: TextView
    private lateinit var btnAllCleanGrantPermission: MaterialButton

    // Track if we were showing the all-clean view (for onResume reload)
    private var isShowingAllClean = false

    // Data
    private var mediaItems = mutableListOf<MediaItem>()
    private var currentIndex = 0
    private var totalCount = 0
    private var isAnimating = false

    // Random mode
    private var isRandomMode = false
    private var randomMenuItem: MenuItem? = null

    // Sort by size mode
    private var isSortBySizeMode = false
    private var sortBySizeMenuItem: MenuItem? = null

    // Undo tracking
    private var undoMenuItem: MenuItem? = null
    private var lastAction: UndoAction? = null

    private data class UndoAction(
        val item: MediaItem,
        val status: String // "trashed" or "kept"
    )

    // Swipe gesture
    private var startX = 0f
    private val SWIPE_THRESHOLD = 250f
    private var screenWidth = 0
    private var screenHeight = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels
        bindViews(view)
        setupButtons()
        setupSwipeGesture()
        setupToolbarMenu()
        loadMediaItems()
    }

    private fun bindViews(view: View) {
        swipeContent = view.findViewById(R.id.swipeContent)
        mediaContainer = view.findViewById(R.id.mediaContainer)
        imageView = view.findViewById(R.id.imageView)
        videoThumbnail = view.findViewById(R.id.videoThumbnail)
        playButton = view.findViewById(R.id.playButton)
        overlayDelete = view.findViewById(R.id.overlayDelete)
        overlayKeep = view.findViewById(R.id.overlayKeep)
        iconOverlayDelete = view.findViewById(R.id.iconOverlayDelete)
        iconOverlayKeep = view.findViewById(R.id.iconOverlayKeep)
        tvCounter = view.findViewById(R.id.tvCounter)
        tvVideoBadge = view.findViewById(R.id.tvVideoBadge)
        tvSwipeHint = view.findViewById(R.id.tvSwipeHint)
        tvFileInfo = view.findViewById(R.id.tvFileInfo)
        tvMediaDate = view.findViewById(R.id.tvMediaDate)
        btnShareMedia = view.findViewById(R.id.btnShareMedia)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnKeep = view.findViewById(R.id.btnKeep)
        btnDecideLater = view.findViewById(R.id.btnDecideLater)
        allCleanView = view.findViewById(R.id.allCleanView)
        noPermissionView = view.findViewById(R.id.noPermissionView)
        btnGrantPermission = view.findViewById(R.id.btnGrantPermission)
        progressBar = view.findViewById(R.id.progressBar)
        tvAllCleanPermissionHint = view.findViewById(R.id.tvAllCleanPermissionHint)
        btnAllCleanGrantPermission = view.findViewById(R.id.btnAllCleanGrantPermission)
        modeBanner = view.findViewById(R.id.modeBanner)
        tvBannerIcon = view.findViewById(R.id.tvBannerIcon)
        tvBannerTitle = view.findViewById(R.id.tvBannerTitle)
        tvBannerSubtitle = view.findViewById(R.id.tvBannerSubtitle)
    }

    private fun setupButtons() {
        btnDelete.setOnClickListener {
            if (!isAnimating && currentIndex < mediaItems.size) {
                animateOut(-1) { markAsDeleted() }
            }
        }
        btnKeep.setOnClickListener {
            if (!isAnimating && currentIndex < mediaItems.size) {
                animateOut(1) { markAsKept() }
            }
        }
        btnDecideLater.setOnClickListener {
            if (!isAnimating && currentIndex < mediaItems.size) {
                // Skip without saving to database - item will reappear next session
                skipCurrent()
            }
        }
        btnGrantPermission.setOnClickListener {
            (activity as? MainActivity)?.requestMediaPermissions()
        }
        btnAllCleanGrantPermission.setOnClickListener {
            handleGrantPermissionClick()
        }
        playButton.setOnClickListener {
            if (!isAnimating && currentIndex < mediaItems.size) {
                val item = mediaItems[currentIndex]
                if (item.mediaType == MediaType.VIDEO) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                    intent.setDataAndType(item.contentUri, "video/*")
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    startActivity(intent)
                }
            }
        }
        btnShareMedia.setOnClickListener {
            if (currentIndex < mediaItems.size) {
                val item = mediaItems[currentIndex]
                val mimeType = if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
                val promoText = if (isRandomMode) {
                    getString(R.string.share_promo_text_random)
                } else {
                    getString(R.string.share_promo_text)
                }
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, item.contentUri)
                    putExtra(Intent.EXTRA_TEXT, promoText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
            }
        }
    }

    /**
     * Sets up the toolbar menu with the random/sequential toggle button.
     * Uses MenuProvider API (modern approach, no deprecated setHasOptionsMenu).
     */
    private fun setupToolbarMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu)
                undoMenuItem = menu.findItem(R.id.action_undo)
                sortBySizeMenuItem = menu.findItem(R.id.action_sort_by_size)
                randomMenuItem = menu.findItem(R.id.action_toggle_random)
                updateRandomIcon()
                updateSortBySizeIcon()
                updateUndoButton()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_undo -> {
                        performUndo()
                        true
                    }
                    R.id.action_sort_by_size -> {
                        toggleSortBySize()
                        true
                    }
                    R.id.action_toggle_random -> {
                        toggleRandomMode()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Toggles between random and sequential ordering mode.
     * When switching to random: shuffles remaining items from currentIndex onward.
     * When switching to sequential: re-sorts remaining items by date (newest first).
     */
    private fun toggleRandomMode() {
        isRandomMode = !isRandomMode

        // Mutual exclusion: deactivate sort-by-size when enabling random
        if (isRandomMode && isSortBySizeMode) {
            isSortBySizeMode = false
            updateSortBySizeIcon()
        }

        updateRandomIcon()

        if (mediaItems.isEmpty() || currentIndex >= mediaItems.size) return

        if (isRandomMode) {
            showModeBanner("\uD83D\uDD00", R.string.mode_random, R.string.mode_random_detail)
            if (mediaItems.size > 1) {
                currentIndex = (0 until mediaItems.size).random()
            }
            resetViewPositionsImmediate()
            showMedia(mediaItems[currentIndex])
            updateCounter()
        } else {
            showModeBanner("\uD83D\uDCC5", R.string.mode_sequential, R.string.mode_sequential_detail)
            mediaItems.sortByDescending { it.dateAdded }
            currentIndex = 0
            resetViewPositionsImmediate()
            showMedia(mediaItems[currentIndex])
            updateCounter()
        }
    }

    /**
     * Toggles sort-by-size mode.
     * When enabled: sorts items by file size (largest first) for prioritized cleanup.
     * When disabled: reverts to default date ordering (newest first).
     * Mutually exclusive with random mode.
     */
    private fun toggleSortBySize() {
        isSortBySizeMode = !isSortBySizeMode

        // Mutual exclusion: deactivate random when enabling sort-by-size
        if (isSortBySizeMode && isRandomMode) {
            isRandomMode = false
            updateRandomIcon()
        }

        updateSortBySizeIcon()

        if (mediaItems.isEmpty() || currentIndex >= mediaItems.size) return

        if (isSortBySizeMode) {
            showModeBanner("\uD83D\uDCCA", R.string.mode_sort_by_size, R.string.mode_sort_by_size_detail)
            mediaItems.sortByDescending { it.size }
            currentIndex = 0
            resetViewPositionsImmediate()
            showMedia(mediaItems[currentIndex])
            updateCounter()
        } else {
            showModeBanner("\uD83D\uDCC5", R.string.mode_sort_by_date, R.string.mode_sort_by_date_detail)
            mediaItems.sortByDescending { it.dateAdded }
            currentIndex = 0
            resetViewPositionsImmediate()
            showMedia(mediaItems[currentIndex])
            updateCounter()
        }
    }

    private fun updateRandomIcon() {
        randomMenuItem?.setIcon(
            if (isRandomMode) R.drawable.ic_sequential else R.drawable.ic_shuffle
        )
    }

    /** Updates the sort-by-size icon opacity to reflect active/inactive state. */
    private fun updateSortBySizeIcon() {
        sortBySizeMenuItem?.icon?.alpha = if (isSortBySizeMode) 255 else 120
    }

    /**
     * Shows an animated banner at the top of the screen to provide clear feedback
     * when the user toggles a display mode (random, sequential, sort-by-size, etc.).
     * The banner slides in from the top, stays visible for 2 seconds, then fades out.
     * Multiple rapid taps cancel the previous banner before showing the new one.
     */
    private fun showModeBanner(icon: String, titleRes: Int, subtitleRes: Int) {
        // Cancel any pending hide animation
        bannerRunnable?.let { modeBanner.removeCallbacks(it) }
        modeBanner.animate().cancel()

        // Set content
        tvBannerIcon.text = icon
        tvBannerTitle.text = getString(titleRes)
        tvBannerSubtitle.text = getString(subtitleRes)

        // Show and animate in
        modeBanner.visibility = View.VISIBLE
        modeBanner.alpha = 1f
        modeBanner.translationY = -100f * resources.displayMetrics.density
        modeBanner.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

        // Schedule fade-out after 2 seconds
        bannerRunnable = Runnable {
            modeBanner.animate()
                .alpha(0f)
                .translationY(-30f * resources.displayMetrics.density)
                .setDuration(300)
                .withEndAction {
                    modeBanner.visibility = View.GONE
                }
                .start()
        }
        modeBanner.postDelayed(bannerRunnable!!, 2000)
    }

    /**
     * Undoes the last swipe action.
     * Removes the item from the database and re-inserts it into the current list.
     */
    private fun performUndo() {
        val action = lastAction ?: return
        lastAction = null
        updateUndoButton()

        lifecycleScope.launch(Dispatchers.IO) {
            // Remove the item from the database (un-review it)
            db.mediaDao().delete(action.item.id)
            withContext(Dispatchers.Main) {
                // Re-insert the item at the current position
                mediaItems.add(currentIndex, action.item)
                // Ensure swipe UI is visible (might have been hidden by "all clean" screen)
                swipeContent.visibility = View.VISIBLE
                allCleanView.visibility = View.GONE
                resetViewPositionsImmediate()
                showMedia(action.item)
                updateCounter()
                // Update badge if we undid a trash action
                if (action.status == MediaStatus.TRASHED) {
                    (activity as? MainActivity)?.updateTrashBadge()
                }
            }
        }
    }

    private fun updateUndoButton() {
        undoMenuItem?.isEnabled = lastAction != null
        undoMenuItem?.icon?.alpha = if (lastAction != null) 255 else 77
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupSwipeGesture() {
        mediaContainer.setOnTouchListener { _, event ->
            if (isAnimating || currentIndex >= mediaItems.size) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    // Translate and rotate the image container children
                    imageView.translationX = dx
                    imageView.rotation = dx / 50f
                    videoThumbnail.translationX = dx
                    videoThumbnail.rotation = dx / 50f

                    // Show overlays based on direction
                    if (dx < -30) {
                        val progress = min(1f, abs(dx) / SWIPE_THRESHOLD)
                        overlayDelete.alpha = progress * 0.5f
                        iconOverlayDelete.alpha = progress
                        overlayKeep.alpha = 0f
                        iconOverlayKeep.alpha = 0f
                    } else if (dx > 30) {
                        val progress = min(1f, abs(dx) / SWIPE_THRESHOLD)
                        overlayKeep.alpha = progress * 0.5f
                        iconOverlayKeep.alpha = progress
                        overlayDelete.alpha = 0f
                        iconOverlayDelete.alpha = 0f
                    } else {
                        overlayDelete.alpha = 0f
                        overlayKeep.alpha = 0f
                        iconOverlayDelete.alpha = 0f
                        iconOverlayKeep.alpha = 0f
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = event.rawX - startX
                    if (dx < -SWIPE_THRESHOLD) {
                        // Swiped left - Delete
                        animateOut(-1) { markAsDeleted() }
                    } else if (dx > SWIPE_THRESHOLD) {
                        // Swiped right - Keep
                        animateOut(1) { markAsKept() }
                    } else {
                        // Spring back
                        resetViewPositions()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun animateOut(direction: Int, onComplete: () -> Unit) {
        isAnimating = true
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val targetX = if (direction < 0) -screenWidth * 1.5f else screenWidth * 1.5f

        imageView.animate()
            .translationX(targetX)
            .rotation(direction * 30f)
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                onComplete()
                isAnimating = false
            }
            .start()

        videoThumbnail.animate()
            .translationX(targetX)
            .rotation(direction * 30f)
            .alpha(0f)
            .setDuration(300)
            .start()
    }

    private fun resetViewPositions() {
        imageView.animate().translationX(0f).rotation(0f).alpha(1f).setDuration(200).start()
        videoThumbnail.animate().translationX(0f).rotation(0f).alpha(1f).setDuration(200).start()
        overlayDelete.animate().alpha(0f).setDuration(200).start()
        overlayKeep.animate().alpha(0f).setDuration(200).start()
        iconOverlayDelete.animate().alpha(0f).setDuration(200).start()
        iconOverlayKeep.animate().alpha(0f).setDuration(200).start()
    }

    private fun markAsDeleted() {
        if (currentIndex >= mediaItems.size) return
        val item = mediaItems[currentIndex]

        lifecycleScope.launch(Dispatchers.IO) {
            db.mediaDao().insert(
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
            withContext(Dispatchers.Main) {
                // Save undo state before removing
                lastAction = UndoAction(item, MediaStatus.TRASHED)
                updateUndoButton()
                // Remove the item from the list since it's been reviewed
                mediaItems.removeAt(currentIndex)
                showNextItem()
                (activity as? MainActivity)?.updateTrashBadge()
            }
        }
    }

    private fun markAsKept() {
        if (currentIndex >= mediaItems.size) return
        val item = mediaItems[currentIndex]

        lifecycleScope.launch(Dispatchers.IO) {
            db.mediaDao().insert(
                MediaItemEntity(
                    mediaId = item.id,
                    mediaUri = item.contentUri.toString(),
                    status = MediaStatus.KEPT,
                    fileSize = item.size,
                    mediaType = item.mediaType,
                    displayName = item.displayName
                )
            )
            withContext(Dispatchers.Main) {
                // Save undo state before removing
                lastAction = UndoAction(item, MediaStatus.KEPT)
                updateUndoButton()
                // Remove the item from the list since it's been reviewed
                mediaItems.removeAt(currentIndex)
                showNextItem()
            }
        }
    }

    /**
     * Skips the current item without saving to database ("Decidir depois").
     * The item is moved to the end of the list so user sees other items first,
     * but will come back to it if they keep going. On next app session,
     * it will also reappear since it was never saved to the database.
     */
    private fun skipCurrent() {
        if (currentIndex >= mediaItems.size) return

        // Move item to end of list so user sees it again later
        val skippedItem = mediaItems.removeAt(currentIndex)
        mediaItems.add(skippedItem)

        // Reset views and show next (currentIndex stays the same since we removed the current)
        resetViewPositionsImmediate()
        showNextItem()
    }

    /**
     * Shows the next item in the list after an action (delete, keep, or skip).
     * If in random mode, picks a random index. Otherwise sequential.
     */
    private fun showNextItem() {
        if (mediaItems.isEmpty()) {
            showAllClean()
            return
        }

        // After removing/moving an item, currentIndex may point to the next item already
        // In random mode, pick a random index from the remaining items
        if (isRandomMode && mediaItems.size > 1) {
            currentIndex = (0 until mediaItems.size).random()
        } else {
            // In sequential mode, keep currentIndex (since we removed the previous item)
            // But make sure we don't go out of bounds
            if (currentIndex >= mediaItems.size) {
                currentIndex = 0
            }
        }

        resetViewPositionsImmediate()
        showMedia(mediaItems[currentIndex])
        updateCounter()
    }

    /** Reset view positions immediately without animation (for transitions) */
    private fun resetViewPositionsImmediate() {
        imageView.translationX = 0f
        imageView.rotation = 0f
        imageView.alpha = 1f
        videoThumbnail.translationX = 0f
        videoThumbnail.rotation = 0f
        videoThumbnail.alpha = 1f
        overlayDelete.alpha = 0f
        overlayKeep.alpha = 0f
        iconOverlayDelete.alpha = 0f
        iconOverlayKeep.alpha = 0f
    }

    fun loadMediaItems() {
        progressBar.visibility = View.VISIBLE
        swipeContent.visibility = View.GONE
        allCleanView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val reviewedIds = db.mediaDao().getReviewedIds().toSet()
            val allMedia = queryMediaStore()
            val unreviewed = allMedia.filter { it.id !in reviewedIds }.toMutableList()

            // Apply sorting based on active mode
            if (isRandomMode) {
                unreviewed.shuffle()
            } else if (isSortBySizeMode) {
                unreviewed.sortByDescending { it.size }
            }

            withContext(Dispatchers.Main) {
                progressBar.visibility = View.GONE
                mediaItems.clear()
                mediaItems.addAll(unreviewed)
                totalCount = allMedia.size
                currentIndex = 0

                if (mediaItems.isNotEmpty()) {
                    swipeContent.visibility = View.VISIBLE

                    // In random mode, start at a random position
                    if (isRandomMode && mediaItems.size > 1) {
                        currentIndex = (0 until mediaItems.size).random()
                    }

                    showMedia(mediaItems[currentIndex])
                    updateCounter()
                } else {
                    showAllClean()
                }
            }
        }
    }

    private fun showMedia(item: MediaItem) {
        if (item.mediaType == MediaType.VIDEO) {
            imageView.visibility = View.GONE
            videoThumbnail.visibility = View.VISIBLE
            playButton.visibility = View.VISIBLE
            tvVideoBadge.visibility = View.VISIBLE

            Glide.with(this)
                .load(item.contentUri)
                .override(screenWidth, screenHeight)
                .centerInside()
                .placeholder(R.color.background_dark)
                .into(videoThumbnail)
        } else {
            imageView.visibility = View.VISIBLE
            videoThumbnail.visibility = View.GONE
            playButton.visibility = View.GONE
            tvVideoBadge.visibility = View.GONE

            Glide.with(this)
                .load(item.contentUri)
                .thumbnail(
                    Glide.with(this)
                        .load(item.contentUri)
                        .override(screenWidth / 10, screenHeight / 10)
                )
                .override(screenWidth, screenHeight)
                .fitCenter()
                .placeholder(R.color.background_dark)
                .into(imageView)
        }

        // File info
        val sizeStr = Formatter.formatFileSize(requireContext(), item.size)
        tvFileInfo.text = getString(R.string.file_info_format, item.displayName, sizeStr)

        // Date display (subtle)
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val date = java.util.Date(item.dateAdded * 1000) // dateAdded is in seconds
        tvMediaDate.text = dateFormat.format(date)

        // Preload next photos into Glide cache for instant swipe
        preloadUpcoming()
    }

    /**
     * Preloads the next 4 upcoming photos into Glide's memory cache.
     * In sequential mode: preloads the next 4 items from current position.
     * In random mode: preloads 4 random items from remaining list.
     * This ensures the next swipe displays the photo instantly.
     */
    private fun preloadUpcoming() {
        if (mediaItems.size <= 1) return

        val itemsToPreload = if (isRandomMode) {
            mediaItems.indices
                .filter { it != currentIndex }
                .shuffled()
                .take(4)
                .map { mediaItems[it] }
        } else {
            (1..4).mapNotNull { offset ->
                val idx = currentIndex + offset
                if (idx < mediaItems.size) mediaItems[idx] else null
            }
        }

        for (item in itemsToPreload) {
            Glide.with(this)
                .load(item.contentUri)
                .override(screenWidth, screenHeight)
                .preload()
        }
    }

    private fun updateCounter() {
        tvCounter.text = getString(R.string.counter_format, currentIndex + 1, mediaItems.size)
    }

    private fun showAllClean() {
        swipeContent.visibility = View.GONE
        allCleanView.visibility = View.VISIBLE
        isShowingAllClean = true

        // Check if we actually have full permissions
        val mainActivity = activity as? MainActivity
        if (mainActivity != null && !mainActivity.hasMediaPermissions()) {
            // No full permission — show hint and button
            tvAllCleanPermissionHint.visibility = View.VISIBLE
            btnAllCleanGrantPermission.visibility = View.VISIBLE

            // Update button text based on whether permission is permanently denied
            if (isPermissionPermanentlyDenied()) {
                btnAllCleanGrantPermission.text = getString(R.string.btn_open_settings)
            } else {
                btnAllCleanGrantPermission.text = getString(R.string.btn_grant_permission)
            }
        } else {
            // Has full permission — normal "all clean" screen
            tvAllCleanPermissionHint.visibility = View.GONE
            btnAllCleanGrantPermission.visibility = View.GONE
        }
    }

    /**
     * Called from MainActivity.onResume() when returning from Settings.
     * If we were showing the "all clean" view and now have permission, reload media.
     */
    fun reloadIfNeeded() {
        if (isShowingAllClean) {
            isShowingAllClean = false
            loadMediaItems()
        }
    }

    /**
     * Handles the "grant permission" button click on the all-clean screen.
     * If permission is permanently denied (user rejected 2x), opens app Settings.
     * Otherwise, requests permission normally.
     */
    private fun handleGrantPermissionClick() {
        val act = activity ?: return

        // Mark that we've attempted to request permission
        val prefs = act.getSharedPreferences(PrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val alreadyRequested = prefs.getBoolean(PrefsKeys.PERMISSION_REQUESTED, false)
        prefs.edit().putBoolean(PrefsKeys.PERMISSION_REQUESTED, true).apply()

        if (isPermissionPermanentlyDenied() && alreadyRequested) {
            // Permission permanently denied — open app settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", act.packageName, null)
            }
            startActivity(intent)
        } else {
            // Request permission normally
            (act as? MainActivity)?.requestMediaPermissions()
        }
    }

    /**
     * Checks if the media permission has been permanently denied.
     * Returns true if shouldShowRequestPermissionRationale is false
     * (which means user selected "Don't ask again" or denied twice).
     */
    private fun isPermissionPermanentlyDenied(): Boolean {
        val act = activity ?: return false
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        // If shouldShowRequestPermissionRationale returns false AND we've already
        // requested before, it means the user permanently denied.
        return !ActivityCompat.shouldShowRequestPermissionRationale(act, permission)
    }

    fun showNoPermission() {
        swipeContent.visibility = View.GONE
        allCleanView.visibility = View.GONE
        progressBar.visibility = View.GONE
        noPermissionView.visibility = View.VISIBLE
    }

    private fun queryMediaStore(): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()
        val context = requireContext()

        // Query images
        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED
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

                while (cursor.moveToNext()) {
                    try {
                        val id = if (idCol != -1) cursor.getLong(idCol) else continue
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        val name = if (nameCol != -1) cursor.getString(nameCol) else null
                        
                        mediaList.add(
                            MediaItem(
                                id = id,
                                contentUri = uri,
                                displayName = name ?: context.getString(R.string.unknown_filename),
                                size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                                dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis() / 1000,
                                mediaType = MediaType.IMAGE
                            )
                        )
                    } catch (e: Exception) {
                        // Skip corrupted item
                    }
                }
            }
        } catch (e: Exception) {
            // Handle possible SecurityException or unsupported query
        }

        // Query videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION
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

                while (cursor.moveToNext()) {
                    try {
                        val id = if (idCol != -1) cursor.getLong(idCol) else continue
                        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                        val name = if (nameCol != -1) cursor.getString(nameCol) else null
                        
                        mediaList.add(
                            MediaItem(
                                id = id,
                                contentUri = uri,
                                displayName = name ?: context.getString(R.string.unknown_filename),
                                size = if (sizeCol != -1) cursor.getLong(sizeCol) else 0L,
                                dateAdded = if (dateCol != -1) cursor.getLong(dateCol) else System.currentTimeMillis() / 1000,
                                mediaType = MediaType.VIDEO,
                                duration = if (durCol != -1) cursor.getLong(durCol) else 0L
                            )
                        )
                    } catch (e: Exception) {
                        // Skip corrupted item
                    }
                }
            }
        } catch (e: Exception) {
            // Handle possible SecurityException or unsupported query
        }

        // Sort by date (newest first)
        mediaList.sortByDescending { it.dateAdded }
        return mediaList
    }
}
