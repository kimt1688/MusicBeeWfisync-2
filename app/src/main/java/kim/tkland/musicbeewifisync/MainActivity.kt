package kim.tkland.musicbeewifisync

import android.app.ActivityManager.TaskDescription
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuCompat
import androidx.core.net.toUri

class MainActivity : WifiSyncBaseActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
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
            setContentView(R.layout.activity_main)
            syncPreviewButton = findViewById(R.id.syncPreviewButton)
            syncStartButton = findViewById(R.id.syncStartButton)
            syncToPlaylists = findViewById(R.id.syncToPlaylists)
            syncToPlaylistsPath = findViewById(R.id.syncToPlaylistPath)
            syncPlayerGoneMad = findViewById(R.id.reverceFromGoneMAD)
            syncPlayerPoweramp = findViewById(R.id.reverceFromPoweramp)
            syncPlayerNone = findViewById(R.id.reverceFromNone)
            val syncFromMusicBee = findViewById<CheckBox>(R.id.syncFromMusicBee)
            syncFromMusicBee.isChecked = WifiSyncServiceSettings.syncFromMusicBee
            syncFromMusicBee.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.syncFromMusicBee = syncFromMusicBee.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            syncToRatings = findViewById<CheckBox>(R.id.syncToRatings)
            syncToPlayCounts = findViewById<CheckBox>(R.id.syncToPlayCounts)
            val syncToUsingPlayer = findViewById<RadioGroup>(R.id.syncToUsingPlayer)
            var playlistsSupported = false
            syncToUsingPlayer.check(R.id.reverceFromGoneMAD)
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
            syncToRatings?.isChecked = WifiSyncServiceSettings.reverseSyncRatings
            syncToRatings?.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.reverseSyncRatings = syncToRatings!!.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
            }
            syncToPlayCounts?.isChecked = WifiSyncServiceSettings.reverseSyncPlayCounts
            syncToPlayCounts?.setOnCheckedChangeListener { _, _ ->
                WifiSyncServiceSettings.reverseSyncPlayCounts = syncToPlayCounts!!.isChecked
                WifiSyncServiceSettings.saveSettings(mainWindow)
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
        setSupportActionBar(findViewById(R.id.my_toolbar))
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
