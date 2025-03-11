package kim.tkland.musicbeewifisync

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.MenuCompat
import java.net.SocketTimeoutException

class PlaylistSyncActivity : WifiSyncBaseActivity() {
    private var syncPreview = false
    private var playlistLoaderThread: Thread? = null
    private var syncPlaylistsDeleteFiles: CheckBox? = null
    private var syncPlaylistsSelector: ListView? = null
    private var syncNoPlaylistsMessage: TextView? = null
    private var syncPlaylistSelectorAdapter: ArrayAdapter<FileSelectedInfo>? = null
    private var syncPlaylistsCountMessage: TextView? = null
    private var syncPlaylistsPreviewButton: Button? = null
    private var syncPlaylistsStartButton: LinearLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_sync)
        syncPlaylistsDeleteFiles = findViewById(R.id.syncPlaylistsDeleteFiles)
        syncPlaylistsSelector = findViewById(R.id.syncPlaylistsSelector)
        syncNoPlaylistsMessage = findViewById(R.id.syncNoPlaylistsMessage)
        syncPlaylistsCountMessage = findViewById(R.id.syncPlaylistsCountMessage)
        syncPlaylistsPreviewButton = findViewById(R.id.syncPlaylistsPreviewButton)
        syncPlaylistsStartButton = findViewById(R.id.syncPlaylistsStartButton)
        syncPlaylistsDeleteFiles?.setChecked(WifiSyncServiceSettings.syncDeleteUnselectedFiles)
        if (selectedPlaylists == null) {
            loadPlaylists()
        } else {
            showPlaylists()
        }
        syncPlaylistsDeleteFiles?.setChecked(WifiSyncServiceSettings.syncDeleteUnselectedFiles)
    }

    override fun onDestroy() {
        if (playlistLoaderThread != null) {
            playlistLoaderThread!!.interrupt()
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        MenuCompat.setGroupDividerEnabled(menu, true)
        val fullSyncItem = menu.findItem(R.id.fullSyncMenuItem)
        fullSyncItem.isCheckable = false
        fullSyncItem.isChecked = false
        val playlistSyncMenuItem = menu.findItem(R.id.playlistSyncMenuItem)
        playlistSyncMenuItem.isCheckable = true
        playlistSyncMenuItem.isChecked = true
        return true
    }

    fun onSyncPlaylistsPreviewButton_Click(view: View) {
        syncPlaylistsPreviewButton!!.isEnabled = false
        try {
            if (setSyncParameters()) {
                syncPreview = true
                WifiSyncService.startSynchronisation(this, 0, true, false)
            }
        } finally {
            syncPlaylistsPreviewButton!!.isEnabled = true
        }
    }

    fun onSyncPlaylistsStartButton_Click(view: View) {
        syncPlaylistsStartButton!!.isEnabled = false
        try {
            if (setSyncParameters()) {
                syncPreview = false
                WifiSyncService.startSynchronisation(this, 0, false, false)
            }
        } finally {
            syncPlaylistsStartButton!!.isEnabled = true
        }
    }

    private fun setSyncParameters(): Boolean {
        WifiSyncServiceSettings.syncCustomFiles = true
        WifiSyncServiceSettings.syncDeleteUnselectedFiles = syncPlaylistsDeleteFiles!!.isChecked
        if (selectedPlaylists != null) {
            WifiSyncServiceSettings.syncCustomPlaylistNames.clear()
            for (info in selectedPlaylists!!) {
                if (info.checked) {
                    WifiSyncServiceSettings.syncCustomPlaylistNames.add(info.filename)
                }
            }
        }
        return if (WifiSyncServiceSettings.syncCustomPlaylistNames.size > 0) {
            WifiSyncServiceSettings.saveSettings(this)
            true
        } else {
            val builder = AlertDialog.Builder(mainWindow)
            builder.setTitle(getString(R.string.syncErrorHeader))
            builder.setMessage(getString(R.string.errorNoPlaylistsSelected))
            builder.setIcon(android.R.drawable.ic_dialog_alert)
            builder.setPositiveButton(android.R.string.ok, null)
            builder.show()
            false
        }
    }

    private fun loadPlaylists() {
        playlistLoaderThread = Thread(Runnable {
            try {
                while (true) {
                    try {
                        val values = ArrayList<FileSelectedInfo>()
                        val lookup = CaseInsensitiveMap()
                        for (playlistName in WifiSyncServiceSettings.syncCustomPlaylistNames) {
                            lookup[playlistName] = null
                        }
                        for (playlistName in WifiSyncService.musicBeePlaylists) {
                            values.add(
                                FileSelectedInfo(
                                    playlistName,
                                    lookup.containsKey(playlistName)
                                )
                            )
                        }
                        selectedPlaylists = values
                        runOnUiThread {
                            if (!playlistLoaderThread!!.isInterrupted) {
                                showPlaylists()
                            }
                        }
                        return@Runnable
                    } catch (ex: InterruptedException) {
                        throw ex
                    } catch (ex: SocketTimeoutException) {
                        showPlaylistRetrievalError()
                        Thread.sleep(2500)
                    } catch (ex: Exception) {
                        ErrorHandler.logError("loadPlaylists", ex)
                        showPlaylistRetrievalError()
                        return@Runnable
                    }
                }
            } catch (ex: Exception) {
            }
        })
        playlistLoaderThread!!.start()
    }

    private fun showPlaylists() {
        if (mainWindow != null) {
            try {
                syncNoPlaylistsMessage!!.visibility = View.GONE
                showPlaylistsSelectedCount()
                syncPlaylistSelectorAdapter = object : ArrayAdapter<FileSelectedInfo>(
                    mainWindow!!,
                    R.layout.row_item_sync_playlist_selector,
                    R.id.syncFileSelectorName,
                    selectedPlaylists!!
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        val info = selectedPlaylists!![position]
                        val filename = view.findViewById<CheckBox>(R.id.syncFileSelectorName)
                        filename.isEnabled = syncPlaylistsSelector!!.isEnabled
                        filename.setOnClickListener {
                            info.checked = !info.checked
                            filename.isChecked = info.checked
                            showPlaylistsSelectedCount()
                        }
                        filename.text = info.filename
                        filename.isChecked = info.checked
                        return view
                    }
                }
                syncPlaylistsSelector!!.adapter = syncPlaylistSelectorAdapter
            } catch (ex: Exception) {
                ErrorHandler.logError("showPlaylists", ex)
            }
        }
    }

    private fun showPlaylistsSelectedCount() {
        var count = 0
        if (selectedPlaylists != null) {
            for (info in selectedPlaylists!!) {
                if (info.checked) {
                    count++
                }
            }
        }
        when (count) {
            0 -> syncPlaylistsCountMessage!!.setText(R.string.syncPlaylists0)
            1 -> syncPlaylistsCountMessage!!.setText(R.string.syncPlaylists1)
            else -> syncPlaylistsCountMessage!!.text =
                String.format(getString(R.string.syncPlaylistsN), count)
        }
    }

    private fun showPlaylistRetrievalError() {
        runOnUiThread { syncNoPlaylistsMessage!!.visibility = View.VISIBLE }
    }

    private inner class FileSelectedInfo internal constructor(
        val filename: String,
        var checked: Boolean
    )

    companion object {
        @Volatile
        private var selectedPlaylists: ArrayList<FileSelectedInfo>? = null
    }
}