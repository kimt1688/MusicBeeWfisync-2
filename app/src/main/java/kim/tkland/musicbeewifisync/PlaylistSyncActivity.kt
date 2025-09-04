package kim.tkland.musicbeewifisync

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.MenuCompat
import kim.tkland.musicbeewifisync.MainActivity.RadioOption
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        syncPlaylistsDeleteFiles?.setChecked(WifiSyncServiceSettings.syncDeleteUnselectedFiles)
        //setSupportActionBar(findViewById(R.id.my_toolbar))
        setContent {
            CustomView()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun CustomView() {
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        var isSyncPlaylistsDeleteFiles by remember { mutableStateOf(WifiSyncServiceSettings.syncDeleteUnselectedFiles) }
        var checkCount by remember { mutableStateOf(getString(R.string.syncPlaylists0)) }
        val isListChecked = remember { mutableStateListOf<Boolean>(false) }

        if (selectedPlaylists == null) {
            loadPlaylists(isListChecked, onIsListCheckChange = {isChecked -> isListChecked.add(isChecked)}, checkCount, onCheckCountChange = {checkCount = it})
        } else {
            showPlaylists(checkCount, onCheckCountChange = {checkCount = it})
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),

            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(75.dp),
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(getColor(R.color.colorButtonBackground)),
                        titleContentColor = Color(getColor(R.color.colorButtonTextEnabled))
                    ),
                    title = {
                        Box( // Wrap the Text in a Box
                            modifier = Modifier.fillMaxHeight(), // Fill the available height in the title slot
                            contentAlignment = Alignment.BottomCenter // Align content to the bottom start
                        ) {
                            Text(
                                text = getString(R.string.title_activity_sync_playlists),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                        ) {
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuWifiFullSync)) },
                                    onClick = {
                                        val intent = Intent(
                                            applicationContext,
                                            MainActivity::class.java
                                        )
                                        expanded = false
                                        startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(horizontalAlignment = Alignment.Start){
                                                Text(getString(R.string.menuWifiPlaylistSync))}
                                            Column(horizontalAlignment = Alignment.End) {
                                                Checkbox(
                                                    checked = true,
                                                    colors = CheckboxDefaults.colors(
                                                        checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                                        checkedColor = Color(getColor(R.color.colorButtonBackground)),
                                                    ),
                                                    onCheckedChange = { isChecked ->
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        val intent = Intent(
                                            applicationContext,
                                            PlaylistSyncActivity::class.java
                                        )
                                        expanded = false
                                        startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuSyncSettings)) },
                                    onClick = {
                                        val intent = Intent(
                                            applicationContext,
                                            SettingsActivity::class.java
                                        )
                                        expanded = false
                                        startActivity(intent)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuFullScanFiles)) },
                                    onClick = {
                                        expanded = false
                                        onFullScanMenuItemClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuAllPlaylistsDelete)) },
                                    onClick = {
                                        expanded = false
                                        onDeleteAllPlaylistsClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuWifiSyncLog)) },
                                    onClick = {
                                        val intent = Intent(
                                            applicationContext,
                                            ViewErrorLogActivity::class.java
                                        )
                                        expanded = false
                                        startActivity(intent)
                                    }
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                Row( // Or a Compose Row
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(R.color.colorButtonTextEnabled)), // 枠線の色
                            ),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            try {
                                //syncStartButton!!.isEnabled = false
                                WifiSyncServiceSettings.syncCustomFiles = false
                                // 画面情報を保存する
                                //WifiSyncServiceSettings.reverseSyncPlayer = reverseSyncPrayerSelected

                                //syncPreview = false
                                WifiSyncService.startSynchronisation(
                                    applicationContext,
                                    0,
                                    true,
                                    false
                                )
                            } catch (ex: Exception) {
                                Log.d("onPreviewButtonClick", ex.message!!)
                            } finally {
                                // syncStartButton!!.isEnabled = true
                            }
                        }) {
                        Modifier.weight(1f)

                        //Icon(
                        //imageVector = Icons.Filled.Sync,
                        //contentDescription = "Sync",
                        //)
                        //Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Preview", fontSize = 24.sp)
                    }
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(R.color.colorButtonTextEnabled)), // 枠線の色
                            ),
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            try {
                                WifiSyncServiceSettings.syncCustomFiles = false
                                //syncPreview = false
                                WifiSyncService.startSynchronisation(
                                    applicationContext,
                                    0,
                                    false,
                                    false
                                )
                            } catch (ex: Exception) {
                                Log.d("onSyncStartButtonClick", ex.message!!)
                            } finally {
                                //syncStartButton!!.isEnabled = true
                            }
                        }) {
                        Modifier.weight(1f)

                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync",
                        )
                        Text("Sync", fontSize = 24.sp)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = getString(R.string.syncPlaylistsPrompt), fontSize = 20.sp)
                Row(
                    modifier = Modifier
                        .toggleable(
                            value = isSyncPlaylistsDeleteFiles,
                            enabled = true,
                            role = Role.Checkbox,
                            onValueChange = { isSyncPlaylistsDeleteFiles = !isSyncPlaylistsDeleteFiles}
                        )
                        .padding(start = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncPlaylistsDeleteFiles,
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            checkedColor = Color(getColor(R.color.colorButtonBackground)),
                        ),
                        onCheckedChange = {
                            isSyncPlaylistsDeleteFiles = it
                        }
                    )
                    Text(getString(R.string.syncPlaylistsDeleteUnselected), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp)
                }
                Row(modifier = Modifier.padding(start = 20.dp).fillMaxWidth()) {Text(checkCount, color = Color.Red, fontSize = 16.sp)}
                // Playlistのロード
                if (selectedPlaylists == null) {
                    loadPlaylists(isListChecked, onIsListCheckChange = {isChecked -> isListChecked.add(isChecked)}, checkCount, onCheckCountChange = {checkCount = it})
                } else {
                    showPlaylists(checkCount, onCheckCountChange = {checkCount = it})
                }
                LazyColumn(
                    modifier = Modifier.padding(start = 40.dp).fillMaxWidth()
                ) {
                    selectedPlaylists?.let { it1 ->
                        for(i in 0 until it1.size) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .toggleable(
                                            value = isListChecked[i],
                                            enabled = true,
                                            role = Role.Checkbox,
                                            onValueChange = {
                                                isListChecked[i] = it
                                                showPlaylistsSelectedCount(
                                                    checkCount = checkCount,
                                                    onCheckCountChange = { checkCount = it })
                                            }
                                        )
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    Checkbox(
                                        checked = isListChecked[i],
                                        enabled = true,
                                        colors = CheckboxDefaults.colors(
                                            checkmarkColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                            checkedColor = Color(getColor(R.color.colorButtonBackground)),
                                        ),
                                        onCheckedChange = {
                                            isListChecked[i] = it
                                            showPlaylistsSelectedCount(
                                                checkCount = checkCount,
                                                onCheckCountChange = { checkCount = it })
                                        }
                                    )
                                    Text(it1[i].filename, fontSize = 16.sp)
                                }
                            }
                            item {
                                HorizontalDivider(thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (playlistLoaderThread != null) {
            playlistLoaderThread!!.interrupt()
        }
        super.onDestroy()
    }

    /*
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
     */

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

    private fun loadPlaylists(isListChecked: MutableList<Boolean>, onIsListCheckChange: (Boolean) -> Unit, checkCount: String, onCheckCountChange: (String) -> Unit) {
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
                            //onIsListCheckChange(lookup.containsKey(playlistName))
                            values.add(
                                FileSelectedInfo(
                                    playlistName,
                                    lookup.containsKey(playlistName)
                                )
                            )
                            onIsListCheckChange(lookup.containsKey(playlistName))
                        }
                        selectedPlaylists = values
                        runOnUiThread {
                            if (!playlistLoaderThread!!.isInterrupted) {
                                showPlaylists(checkCount = checkCount, onCheckCountChange = onCheckCountChange)
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

    private fun showPlaylists(checkCount: String, onCheckCountChange: (String) -> Unit) {
        if (mainWindow != null) {
            try {
                //syncNoPlaylistsMessage!!.visibility = View.GONE
                showPlaylistsSelectedCount(checkCount = checkCount, onCheckCountChange = {onCheckCountChange(checkCount)})
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
                            showPlaylistsSelectedCount(checkCount, onCheckCountChange = {onCheckCountChange(info.filename)})
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

    private fun showPlaylistsSelectedCount(checkCount: String, onCheckCountChange: (String) -> Unit) {
        var count = 0
        if (selectedPlaylists != null) {
            for (info in selectedPlaylists!!) {
                if (info.checked) {
                    count++
                }
            }
        }
        when (count) {
            0 -> onCheckCountChange(getString(R.string.syncPlaylists0))
            1 -> onCheckCountChange(getString(R.string.syncPlaylists1))
            else -> onCheckCountChange(
                String.format(getString(R.string.syncPlaylistsN), count))
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