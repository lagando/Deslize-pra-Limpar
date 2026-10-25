package com.lagando.deslize

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import com.bumptech.glide.Glide
import com.lagando.deslize.data.PrefsKeys
import com.lagando.deslize.model.MediaItem
import com.lagando.deslize.model.MediaStatus
import com.lagando.deslize.model.MediaType
import com.google.android.material.button.MaterialButton
import kotlin.math.abs
import kotlin.math.min

/**
 * Fragment principal para a interface de swipe-to-clean.
 * Exibe fotos e vídeos e permite deslizar:
 * - Esquerda: marcar para a lixeira
 * - Direita: manter na galeria
 * Utiliza SwipeViewModel para preservar o estado através de rotações de tela.
 */
class SwipeFragment : Fragment() {

    private val viewModel: SwipeViewModel by viewModels()

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

    private var isShowingAllClean = false
    private var isAnimating = false

    private var randomMenuItem: MenuItem? = null
    private var sortBySizeMenuItem: MenuItem? = null
    private var undoMenuItem: MenuItem? = null

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
        screenWidth = resources.displayMetrics.widthPixels
        screenHeight = resources.displayMetrics.heightPixels

        bindViews(view)
        setupButtons()
        setupSwipeGesture()
        setupToolbarMenu()
        observeViewModel()

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

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.mediaItems.observe(viewLifecycleOwner) { items ->
            if (items.isNullOrEmpty()) {
                if (viewModel.isLoading.value != true) {
                    showAllClean()
                }
            } else {
                swipeContent.visibility = View.VISIBLE
                allCleanView.visibility = View.GONE
                val idx = viewModel.currentIndex.value ?: 0
                val safeIdx = idx.coerceIn(0, items.size - 1)
                resetViewPositionsImmediate()
                showMedia(items[safeIdx])
                updateCounter(safeIdx, items.size)
            }
        }

        viewModel.lastAction.observe(viewLifecycleOwner) { action ->
            undoMenuItem?.isEnabled = action != null
            undoMenuItem?.icon?.alpha = if (action != null) 255 else 77
        }

        viewModel.isRandomMode.observe(viewLifecycleOwner) { isRandom ->
            randomMenuItem?.icon?.mutate()?.alpha = if (isRandom) 255 else 120
        }

