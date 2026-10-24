package com.lagando.deslize

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.core.view.isVisible
import androidx.core.net.toUri
import com.bumptech.glide.Glide

/**
 * Full-screen photo/video viewer activity.
 * Opens media in a large view with options to share or open in an external app.
 * Receives content URI and media type via intent extras.
 */
class PhotoViewerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MEDIA_URI = "extra_media_uri"
        const val EXTRA_MEDIA_TYPE = "extra_media_type"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_viewer)

        // Immersive mode — replaces deprecated android:windowFullscreen
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val mediaUriString = intent.getStringExtra(EXTRA_MEDIA_URI) ?: run {
            finish()
            return
        }
        val mediaUri = mediaUriString.toUri()
        val mediaType = intent.getStringExtra(EXTRA_MEDIA_TYPE) ?: "image"
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val ivFullImage: ImageView = findViewById(R.id.ivFullImage)
        val ivVideoThumb: ImageView = findViewById(R.id.ivVideoThumb)
        val ivPlayButton: ImageView = findViewById(R.id.ivPlayButton)
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        val btnShare: ImageButton = findViewById(R.id.btnShare)
        val btnOpenWith: ImageButton = findViewById(R.id.btnOpenWith)
        val tvFileName: TextView = findViewById(R.id.tvFileName)

        // Show filename at the bottom
        tvFileName.text = displayName

        if (mediaType == "video") {
            // For videos: show thumbnail + play button
            ivFullImage.visibility = View.GONE
            ivVideoThumb.visibility = View.VISIBLE
            ivPlayButton.visibility = View.VISIBLE

            Glide.with(this)
                .load(mediaUri)
                .override(screenWidth, screenHeight)
                .fitCenter()
                .into(ivVideoThumb)

            // Tap play to open video in external player
            ivPlayButton.setOnClickListener {
                openWithExternalApp(mediaUri, "video/*")
            }
        } else {
            // For images: show full image
            ivFullImage.visibility = View.VISIBLE
            ivVideoThumb.visibility = View.GONE
            ivPlayButton.visibility = View.GONE

            Glide.with(this)
                .load(mediaUri)
                .thumbnail(
                    Glide.with(this)
                        .load(mediaUri)
                        .override(screenWidth / 10, screenHeight / 10)
                )
                .override(screenWidth, screenHeight)
                .fitCenter()
                .into(ivFullImage)
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Share button - opens Android share sheet
        btnShare.setOnClickListener {
            val mimeType = if (mediaType == "video") "video/*" else "image/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, mediaUri)
                putExtra(Intent.EXTRA_TEXT, getString(R.string.share_promo_text))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share)))
        }

        // Open with button - opens in external gallery/viewer app
        btnOpenWith.setOnClickListener {
            val mimeType = if (mediaType == "video") "video/*" else "image/*"
            openWithExternalApp(mediaUri, mimeType)
        }

        // Tap on the image to toggle top/bottom bar visibility
        val topBar: View = findViewById(R.id.topBar)

        // Apply system bar insets so controls aren't hidden behind status/nav bars
        // when the user swipes to reveal them in immersive mode
        ViewCompat.setOnApplyWindowInsetsListener(topBar) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = insets.top)
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(tvFileName) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        val clickTarget = if (mediaType == "video") ivVideoThumb else ivFullImage
        clickTarget.setOnClickListener {
            val newVisibility = if (topBar.isVisible) View.GONE else View.VISIBLE
            topBar.visibility = newVisibility
            tvFileName.visibility = newVisibility
        }
    }

    private fun openWithExternalApp(uri: Uri, mimeType: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(viewIntent)
        } catch (e: Exception) {
            // No app available to handle this intent
        }
    }
}
