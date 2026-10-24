package com.lagando.deslize

import android.annotation.SuppressLint

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.format.Formatter
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lagando.deslize.data.AppDatabase
import com.lagando.deslize.worker.TrashCleanupWorker
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.net.HttpURLConnection

/**
 * Main Activity for Deslize pra Limpar.
 * Manages the navigation drawer, fragment navigation, and permissions.
 * Sets up periodic trash cleanup via WorkManager.
 * Shows a notification badge on the trash menu item when items exist.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var db: AppDatabase

    // Trash badge view
    private var tvBadgeCount: TextView? = null
    // Hamburger badge view (toolbar notification dot)
    private var tvHamburgerBadge: TextView? = null

    // Current fragment tag
    private var currentFragment = TAG_SWIPE

    // Permission launcher
    @SuppressLint("InlinedApi")
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val hasPartialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                permissions[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true

        // Filter out POST_NOTIFICATIONS when checking if media permissions are granted,
        // because we don't want to block the app just because notifications were denied.
        val mediaPermissions = permissions.filterKeys { it != Manifest.permission.POST_NOTIFICATIONS }
        val allMediaGranted = mediaPermissions.values.all { it } || hasPartialAccess

        if (allMediaGranted) {
            showSwipeFragment()
        } else {
            val fragment = supportFragmentManager.findFragmentByTag(TAG_SWIPE)
            (fragment as? SwipeFragment)?.showNoPermission()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)

        setupToolbar()
        setupDrawer()
        setupNavigation()
        setupTrashCleanupWorker()
        setupEdgeToEdge()
        setupBackNavigation()

        if (savedInstanceState == null) {
            checkPermissionsAndLoad()
            showTutorialIfFirstLaunch()
        }
    }

    override fun onResume() {
        super.onResume()
        updateTrashBadge()
        updateSpaceCounter()

        // When returning from Settings or after granting permission,
        // reload media if we now have access
        if (currentFragment == TAG_SWIPE && hasMediaPermissions()) {
            val fragment = supportFragmentManager.findFragmentByTag(TAG_SWIPE)
            (fragment as? SwipeFragment)?.reloadIfNeeded()
        }

        showTrashReminderIfNeeded()
    }

    private fun setupToolbar() {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        tvHamburgerBadge = findViewById(R.id.tvHamburgerBadge)
    }

    private fun setupDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Update badge whenever drawer opens/closes
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                updateTrashBadge()
                updateSpaceCounter()
                // Hide hamburger badge when drawer is open (trash badge is visible directly)
                tvHamburgerBadge?.visibility = View.GONE
            }
            override fun onDrawerClosed(drawerView: View) {
                // Restore hamburger badge when drawer closes
                updateTrashBadge()
            }
        })
    }

    private fun setupNavigation() {
        navigationView = findViewById(R.id.navigationView)

        // Setup trash badge action view
        val trashMenuItem = navigationView.menu.findItem(R.id.nav_trash)
        trashMenuItem.setActionView(R.layout.menu_badge)
        tvBadgeCount = trashMenuItem.actionView?.findViewById(R.id.tvBadgeCount)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    if (currentFragment != TAG_SWIPE) {
                        showSwipeFragment()
                    }
                }
                R.id.nav_kept -> {
                    if (currentFragment != TAG_KEPT) {
                        showKeptFragment()
                    }
                }
                R.id.nav_trash -> {
                    if (currentFragment != TAG_TRASH) {
                        showTrashFragment()
                    }
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
        // Set home as default selected
        navigationView.setCheckedItem(R.id.nav_home)

        // Add footer with "feito por lagando" and website link
        val footerView = layoutInflater.inflate(R.layout.nav_footer, navigationView, false)
        navigationView.addView(footerView)

        // Position footer at the bottom of the drawer
        val params = footerView.layoutParams as android.widget.FrameLayout.LayoutParams
        params.gravity = android.view.Gravity.BOTTOM
        footerView.layoutParams = params

        // Make the website link clickable
        footerView.findViewById<View>(R.id.tvWebsiteLink)?.setOnClickListener {
            val url = getString(R.string.website_url)
            startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        }

        // Fetch and display remote message from GitHub
        val tvRemoteMessage = footerView.findViewById<TextView>(R.id.tvRemoteMessage)
        fetchRemoteMessage(tvRemoteMessage)

        updateSpaceCounter()
    }

    /**
     * Fetches a message from a remote URL and displays it in the given TextView.
     * Runs on IO dispatcher; updates UI on Main. Silently fails if network is unavailable.
     */
    private fun fetchRemoteMessage(textView: TextView?) {
        if (textView == null) return
        lifecycleScope.launch {
            try {
                val message = withContext(Dispatchers.IO) {
                    val connection = java.net.URL(REMOTE_MESSAGE_URL)
                        .openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    try {
                        connection.inputStream.bufferedReader().readText().trim()
                    } finally {
                        connection.disconnect()
                    }
                }
                if (message.isNotEmpty()) {
                    textView.text = message
                    textView.visibility = View.VISIBLE
                }
            } catch (_: Exception) {
                // Silently ignore — no network or URL issue
            }
        }
    }

    /**
     * Updates the red notification badge on the Lixeira menu item.
     * Shows the count of items in trash; hides badge when trash is empty.
     */
    fun updateTrashBadge() {
        lifecycleScope.launch(Dispatchers.IO) {
            val count = db.mediaDao().getTrashCount()
            withContext(Dispatchers.Main) {
                val badgeText = if (count > 99) getString(R.string.badge_overflow) else count.toString()
                if (count > 0) {
                    tvBadgeCount?.visibility = View.VISIBLE
                    tvBadgeCount?.text = badgeText
                    // Also show badge on hamburger icon (only if drawer is closed)
                    if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        tvHamburgerBadge?.visibility = View.VISIBLE
                        tvHamburgerBadge?.text = badgeText
                    }
                } else {
                    tvBadgeCount?.visibility = View.GONE
                    tvHamburgerBadge?.visibility = View.GONE
                }
            }
        }
        updateSpaceCounter()
    }

    /**
     * Updates the space counter in the navigation drawer.
     * Shows total size of items in trash that can be freed.
     */
    private fun updateSpaceCounter() {
        lifecycleScope.launch(Dispatchers.IO) {
            val totalSize = db.mediaDao().getTotalTrashSize()
            val prefs = getSharedPreferences("deslize_prefs", MODE_PRIVATE)
            val totalFreed = prefs.getLong("total_space_freed", 0L)
            withContext(Dispatchers.Main) {
                val spaceItem = navigationView.menu.findItem(R.id.nav_space_counter)
                if (totalSize > 0) {
                    val sizeStr = Formatter.formatFileSize(this@MainActivity, totalSize)
                    spaceItem?.title = getString(R.string.space_counter_format, sizeStr)
                } else {
                    spaceItem?.title = getString(R.string.space_counter_empty)
                }
                
                val freedItem = navigationView.menu.findItem(R.id.nav_space_freed)
                if (totalFreed > 0) {
                    val freedStr = Formatter.formatFileSize(this@MainActivity, totalFreed)
                    freedItem?.title = getString(R.string.space_freed_format, freedStr)
                    freedItem?.isVisible = true
                } else {
                    freedItem?.title = getString(R.string.space_freed_empty)
                    freedItem?.isVisible = true
                }
            }
        }
    }

    /**
     * Shows a dialog reminder when there are items in the trash.
     * Appears every time the app is opened on the swipe screen.
     * Tapping the action button navigates to the TrashFragment.
     */
    private fun showTrashReminderIfNeeded() {
        if (currentFragment != TAG_SWIPE) return

        lifecycleScope.launch(Dispatchers.IO) {
            val count = db.mediaDao().getTrashCount()
            if (count > 0) {
                val totalSize = db.mediaDao().getTotalTrashSize()
                withContext(Dispatchers.Main) {
                    val message = resources.getQuantityString(
                        R.plurals.trash_reminder_message, count, count
                    )
                    val sizeStr = Formatter.formatFileSize(this@MainActivity, totalSize)
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle(R.string.trash_reminder_title)
                        .setMessage("$message\n${getString(R.string.trash_reminder_size, sizeStr)}")
                        .setPositiveButton(R.string.trash_reminder_action) { _, _ ->
                            showTrashFragment()
                            navigationView.setCheckedItem(R.id.nav_trash)
                        }
                        .setNegativeButton(R.string.trash_reminder_dismiss, null)
                        .show()
                }
            }
        }
    }

    private fun setupTrashCleanupWorker() {
        val cleanupRequest = PeriodicWorkRequestBuilder<TrashCleanupWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "trash_cleanup",
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupRequest
        )
    }

    private fun checkPermissionsAndLoad() {
        if (hasMediaPermissions()) {
            showSwipeFragment()
        } else {
            // Show the swipe fragment first (it will show the no-permission view)
            showSwipeFragment()
            requestMediaPermissions()
        }
    }

    fun requestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            // Android 12 and below
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    fun hasMediaPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Android 14+ allows partial access
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
                    PackageManager.PERMISSION_GRANTED ||
            (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED &&
             ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showSwipeFragment() {
        currentFragment = TAG_SWIPE
        toolbar.title = getString(R.string.app_name)
        val fragment = SwipeFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, TAG_SWIPE)
            .commit()

        // If we don't have permissions yet, show the no-permission view after a short delay
        if (!hasMediaPermissions()) {
            toolbar.postDelayed({
                (supportFragmentManager.findFragmentByTag(TAG_SWIPE) as? SwipeFragment)
                    ?.showNoPermission()
            }, 100)
        }
    }

    private fun showKeptFragment() {
        currentFragment = TAG_KEPT
        toolbar.title = getString(R.string.menu_kept)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, KeptFragment(), TAG_KEPT)
            .commit()
    }

    private fun showTrashFragment() {
        currentFragment = TAG_TRASH
        toolbar.title = getString(R.string.menu_trash)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, TrashFragment(), TAG_TRASH)
            .commit()
    }

    /**
     * Handles edge-to-edge display for API 35+ (Android 15+).
     * Applies system bar insets as padding so UI doesn't overlap status/navigation bars.
     */
    private fun setupEdgeToEdge() {
        val coordinatorLayout = findViewById<View>(R.id.fragmentContainer).parent as View
        ViewCompat.setOnApplyWindowInsetsListener(coordinatorLayout) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(left = insets.left, right = insets.right, bottom = insets.bottom)
            windowInsets
        }
        val appBarLayout = findViewById<View>(R.id.appBarLayout)
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.updatePadding(top = insets.top)
            windowInsets
        }
    }

    /**
     * Modern back navigation using OnBackPressedCallback.
     * Replaces deprecated onBackPressed() for Predictive Back support (Android 14+).
     */
    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else if (currentFragment != TAG_SWIPE) {
                    showSwipeFragment()
                    navigationView.setCheckedItem(R.id.nav_home)
                } else {
                    // Let the system handle back (exit app)
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    /**
     * Shows a tutorial dialog on first app launch.
     * Uses SharedPreferences to track whether the tutorial has been shown.
     */
    private fun showTutorialIfFirstLaunch() {
        val prefs = getSharedPreferences("deslize_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("tutorial_shown", false)) return

        AlertDialog.Builder(this)
            .setTitle(R.string.tutorial_title)
            .setMessage(R.string.tutorial_message)
            .setPositiveButton(R.string.tutorial_ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()

        prefs.edit { putBoolean("tutorial_shown", true) }
    }

    companion object {
        private const val TAG_SWIPE = "swipe"
        private const val TAG_KEPT = "kept"
        private const val TAG_TRASH = "trash"
        private const val REMOTE_MESSAGE_URL = "https://raw.githubusercontent.com/lagando/mascotes/refs/heads/main/msgleitor.txt"
    }
}
