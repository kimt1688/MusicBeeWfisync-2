package kim.tkland.musicbeewifisync

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import android.widget.TextView.OnEditorActionListener
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuCompat

class MainActivity : WifiSyncBaseActivity() {
    private var syncPreview = false
    private var syncToPlaylists: CheckBox? = null
    private var syncToPlaylistsPath: EditText? = null
    private var syncPreviewButton: Button? = null
    private var syncStartButton: LinearLayout? = null
    private var serverStatusThread: Thread? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ErrorHandler.initialise(this)
        // needed so android "Recent Views" actually shows the icon - only seems to be an issue with P
        @Suppress("DEPRECATION")setTaskDescription(TaskDescription(null,
            R.drawable.ic_launcher_round
        ))

        WifiSyncServiceSettings.loadSettings(this)
        if (WifiSyncServiceSettings.defaultIpAddressValue.length == 0) {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else if (WifiSyncService.syncIsRunning.get()) {
            val intent: Intent
            intent = Intent(this, SyncResultsStatusActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        } else {
            setContentView(R.layout.activity_main)
            // PermissionsHandler.demandInternalStorageAccessPermissions(this)
            syncPreviewButton = findViewById(R.id.syncPreviewButton)
            syncStartButton = findViewById(R.id.syncStartButton)
            syncToPlaylists = findViewById(R.id.syncToPlaylists)
            syncToPlaylistsPath = findViewById(R.id.syncToPlaylistPath)
            val syncFromMusicBee = findViewById<CheckBox>(R.id.syncFromMusicBee)
            syncFromMusicBee.isChecked = WifiSyncServiceSettings.syncFromMusicBee
            syncFromMusicBee.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.syncFromMusicBee = syncFromMusicBee.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            val syncToRatings = findViewById<CheckBox>(R.id.syncToRatings)
            val syncToPlayCounts = findViewById<CheckBox>(R.id.syncToPlayCounts)
            val syncToUsingPlayer = findViewById<RadioGroup>(R.id.syncToUsingPlayer)
            var playlistsSupported = false
            when (WifiSyncServiceSettings.reverseSyncPlayer) {
                WifiSyncServiceSettings.PLAYER_GONEMAD -> {
                    playlistsSupported = true
                    syncToUsingPlayer.check(R.id.syncPlayerGoneMad)
                }
                WifiSyncServiceSettings.PLAYER_POWERAMP -> syncToUsingPlayer.check(R.id.syncPlayerPowerAmp)
            }
            syncToUsingPlayer.setOnCheckedChangeListener { _, _ ->
                when (syncToUsingPlayer.checkedRadioButtonId) {
                    R.id.syncPlayerGoneMad -> {
                        WifiSyncServiceSettings.reverseSyncPlaylistsPath = "/gmmp/playlists"
                        WifiSyncServiceSettings.reverseSyncPlayer =
                            WifiSyncServiceSettings.PLAYER_GONEMAD
                        setPlaylistsEnabled(true)
                    }
                    R.id.syncPlayerPowerAmp -> {
                        WifiSyncServiceSettings.reverseSyncPlaylistsPath = ""
                        WifiSyncServiceSettings.reverseSyncPlayer =
                            WifiSyncServiceSettings.PLAYER_POWERAMP
                        setPlaylistsEnabled(false)
                    }
                }
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            setPlaylistsEnabled(playlistsSupported)
            syncToPlaylists?.let{it.setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.reverseSyncPlaylists = syncToPlaylists?.let{it.isChecked()}!!
                WifiSyncServiceSettings.saveSettings(mainWindow)
            })}
            syncToPlaylistsPath?.let{it.setOnEditorActionListener(OnEditorActionListener { _, _, _ ->
                WifiSyncServiceSettings.reverseSyncPlaylistsPath =
                    syncToPlaylistsPath?.let{it.getText().toString()}!!
                WifiSyncServiceSettings.saveSettings(mainWindow)
                false
            })}
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
            checkServerStatus()
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
        val playlistSyncMenuItem = menu.findItem(R.id.playlistSyncMenuItem)
        playlistSyncMenuItem.isCheckable = false
        playlistSyncMenuItem.isChecked = false
        return true
    }

    fun onSyncPreviewButton_Click(view: View) {
        if (isConfigOK) {
            syncPreviewButton!!.isEnabled = false
            try {
                WifiSyncServiceSettings.syncCustomFiles = false
                syncPreview = true
                //if (tryGetStorageAccessGrant()) {
                WifiSyncService.startSynchronisation(this, 0, true, false)
                //}
            } finally {
                syncPreviewButton!!.isEnabled = true
            }
        }
    }

    fun onSyncStartButton_Click(view: View) {
        if (isConfigOK) {
            syncStartButton!!.isEnabled = false
            try {
                WifiSyncServiceSettings.syncCustomFiles = false
                syncPreview = false
                //if (tryGetStorageAccessGrant()) {
                WifiSyncService.startSynchronisation(this, 0, false, false)
                //}
            } finally {
                syncStartButton!!.isEnabled = true
            }
        }
    }

    @Suppress("REDUNDANT_MODIFIER_IN_GETTER")
    private val isConfigOK: Boolean
        private get() {
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
                if (WifiSyncServiceSettings.reverseSyncPlayer == 0) {
                    message = getString(R.string.errorSyncParamsPlayerNotSelected)
                }
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