        viewModel.isSortBySizeMode.observe(viewLifecycleOwner) { isSort ->
            sortBySizeMenuItem?.icon?.mutate()?.alpha = if (isSort) 255 else 120
        }
    }

    private fun setupButtons() {
        btnDelete.setOnClickListener {
            if (!isAnimating && getCurrentItem() != null) {
                animateOut(-1) {
                    viewModel.markCurrentAsDeleted {
                        (activity as? MainActivity)?.updateTrashBadge()
                    }
                }
            }
        }

        btnKeep.setOnClickListener {
            if (!isAnimating && getCurrentItem() != null) {
                animateOut(1) {
                    viewModel.markCurrentAsKept {}
                }
            }
        }

        btnDecideLater.setOnClickListener {
            if (!isAnimating && getCurrentItem() != null) {
                resetViewPositionsImmediate()
                viewModel.skipCurrent {}
            }
        }

        btnGrantPermission.setOnClickListener {
            (activity as? MainActivity)?.requestMediaPermissions()
        }

        btnAllCleanGrantPermission.setOnClickListener {
            handleGrantPermissionClick()
        }

        playButton.setOnClickListener {
            val item = getCurrentItem()
            if (!isAnimating && item != null && item.mediaType == MediaType.VIDEO) {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(item.contentUri, "video/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
        }

        btnShareMedia.setOnClickListener {
            val item = getCurrentItem() ?: return@setOnClickListener
            val mimeType = if (item.mediaType == MediaType.VIDEO) "video/*" else "image/*"
            val promoText = if (viewModel.isRandomMode.value == true) {
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

    private fun setupToolbarMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu)
                undoMenuItem = menu.findItem(R.id.action_undo)
                sortBySizeMenuItem = menu.findItem(R.id.action_sort_by_size)
                randomMenuItem = menu.findItem(R.id.action_toggle_random)

                // Sincroniza estados
                val action = viewModel.lastAction.value
                undoMenuItem?.isEnabled = action != null
                undoMenuItem?.icon?.alpha = if (action != null) 255 else 77

                val isRandom = viewModel.isRandomMode.value ?: false
                randomMenuItem?.icon?.mutate()?.alpha = if (isRandom) 255 else 120

                val isSort = viewModel.isSortBySizeMode.value ?: false
                sortBySizeMenuItem?.icon?.mutate()?.alpha = if (isSort) 255 else 120
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_undo -> {
                        viewModel.performUndo { _, status ->
                            resetViewPositionsImmediate()
                            if (status == MediaStatus.TRASHED) {
                                (activity as? MainActivity)?.updateTrashBadge()
                            }
                        }
                        true
                    }
                    R.id.action_sort_by_size -> {
                        viewModel.toggleSortBySizeMode()
                        val isSort = viewModel.isSortBySizeMode.value ?: false
                        if (isSort) {
                            showModeBanner("\uD83D\uDCCA", R.string.mode_sort_by_size, R.string.mode_sort_by_size_detail)
                        } else {
                            showModeBanner("\uD83D\uDCC5", R.string.mode_sort_by_date, R.string.mode_sort_by_date_detail)
                        }
                        true
                    }
                    R.id.action_toggle_random -> {
                        viewModel.toggleRandomMode()
                        val isRandom = viewModel.isRandomMode.value ?: false
                        if (isRandom) {
                            showModeBanner("\uD83D\uDD00", R.string.mode_random, R.string.mode_random_detail)
                        } else {
                            showModeBanner("\uD83D\uDCC5", R.string.mode_sequential, R.string.mode_sequential_detail)
                        }
                        true
                    }
                    R.id.action_filter_album -> {
                        showAlbumFilterDialog()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showAlbumFilterDialog() {
        val buckets = viewModel.availableBuckets.value ?: emptyList()
        val currentBucket = viewModel.selectedBucket.value

        val options = mutableListOf<String>()
        options.add(getString(R.string.all_albums))
        options.addAll(buckets)

        val selectedIdx = if (currentBucket == null) 0 else {
            val idx = buckets.indexOf(currentBucket)
            if (idx >= 0) idx + 1 else 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.filter_album)
            .setSingleChoiceItems(options.toTypedArray(), selectedIdx) { dialog, which ->
                val chosenBucket = if (which == 0) null else options[which]
                viewModel.setBucketFilter(chosenBucket)
                dialog.dismiss()

                val title = if (chosenBucket == null) getString(R.string.all_albums) else chosenBucket
                showModeBanner("📁", R.string.filter_album, customSubtitle = title)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showModeBanner(icon: String, titleRes: Int, subtitleRes: Int = 0, customSubtitle: String? = null) {
        bannerRunnable?.let { modeBanner.removeCallbacks(it) }
        modeBanner.animate().cancel()

        tvBannerIcon.text = icon
        tvBannerTitle.text = getString(titleRes)
        tvBannerSubtitle.text = customSubtitle ?: if (subtitleRes != 0) getString(subtitleRes) else ""

        modeBanner.visibility = View.VISIBLE
        modeBanner.alpha = 1f
        modeBanner.translationY = -100f * resources.displayMetrics.density
        modeBanner.animate()
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

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

    @Suppress("ClickableViewAccessibility")
    private fun setupSwipeGesture() {
        mediaContainer.setOnTouchListener { _, event ->
            if (isAnimating || getCurrentItem() == null) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX

                    imageView.translationX = dx
                    imageView.rotation = dx / 50f
                    videoThumbnail.translationX = dx
                    videoThumbnail.rotation = dx / 50f

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
                        animateOut(-1) {
                            viewModel.markCurrentAsDeleted {
                                (activity as? MainActivity)?.updateTrashBadge()
                            }
                        }
                    } else if (dx > SWIPE_THRESHOLD) {
                        animateOut(1) {
                            viewModel.markCurrentAsKept {}
                        }
                    } else {
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
        val width = resources.displayMetrics.widthPixels.toFloat()
        val targetX = if (direction < 0) -width * 1.5f else width * 1.5f

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

    private fun getCurrentItem(): MediaItem? {
        val items = viewModel.mediaItems.value ?: return null
        val idx = viewModel.currentIndex.value ?: 0
        return if (idx in items.indices) items[idx] else null
    }

    private fun showMedia(item: MediaItem) {
        if (item.mediaType == MediaType.VIDEO) {
            imageView.visibility = View.GONE
            videoThumbnail.visibility = View.VISIBLE
            playButton.visibility = View.VISIBLE
            tvVideoBadge.visibility = View.VISIBLE
            tvVideoBadge.text = getString(R.string.video_label)

            Glide.with(this)
                .load(item.contentUri)
                .override(screenWidth, screenHeight)
                .centerInside()
                .placeholder(R.color.background_dark)
                .into(videoThumbnail)
        } else {
            videoThumbnail.visibility = View.GONE
            playButton.visibility = View.GONE

            val isGif = item.displayName.endsWith(".gif", ignoreCase = true)
            if (isGif) {
                tvVideoBadge.visibility = View.VISIBLE
                tvVideoBadge.text = "GIF"
            } else {
                tvVideoBadge.visibility = View.GONE
            }

            imageView.visibility = View.VISIBLE

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

        val sizeStr = Formatter.formatFileSize(requireContext(), item.size)
        tvFileInfo.text = getString(R.string.file_info_format, item.displayName, sizeStr)

        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
        val date = java.util.Date(item.dateAdded * 1000)
        tvMediaDate.text = dateFormat.format(date)

        preloadUpcoming()
    }

    private fun preloadUpcoming() {
        val items = viewModel.mediaItems.value ?: return
        val currentIdx = viewModel.currentIndex.value ?: 0
        if (items.size <= 1) return

        val itemsToPreload = (1..4).mapNotNull { offset ->
            val idx = currentIdx + offset
            if (idx < items.size) items[idx] else null
        }

        for (it in itemsToPreload) {
            Glide.with(this)
                .load(it.contentUri)
                .override(screenWidth, screenHeight)
                .preload()
        }
    }

    private fun updateCounter(current: Int, total: Int) {
        tvCounter.text = getString(R.string.counter_format, current + 1, total)
    }

    fun loadMediaItems() {
        viewModel.loadMedia()
    }

    private fun showAllClean() {
        swipeContent.visibility = View.GONE
        allCleanView.visibility = View.VISIBLE
        isShowingAllClean = true

        val mainActivity = activity as? MainActivity
        if (mainActivity != null && !mainActivity.hasMediaPermissions()) {
            tvAllCleanPermissionHint.visibility = View.VISIBLE
            btnAllCleanGrantPermission.visibility = View.VISIBLE
            if (isPermissionPermanentlyDenied()) {
                btnAllCleanGrantPermission.text = getString(R.string.btn_open_settings)
            } else {
                btnAllCleanGrantPermission.text = getString(R.string.btn_grant_permission)
            }
        } else {
            tvAllCleanPermissionHint.visibility = View.GONE
            btnAllCleanGrantPermission.visibility = View.GONE
        }
    }

    fun reloadIfNeeded() {
        if (isShowingAllClean) {
            isShowingAllClean = false
            viewModel.loadMedia(forceRefresh = true)
        }
    }

    private fun handleGrantPermissionClick() {
        val act = activity ?: return
        val prefs = act.getSharedPreferences(PrefsKeys.PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val alreadyRequested = prefs.getBoolean(PrefsKeys.PERMISSION_REQUESTED, false)
        prefs.edit().putBoolean(PrefsKeys.PERMISSION_REQUESTED, true).apply()

        if (isPermissionPermanentlyDenied() && alreadyRequested) {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", act.packageName, null)
            }
            startActivity(intent)
        } else {
            (act as? MainActivity)?.requestMediaPermissions()
        }
    }

    private fun isPermissionPermanentlyDenied(): Boolean {
        val act = activity ?: return false
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return !ActivityCompat.shouldShowRequestPermissionRationale(act, permission)
    }

    fun showNoPermission() {
        swipeContent.visibility = View.GONE
        allCleanView.visibility = View.GONE
        progressBar.visibility = View.GONE
        noPermissionView.visibility = View.VISIBLE
    }
}
