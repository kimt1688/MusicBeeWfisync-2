package kim.tkland.musicbeewifisync

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.MenuCompat
import androidx.core.net.toUri
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity() : WifiSyncBaseActivity() {
    private var syncPreview = false
    private var syncToPlaylists: CheckBox? = null
    private var syncToPlaylistsPath: EditText? = null
    private var syncPreviewButton: Button? = null
    private var syncStartButton: LinearLayout? = null
    private var serverStatusThread: Thread? = null
    private var syncPlayerGoneMad: RadioButton? = null
    private var syncPlayerPoweramp: RadioButton? = null
    private var syncPlayerNone: RadioButton? = null
    private var syncToPlayCounts: CheckBox? = null
    private var syncToRatings: CheckBox? = null

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun MainActivityScreen(onNavigateToConversation: () -> Unit) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = onNavigateToConversation) {
                    Icon(Icons.Filled.Info, contentDescription = "View Error Log")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_settings)
        ErrorHandler.initialise(this)
        // needed so android "Recent Views" actually shows the icon - only seems to be an issue with P
        @Suppress("DEPRECATION") setTaskDescription(
            TaskDescription(
                null,
                R.drawable.ic_launcher_round
            )
        )

        WifiSyncServiceSettings.loadSettings(this)
        if (WifiSyncServiceSettings.defaultIpAddressValue.isEmpty()) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else if (WifiSyncService.syncIsRunning.get()) {
            val intent = Intent(this, SyncResultsStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            setContent {
               val navController = rememberNavController()
               //CustomView(onNavigateToViewErrorLog = {navController.navigate("ViewErrorLogActivityScreen")})
                //AppCompat {
                    NavHost(navController = navController, startDestination = "CustomView") {
                        composable(route = "CustomView") {
                            CustomView(navController)
                        }
                        composable(route = "ViewErrorLogActivityScreen") { ViewErrorLogActivityScreen(navController) }
                    }
                //}
            }

            checkServerStatus()
        }

        if (!MediaStore.canManageMedia(this)) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_MANAGE_MEDIA)
                    .setData("package:${packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        //setSupportActionBar(findViewById(R.id.my_toolbar))

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView(navController: NavController) {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        var expanded by remember { mutableStateOf(false) }

        Scaffold(
            modifier = Modifier.fillMaxSize().padding(statusBarPadding),

            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    title = {
                        Text(
                            getString(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        Box(modifier = Modifier
                            .padding(16.dp)
                        ) {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View error log...") },
                                    onClick = {
                                        // navController.navigate("ViewErrorLogActivityScreen")
                                        val intent = Intent(applicationContext, ViewErrorLogActivity::class.java)
                                        startActivity(intent)
                                        //navController.navigate("ViewErrorLogActivityScreen")

                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Option 2") },
                                    onClick = { /* Do something... */ }
                                )
                            }
                        }
                    },
                            scrollBehavior = scrollBehavior,
                    )
                }
        ) { innerPadding ->
            AndroidView(
                modifier = Modifier
                    .fillMaxSize() // Fill the available space within the Scaffold
                    //.background(Black)
                    //.windowInsetsPadding(WindowInsets.statusBars),
                    .padding(innerPadding),
                factory = { context ->
                    var rootView =
                        LayoutInflater.from(context).inflate(R.layout.activity_main, null)

                    rootView.apply {
                        // Creates view
                        syncPreviewButton = findViewById(R.id.syncPreviewButton)
                        syncStartButton = findViewById(R.id.syncStartButton)
                        syncToPlaylists = findViewById(R.id.syncToPlaylists)
                        syncToPlaylistsPath = findViewById(R.id.syncToPlaylistPath)
                        syncPlayerGoneMad = findViewById(R.id.reverceFromGoneMAD)
                        syncPlayerPoweramp = findViewById(R.id.reverceFromPoweramp)
                        syncPlayerNone = findViewById(R.id.reverceFromNone)

                        val syncFromMusicBee = findViewById<CheckBox>(R.id.syncFromMusicBee)
                        syncFromMusicBee.isChecked = WifiSyncServiceSettings.syncFromMusicBee
                        syncFromMusicBee.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.syncFromMusicBee = isChecked
                            // Consider how you get 'mainWindow' here. If it's the activity,
                            // you might pass 'context as YourActivityInterface' or use a ViewModel.
                            WifiSyncServiceSettings.saveSettings(this@MainActivity) // Or pass context
                        }

                        syncToRatings = findViewById<CheckBox>(R.id.syncToRatings)
                        syncToPlayCounts = findViewById<CheckBox>(R.id.syncToPlayCounts)
                        val syncToUsingPlayer = findViewById<RadioGroup>(R.id.syncToUsingPlayer)
                        var playlistsSupported = false
                        // syncToUsingPlayer.check(R.id.reverceFromGoneMAD) // This seems to be set below

                        when (WifiSyncServiceSettings.reverseSyncPlayer) {
                            WifiSyncServiceSettings.PLAYER_GONEMAD -> {
                                playlistsSupported = true
                                syncToUsingPlayer.check(R.id.reverceFromGoneMAD)
                            }

                            WifiSyncServiceSettings.PLAYER_POWERAMP -> {
                                playlistsSupported = false
                                syncToUsingPlayer.check(R.id.reverceFromPoweramp)
                            }

                            0 -> syncToUsingPlayer.check(R.id.reverceFromNone)
                        }

                        syncToUsingPlayer.setOnCheckedChangeListener { group, checkedId ->
                            if (syncPlayerGoneMad!!.isChecked) { // Or if (checkedId == R.id.reverceFromGoneMAD)
                                WifiSyncServiceSettings.reverseSyncPlaylistsPath =
                                    "/gmmp/playlists"
                                WifiSyncServiceSettings.reverseSyncPlayer =
                                    WifiSyncServiceSettings.PLAYER_GONEMAD
                                // You'll need a way to call setPlaylistsEnabled from here.
                                // This might involve passing a lambda or having setPlaylistsEnabled
                                // operate on the views directly obtained via findViewById within this factory.
                                // For now, let's assume you'll update the views directly.
                                syncToPlaylists?.isEnabled = true
                                syncToPlaylists?.isChecked =
                                    WifiSyncServiceSettings.reverseSyncPlaylists
                                syncToPlaylistsPath?.isEnabled = true
                                syncToPlaylistsPath?.setText(WifiSyncServiceSettings.reverseSyncPlaylistsPath)
                            }
                            WifiSyncServiceSettings.saveSettings(this@MainActivity) // Or pass context
                        }

                        // Call the logic from your original setPlaylistsEnabled here,
                        // using the view references obtained above.
                        syncToPlaylists?.isEnabled = playlistsSupported
                        syncToPlaylists?.isChecked =
                            if (playlistsSupported) WifiSyncServiceSettings.reverseSyncPlaylists else false
                        syncToPlaylistsPath?.isEnabled = playlistsSupported
                        syncToPlaylistsPath?.setText(WifiSyncServiceSettings.reverseSyncPlaylistsPath)


                        syncToPlaylists?.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.reverseSyncPlaylists = isChecked
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                        }

                        syncToPlaylistsPath?.setOnEditorActionListener { v, actionId, event ->
                            WifiSyncServiceSettings.reverseSyncPlaylistsPath =
                                syncToPlaylistsPath?.text?.toString() ?: ""
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                            false
                        }

                        syncToRatings?.isChecked = WifiSyncServiceSettings.reverseSyncRatings
                        syncToRatings?.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.reverseSyncRatings = isChecked
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                        }

                        syncToPlayCounts?.isChecked =
                            WifiSyncServiceSettings.reverseSyncPlayCounts
                        syncToPlayCounts?.setOnCheckedChangeListener { _, isChecked ->
                            WifiSyncServiceSettings.reverseSyncPlayCounts = isChecked
                            WifiSyncServiceSettings.saveSettings(this@MainActivity)
                        }

                        // You also have click listeners (onSyncPreviewButtonClick, etc.) in your XML.
                        // You can set them here programmatically:
                        syncPreviewButton?.setOnClickListener {
                            onSyncPreviewButtonClick(it) // 'it' is the clicked view
                            rootView = LayoutInflater.from(context)
                                .inflate(R.layout.activity_sync_preview, null)
                        }
                        syncStartButton?.setOnClickListener {
                            // The view passed to onSyncStartButtonClick might not be syncStartButton directly
                            // if it's a LinearLayout with children. You might need to adjust.
                            onSyncStartButtonClick(it)
                        }
                        syncPlayerGoneMad?.setOnClickListener { onGoneMADCheckClick(it) }
                        syncPlayerPoweramp?.setOnClickListener { onPowerampCheckClick(it) }
                        // ... and so on for other click listeners defined in XML


                    }

                    // Return the inflated and configured view
                    rootView
                },
                update = { view ->
                    // Called when the composable recomposes.
                    // You can update the view here if its state needs to change
                    // based on changes in Compose state.
                    // For example, if WifiSyncServiceSettings could change from elsewhere
                    // and you needed to update the checkboxes.
                }
            )
        }
    }

    private fun setPlaylistsEnabled(enabled: Boolean) {
        if (!enabled) {
            WifiSyncServiceSettings.reverseSyncPlaylists = false
        }
        syncToPlaylists!!.isEnabled = enabled
        syncToPlaylists!!.isChecked = WifiSyncServiceSettings.reverseSyncPlaylists
        syncToPlaylistsPath!!.isEnabled = enabled
        syncToPlaylistsPath!!.setText(WifiSyncServiceSettings.reverseSyncPlaylistsPath)
    }

    override fun onDestroy() {
        if (serverStatusThread != null) {
            serverStatusThread!!.interrupt()
            serverStatusThread = null
        }
        mainWindow = null
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        val fullSyncItem = menu.findItem(R.id.fullSyncMenuItem)
        fullSyncItem.isCheckable = true
        fullSyncItem.isChecked = true
        // val playlistSyncMenuItem = menu.findItem(R.id.playlistSyncMenuItem)
        // playlistSyncMenuItem.isCheckable = false
        // playlistSyncMenuItem.isChecked = false
        return true
    }

    fun onSyncPreviewButtonClick(view: View) {
        if (isConfigOK) {
            syncPreviewButton!!.isEnabled = false
            try {
                WifiSyncServiceSettings.syncCustomFiles = false
                syncPreview = true
                WifiSyncService.startSynchronisation(this, 0, true, false)
            }catch (ex:Exception){
                Log.d("onSyncPreviewButtonClick", ex.message!!)
            } finally {
                syncPreviewButton!!.isEnabled = true
            }
        }
    }

    fun onSyncStartButtonClick(view: View) {
        if (isConfigOK) {
            syncStartButton!!.isEnabled = false
            try {
                WifiSyncServiceSettings.syncCustomFiles = false
                syncPreview = false
                WifiSyncService.startSynchronisation(this, 0, false, false)
            }catch (ex:Exception){
                Log.d("onSyncStartButtonClick", ex.message!!)
            } finally {
                syncStartButton!!.isEnabled = true
            }
        }
    }

    fun onGoneMADCheckClick(view: View) {
        WifiSyncServiceSettings.reverseSyncPlayer = WifiSyncServiceSettings.PLAYER_GONEMAD
        syncToPlayCounts!!.isEnabled = true
        syncToRatings!!.isEnabled = true
        setPlaylistsEnabled(true)
    }

    fun onPowerampCheckClick(view: View) {
        WifiSyncServiceSettings.reverseSyncPlayer = WifiSyncServiceSettings.PLAYER_POWERAMP
        syncToPlayCounts!!.isEnabled = true
        syncToRatings!!.isEnabled = true
        setPlaylistsEnabled(false)
    }

    fun onNoneCheckClick(view: View) {
        WifiSyncServiceSettings.reverseSyncPlayer = 0
        syncToPlayCounts!!.isEnabled = false
        syncToRatings!!.isEnabled = false
        setPlaylistsEnabled(false)
    }

    private val isConfigOK: Boolean
        get () {
            var message: String? = null
            if (serverStatusThread != null) {
                message = getString(R.string.errorServerNotFound)
            }
            val anyReverseSync =
                WifiSyncServiceSettings.reverseSyncPlaylists || WifiSyncServiceSettings.reverseSyncRatings || WifiSyncServiceSettings.reverseSyncPlayCounts
            if (!anyReverseSync) {
                if (!WifiSyncServiceSettings.syncFromMusicBee) {
                    message = getString(R.string.errorSyncParamsNoneSelected)
                }
            } else {
                if (syncPlayerGoneMad!!.isChecked)
                    WifiSyncServiceSettings.reverseSyncPlayer = 1
                else if (syncPlayerPoweramp!!.isChecked)
                    WifiSyncServiceSettings.reverseSyncPlayer = 2
                else
                    WifiSyncServiceSettings.reverseSyncPlayer = 0
            }
            return if (message == null) {
                true
            } else {
                val builder = AlertDialog.Builder(mainWindow!!)
                builder.setTitle(getString(R.string.syncErrorHeader))
                builder.setMessage(message)
                builder.setIcon(android.R.drawable.ic_dialog_alert)
                builder.setCancelable(false)
                builder.setPositiveButton(android.R.string.ok, null)
                builder.show()
                false
            }
        }

        private fun checkServerStatus() {
        serverStatusThread = Thread {
            try {
                var statusDisplayed = false
                while (true) {
                    if (WifiSyncService.ServerPinger.ping()) {
                        if (statusDisplayed) {
                            runOnUiThread {
                                if (serverStatusThread != null) {
                                    serverStatusThread = null
                                    findViewById<View>(R.id.syncServerStatus).visibility = View.GONE
                                }
                            }
                        }
                        break
                    }
                    if (!statusDisplayed) {
                        runOnUiThread {
                            findViewById<View>(R.id.syncServerStatus).visibility = View.VISIBLE
                        }
                        statusDisplayed = true
                    }
                    Thread.sleep(2500)
                }
            } catch (_: Exception) {
            }
            serverStatusThread = null
        }
        serverStatusThread!!.start()
    }
}
