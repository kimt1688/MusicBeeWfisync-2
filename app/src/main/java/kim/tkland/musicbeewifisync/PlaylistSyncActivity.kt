package kim.tkland.musicbeewifisync

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.SocketTimeoutException

class PlaylistSyncActivity : WifiSyncBaseActivity {
    private var checkCount by mutableStateOf("")

    private val isListChecked = mutableStateListOf<Boolean>(false)
    private val listFilename = mutableStateListOf<String>("")
    var showFullScanDialogShow = mutableStateOf(false)

    constructor() : super("") {
    }

    constructor(playlistName: String) : super(playlistName) {
    }

    private var PlaylistSyncActivity.showProgress: Boolean
        get() = viewModel.showDialog.value
        set(value) {}
    private val PlaylistSyncActivity.viewModel: WifiSyncViewModel
        get() = this.viewModels<WifiSyncViewModel>().value

    private var syncPreview = false
    private var playlistLoaderThread: Thread? = null

    var showDialog = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra("playlistSync", false)) {
            syncPreview = true
        }

        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Playlistのロード
            isListChecked.clear()
            listFilename.clear()
            if (selectedPlaylists == null) {
                loadPlaylists(
                    isListChecked,
                    onIsListCheckChange = { isChecked -> isListChecked.add(isChecked) },
                    listFilename,
                    onListFilenameChange = { newFilename: String ->
                        listFilename.add(
                            newFilename
                        )
                    },
                    "",
                    onCheckCountChange = { newCheckCountValue: String ->
                        checkCount =
                            newCheckCountValue // Also update here if showPlaylists modifies it
                    })
                // ここでスレッドを待ちたい！！！
                //playlistLoaderThread?.join()
            } else {
                showPlaylists(
                    isListChecked,
                    onIsListCheckChange = { isChecked -> isListChecked.add(isChecked) },
                    listFilename,
                    onListFilenameChange = { newFilename: String -> listFilename.add(newFilename) },
                    "",
                    onCheckCountChange = { newCheckCountValue: String ->
                        checkCount =
                            newCheckCountValue // Also update here if showPlaylists modifies it
                    })
            }
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
        val isOpenDialog = remember { mutableStateOf(false) }

        val context = LocalContext.current
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
        val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
        var data by remember { mutableStateOf("Loading...") }
        var showFullScanDialogShow by remember { mutableStateOf(false) }
        val showDialogFromViewModel by viewModel.showDialog.collectAsStateWithLifecycle()
        var showProgressDialogShow by remember { mutableStateOf(showDialogFromViewModel) }

        // You'll need to observe changes from the ViewModel and update the local state
        LaunchedEffect(showDialogFromViewModel) {
            showProgressDialogShow = showDialogFromViewModel
        }

        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
                    title = {
                        Box( // Wrap the Text in a Box
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
                                modifier = Modifier.background(Color(getColor(android.R.color.white))),
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
                                            Column(horizontalAlignment = Alignment.Start) {
                                                Text(getString(R.string.menuWifiPlaylistSync))
                                            }
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
                                        showFullScanDialogShow = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(getString(R.string.menuAllPlaylistsDelete)) },
                                    onClick = {
                                        expanded = false
                                        setContent { OnDeleteAllPlaylistsClick() }
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
                Row(
                    // Or a Compose Row
                    modifier = Modifier
                        .padding(navigationBarPadding),
                ) {
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(android.R.color.white)), // 枠線の色
                            ),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            isOpenDialog.value =
                                !setSyncParameters(isListChecked, isSyncPlaylistsDeleteFiles)
                            if (!isOpenDialog.value) {
                                WifiSyncServiceSettings.syncCustomFiles = true

                                WifiSyncService.startSynchronisation(
                                    context,
                                    0,
                                    true,
                                    false
                                )
                            }
                        }
                    ) {
                        Modifier.weight(1f)
                        Text(getString(R.string.syncPreview), fontSize = 24.sp)
                    }
                    if (isOpenDialog.value) {
                        AlertDialog(
                            onDismissRequest = {
                                isOpenDialog.value = false
                            }, // ダイアログの外側をクリックしたときの処理
                            title = { Text(getString(R.string.syncErrorHeader)) },
                            icon = { // Use the dedicated 'icon' parameter
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                                    contentDescription = "Error Icon"
                                )  /* Provide a content description)*/
                            },
                            text = { Text(getString(R.string.errorNoPlaylistsSelected)) },
                            confirmButton = {
                                Button(
                                    colors = ButtonDefaults.buttonColors (
                                        containerColor = Color(getColor(R.color.colorButtonBackground)),
                                        contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                                    ),
                                    onClick = {
                                        isOpenDialog.value = false
                                }) { /* Handle confirm action */
                                    Text(getString(android.R.string.ok)) // Or use a string resource
                                }
                            },
                            dismissButton = null
                        )
                    }
                    Button(
                        modifier = Modifier
                            .weight(0.5f)
                            .height(80.dp)
                            .border(
                                width = 2.dp, // 枠線の幅
                                color = Color(getColor(android.R.color.white)), // 枠線の色
                            ),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            isOpenDialog.value =
                                !setSyncParameters(isListChecked, isSyncPlaylistsDeleteFiles)
                            if (!isOpenDialog.value) {
                                WifiSyncServiceSettings.syncCustomFiles = true

                                WifiSyncService.startSynchronisation(
                                    context,
                                    0,
                                    false,
                                    false
                                )
                            }
                        }
                    ) {
                        Modifier.weight(1f)

                        Icon(
                            imageVector = Icons.Filled.Sync,
                            contentDescription = "Sync",
                        )
                        Text(getString(R.string.syncNow), fontSize = 24.sp)
                    }
                    if (isOpenDialog.value) {
                        AlertDialog(
                            onDismissRequest = {
                                isOpenDialog.value = false
                            }, // ダイアログの外側をクリックしたときの処理
                            icon = { // Use the dedicated 'icon' parameter
                                Icon(
                                    painter = painterResource(id = android.R.drawable.ic_dialog_alert),
                                    contentDescription = "Error Icon"
                                )  /* Provide a content description)*/
                            },
                            title = { Text(getString(R.string.syncErrorHeader)) },
                            text = { Text(getString(R.string.errorNoPlaylistsSelected)) },
                            confirmButton = {
                                Button(onClick = {
                                    isOpenDialog.value = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(getColor(R.color.colorButtonBackground)),
                                    contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                                )) { /* Handle confirm action */
                                    Text(getString(android.R.string.ok)) // Or use a string resource
                                }
                            },
                            dismissButton = null
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (showProgress) {
                CreateProgressDialog(viewModel)
            }
            if (showFullScanDialogShow) {
                AlertDialog(
                    onDismissRequest = {
                        showFullScanDialogShow = false
                    }, // ダイアログの外側をクリックしたときの処理
                    icon = { // Use the dedicated 'icon' parameter
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_dialog_info),
                            contentDescription = "Info Icon"
                        )  /* Provide a content description)*/
                    },
                    title = { Text(getString(R.string.syncConfirmHeader)) },
                    text = { Text(getString(R.string.alertDialogMessage)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                runBlocking {
                                    val deffered = async {
                                        showFullScanDialogShow = false
                                    }
                                    deffered.await()
                                }
                                getMusicFiles()
                                val thread = Thread(getMusicFilesThread)
                                viewModel.setValues(thread, getString(R.string.progressDialogMessage))
                                lifecycleScope.launch {
                                    viewModel.doAsyncWork()
                                }
                                showProgressDialogShow = false // ダイアログを閉じる
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(getColor(R.color.colorButtonBackground)),
                                contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            )
                        ) { /* Handle confirm action */
                            Text(getString(android.R.string.ok)) // Or use a string resource
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                showFullScanDialogShow = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(getColor(R.color.colorButtonBackground)),
                                contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            )
                        ) { /* Handle confirm action */
                            Text(getString(android.R.string.cancel)) // Or use a string resource
                        }
                    }
                )
            }
            Column(
                modifier = Modifier
                    .background(Color(getColor(android.R.color.white)))
                    .padding(innerPadding)
                    .padding(statusBarPadding),
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
                            onValueChange = {
                                isSyncPlaylistsDeleteFiles = it
                                WifiSyncServiceSettings.syncDeleteUnselectedFiles = it
                            }
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
                            checkmarkColor = Color(getColor(android.R.color.white)),
                            uncheckedColor = Color(getColor(android.R.color.black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncPlaylistsDeleteFiles = it
                            WifiSyncServiceSettings.syncDeleteUnselectedFiles = it
                        }
                    )
                    Text(
                        getString(R.string.syncPlaylistsDeleteUnselected),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .fillMaxWidth()
                ) { Text(text = checkCount, color = Color.Red, fontSize = 16.sp) }
                showPlaylistsSelectedCount(
                    checkCount = "",
                    onCheckCountChange = { newCheckCountValue: String ->
                        checkCount = newCheckCountValue
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .padding(start = 40.dp)
                        .fillMaxWidth()
                ) {
                    selectedPlaylists?.let { it1 ->
                        for (i in 0 until it1.size) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .toggleable(
                                            value = isListChecked[i],
                                            enabled = true,
                                            role = Role.Checkbox,
                                            onValueChange = { it ->
                                                isListChecked[i] = it
                                                it1[i].checked = it
                                                showPlaylistsSelectedCount(
                                                    checkCount = "",
                                                    onCheckCountChange = { newCheckCountValue: String ->
                                                        checkCount = newCheckCountValue
                                                    }
                                                )
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
                                            checkmarkColor = Color(getColor(android.R.color.white)),
                                            uncheckedColor = Color(getColor(android.R.color.black)),
                                            checkedColor = Color(getColor(R.color.colorAccent)),
                                        ),
                                        onCheckedChange = {
                                            isListChecked[i] = it
                                            it1[i].checked = it
                                            showPlaylistsSelectedCount(
                                                "",
                                                { newCheckCountValue: String ->
                                                    checkCount =
                                                        newCheckCountValue // Also update here if showPlaylists modifies it
                                                })
                                        }
                                    )
                                    Text(listFilename[i], fontSize = 16.sp)
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

    private fun setSyncParameters(
        isListChecked: MutableList<Boolean>,
        isSyncPlaylistsDeleteFiles: Boolean
    ): Boolean {
        WifiSyncServiceSettings.syncCustomFiles = true
        WifiSyncServiceSettings.syncDeleteUnselectedFiles = isSyncPlaylistsDeleteFiles
        if (selectedPlaylists != null) {
            WifiSyncServiceSettings.syncCustomPlaylistNames.clear()
            for (i in 0 until selectedPlaylists!!.size) {
                if (isListChecked[i]) {
                    WifiSyncServiceSettings.syncCustomPlaylistNames.add(selectedPlaylists!![i].filename)
                }
            }
        }
        if (WifiSyncServiceSettings.syncCustomPlaylistNames.isNotEmpty()) {
            WifiSyncServiceSettings.saveSettings(applicationContext)
            return true
        } else {
            return false
        }
    }

    private fun loadPlaylists(
        isListChecked: MutableList<Boolean>,
        onIsListCheckChange: (Boolean) -> Unit,
        listFilename: MutableList<String>,
        onListFilenameChange: (String) -> Unit,
        checkCount: String,
        onCheckCountChange: (String) -> Unit
    ) {
        playlistLoaderThread = Thread(Runnable {
        //val context = LocalContext.current

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
                    onListFilenameChange(playlistName)
                }
                selectedPlaylists = values
                runOnUiThread {
                    if (!playlistLoaderThread!!.isInterrupted) {
                        showPlaylistsSelectedCount(
                            checkCount = checkCount,
                            onCheckCountChange = onCheckCountChange
                        )
                    }
                }
                return@Runnable
            } catch (ex: InterruptedException) {
                throw ex
            } catch (ex: SocketTimeoutException) {
                //showPlaylistRetrievalError()
                Thread.sleep(2500)
            } catch (ex: Exception) {
                ErrorHandler.logError("loadPlaylists", ex)
                //showPlaylistRetrievalError()
                return@Runnable
            }
        }
//}
      })
        runBlocking {
            val deferredResult = async {
                playlistLoaderThread!!.start()
            }
            deferredResult.await()
        }
    }


    private fun showPlaylists(
        isListChecked: MutableList<Boolean>,
        onIsListCheckChange: (Boolean) -> Unit,
        listFilename: MutableList<String>,
        onListFilenameChange: (String) -> Unit,
        checkCount: String,
        onCheckCountChange: (String) -> Unit
    ) {

        if (mainWindow != null) {
            try {
                if (selectedPlaylists != null) {
                    for (i in 0 until selectedPlaylists!!.size) {
                        onIsListCheckChange(selectedPlaylists!![i].checked)
                        onListFilenameChange(selectedPlaylists!![i].filename)
                    }
                }
                showPlaylistsSelectedCount(
                    checkCount = checkCount,
                    { newCheckCountValue: String ->
                        checkCount
                    })
            } catch (ex: Exception) {
                ErrorHandler.logError("showPlaylists", ex)
            }
        }
    }

    private fun showPlaylistsSelectedCount(
        checkCount: String,
        onCheckCountChange: (String) -> Unit
    ) {  // Also update here if showPlaylists modifies it)
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
                String.format(getString(R.string.syncPlaylistsN), count)
            )
        }
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