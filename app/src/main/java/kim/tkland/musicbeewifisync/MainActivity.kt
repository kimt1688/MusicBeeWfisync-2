package kim.tkland.musicbeewifisync

import android.app.Activity
import android.app.ActivityManager.TaskDescription
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuCompat

class MainActivity : WifiSyncBaseActivity() {
    private var syncPreview = false
    private var syncToPlaylists: CheckBox? = null
    private var syncToPlaylistsPath: EditText? = null
    private var syncPreviewButton: Button? = null
    private var syncStartButton: LinearLayout? = null
    private var serverStatusThread: Thread? = null
    private var syncPlayerGoneMad: CheckBox? = null
    private val PERMISSION_READ_EXTERNAL_STORAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val intent: Intent = Intent(this, SyncResultsStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            setContentView(R.layout.activity_main)
            syncPreviewButton = findViewById(R.id.syncPreviewButton)
            syncStartButton = findViewById(R.id.syncStartButton)
            syncToPlaylists = findViewById(R.id.syncToPlaylists)
            syncToPlaylistsPath = findViewById(R.id.syncToPlaylistPath)
            syncPlayerGoneMad = findViewById(R.id.syncPlayerGoneMad)
            val syncFromMusicBee = findViewById<CheckBox>(R.id.syncFromMusicBee)
            syncFromMusicBee.isChecked = WifiSyncServiceSettings.syncFromMusicBee
            syncFromMusicBee.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.syncFromMusicBee = syncFromMusicBee.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            val syncToRatings = findViewById<CheckBox>(R.id.syncToRatings)
            val syncToPlayCounts = findViewById<CheckBox>(R.id.syncToPlayCounts)
            val syncToUsingPlayer = findViewById<RadioGroup>(R.id.syncToUsingPlayer)
            val playlistsSupported = true
            syncToUsingPlayer.check(R.id.syncPlayerGoneMad)
            syncToUsingPlayer.setOnCheckedChangeListener { _, _ ->
                if (syncPlayerGoneMad!!.isChecked) {
                    WifiSyncServiceSettings.reverseSyncPlaylistsPath = "/gmmp/playlists"
                    WifiSyncServiceSettings.reverseSyncPlayer =
                        WifiSyncServiceSettings.PLAYER_GONEMAD
                    setPlaylistsEnabled(true)
                }
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            setPlaylistsEnabled(playlistsSupported)
            syncToPlaylists?.let {
                it.setOnCheckedChangeListener { _, _ ->
                    WifiSyncServiceSettings.reverseSyncPlaylists = syncToPlaylists?.isChecked!!
                    WifiSyncServiceSettings.saveSettings(mainWindow)
                }
            }
            syncToPlaylistsPath?.let {
                it.setOnEditorActionListener { _, _, _ ->
                    WifiSyncServiceSettings.reverseSyncPlaylistsPath =
                        syncToPlaylistsPath?.getText()?.toString()!!
                    WifiSyncServiceSettings.saveSettings(mainWindow)
                    false
                }
            }
            syncToRatings.isChecked = WifiSyncServiceSettings.reverseSyncRatings
            syncToRatings.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.reverseSyncRatings = syncToRatings.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            syncToPlayCounts.isChecked = WifiSyncServiceSettings.reverseSyncPlayCounts
            syncToPlayCounts.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.reverseSyncPlayCounts = syncToPlayCounts.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }

            (WifiSyncServiceSettings.reverseSyncPlayer == WifiSyncServiceSettings.PLAYER_GONEMAD).also { syncPlayerGoneMad!!.isChecked = it }
            checkServerStatus()
        }
        launcher.launch(setLaunchIntent())
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
        val playlistSyncMenuItem = menu.findItem(R.id.playlistSyncMenuItem)
        // playlistSyncMenuItem.isCheckable = false
        // playlistSyncMenuItem.isChecked = false
        return true
    }

    // アクティビティの結果に対するコールバックの登録
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("registerForActivityResult(result)", result.toString())

        if (result.resultCode != RESULT_OK) {
            // アクティビティ結果NG
            return@registerForActivityResult
        } else {
            // アクティビティ結果OK
            try {
                val mUri = result.data?.data
                clearAllPersistedUriPermissions(applicationContext)
                contentResolver.takePersistableUriPermission(
                    mUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        // Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        //FLAG_GRANT_PREFIX_URI_PERMISSION
                )
                val preferences = applicationContext.getSharedPreferences("kim.tkland.musicbeewifisync.sharedpref", Context.MODE_PRIVATE)
                preferences.edit().putString("accesseduri", mUri.toString()).commit()

                requestPermissionForReadWrite(this)
            } catch (e: Exception) {
                Log.d("launcher", e.message!!)
            }
        }
    }

    private fun clearAllPersistedUriPermissions(context: Context) {
        try {
            val contentResolver = context.contentResolver
            for (uriPermission in contentResolver.persistedUriPermissions) {
                applicationContext.contentResolver.releasePersistableUriPermission(
                    /* uri = */       uriPermission.uri,
                    /* modeFlags = */ Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        } catch (e: Throwable) {
            // just to be safe...
            e.printStackTrace()
        }
    }

    private fun requestPermissionForReadWrite(context: Context) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(
                android.Manifest.permission.READ_MEDIA_AUDIO
            ), PERMISSION_READ_EXTERNAL_STORAGE
        )
    }

    private fun setLaunchIntent(): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/xml"
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, "/storage/emulated/0/gmmp")
        }
        return intent
    }

    fun onSyncPreviewButtonClick(view: View) {
        if (isConfigOK) {
            syncPreviewButton!!.isEnabled = false
            try {
                WifiSyncServiceSettings.syncCustomFiles = false
                syncPreview = true
                WifiSyncService.startSynchronisation(this, 0, true, false)
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
        if (syncPlayerGoneMad!!.isChecked) {
            WifiSyncServiceSettings.reverseSyncPlayer = WifiSyncServiceSettings.PLAYER_GONEMAD
        } else WifiSyncServiceSettings.reverseSyncPlayer = 0
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
                else
                    WifiSyncServiceSettings.reverseSyncPlayer = 0
            }
            return if (message == null) {
                true
            } else {
                val builder = AlertDialog.Builder(mainWindow?.applicationContext!!)
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
            } catch (ex: Exception) {
            }
            serverStatusThread = null
        }
        serverStatusThread!!.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }
}
