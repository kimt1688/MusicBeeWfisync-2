package kim.tkland.musicbeewifisync

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import kotlin.time.Duration.Companion.milliseconds

class PlaylistSyncActivity : WifiSyncStartSyncBaseActivity() {
    private var checkCount by mutableStateOf("")

    private val isListChecked = mutableStateListOf<Boolean>()
    private val listFilename = mutableStateListOf<String>()
    private var PlaylistSyncActivity.showProgress: Boolean
        get() = viewModel.showDialog.value
        set(value) {}
    private val PlaylistSyncActivity.viewModel: WifiSyncViewModel
        get() = this.viewModels<WifiSyncViewModel>().value
    private var syncPreview = false
    private var playlistLoaderThread: Thread? = null
    val isOpenDialog = mutableStateOf(false)
    var isSyncPlaylistsDeleteFiles = mutableStateOf(WifiSyncServiceSettings.syncDeleteUnselectedFiles)

    private var isLoading by mutableStateOf(true)

    @SuppressLint("CoroutineCreationDuringComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra("playlistSync", false)) {
            syncPreview = true
        }

        //enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Playlistのロード
            LaunchedEffect(Unit) {
                if (selectedPlaylists == null || selectedPlaylists!!.isEmpty()) {
                    isLoading = true
                    loadPlaylistsAsync()
                    isLoading = false
                } else {
                    isListChecked.clear()
                    listFilename.clear()
                    selectedPlaylists?.forEach {
                        isListChecked.add(it.checked)
                        listFilename.add(it.filename)
                    }
                    showPlaylistsSelectedCount("", { checkCount = it })
                    isLoading = false
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CustomView()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun CustomView() {
        var showFullScanDialogShow by remember { mutableStateOf(false) }
        val showDialogFromViewModel by viewModel.showDialog.collectAsStateWithLifecycle()
        var showProgressDialogShow by remember { mutableStateOf(showDialogFromViewModel) }
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }

        // You'll need to observe changes from the ViewModel and update the local state
        LaunchedEffect(showDialogFromViewModel) {
            showProgressDialogShow = showDialogFromViewModel
        }

        isFullSync.value = false
        appBarTitle.value = getString(R.string.title_activity_sync_playlists)
        val buttons = WifiSyncSyncButtons(::onPreviewButtonClick, ::onSyncNowButtonClick)

        super.CustomView()
        Scaffold(
            topBar = {
                SyncScreenTopBar(appBarTitle.value,
                    expanded,
                    { newValue -> expanded = newValue},
                    { showFullScanDialog()},
                    //{ showDeleteAllPlaylistsDialog() },
                    isFullSync.value)
            },
            bottomBar = {
                buttons.BottomBarContent()
            }
        )
        { innerPadding ->
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
                            value = isSyncPlaylistsDeleteFiles.value,
                            enabled = true,
                            role = Role.Checkbox,
                            onValueChange = {
                                isSyncPlaylistsDeleteFiles.value = it
                                WifiSyncServiceSettings.syncDeleteUnselectedFiles = it
                            }
                        )
                        .padding(start = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Checkbox(
                        checked = isSyncPlaylistsDeleteFiles.value,
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color(getColor(android.R.color.white)),
                            uncheckedColor = Color(getColor(android.R.color.black)),
                            checkedColor = Color(getColor(R.color.colorAccent)),
                        ),
                        onCheckedChange = {
                            isSyncPlaylistsDeleteFiles.value = it
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

    private suspend fun loadPlaylistsAsync() {
        withContext(Dispatchers.IO) {
            var attempt = 0
            while (attempt < 5) {
                attempt++
                try {
                    Log.d("PlaylistSyncActivity", "Loading playlists, attempt $attempt")
                    
                    // NetworkOnMainThreadExceptionを確実に回避するため、明示的なスレッドを使用
                    var playlistsResult: ArrayList<String>? = null
                    var exception: Exception? = null
                    val thread = Thread {
                        try {
                            playlistsResult = WifiSyncService.musicBeePlaylists
                        } catch (e: Exception) {
                            exception = e
                        }
                    }
                    thread.start()
                    thread.join()

                    if (exception != null) throw exception
                    val playlists = playlistsResult ?: ArrayList()

                    if (playlists.isEmpty()) {
                        Log.d("PlaylistSyncActivity", "Playlists empty, retrying in 2s...")
                        delay(2000.milliseconds)
                        continue
                    }

                    // メインスレッド（UI）の更新
                    withContext(Dispatchers.Main) {
                        isListChecked.clear()
                        listFilename.clear()
                        val values = ArrayList<FileSelectedInfo>()
                        val lookup = CaseInsensitiveMap()
                        for (playlistName in WifiSyncServiceSettings.syncCustomPlaylistNames) {
                            lookup[playlistName] = null
                        }
                        for (playlistName in playlists) {
                            val isChecked = lookup.containsKey(playlistName)
                            values.add(FileSelectedInfo(playlistName, isChecked))
                            isListChecked.add(isChecked)
                            listFilename.add(playlistName)
                        }
                        selectedPlaylists = values
                        showPlaylistsSelectedCount("", { checkCount = it })
                    }
                    Log.d("PlaylistSyncActivity", "Playlists loaded successfully: ${playlists.size} items")
                    return@withContext
                } catch (ex: SocketTimeoutException) {
                    Log.w("PlaylistSyncActivity", "Socket timeout on attempt $attempt, retrying...")
                    delay(2500.milliseconds)
                } catch (ex: Exception) {
                    Log.e("PlaylistSyncActivity", "Error on attempt $attempt", ex)
                    if (attempt < 3) {
                        delay(2000.milliseconds)
                        continue
                    }
                    ErrorHandler.logError("loadPlaylists", ex)
                    return@withContext
                }
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

    override fun onPreviewButtonClick() {
        isOpenDialog.value =
            !setSyncParameters(isListChecked, isSyncPlaylistsDeleteFiles.value)
        if (!isOpenDialog.value) {
            WifiSyncServiceSettings.syncCustomFiles = true

            WifiSyncService.startSynchronisation(
                applicationContext,
                0,
                true,
                false
            )
        }
    }

    override fun onSyncNowButtonClick() {
        isOpenDialog.value =
            !setSyncParameters(isListChecked, isSyncPlaylistsDeleteFiles.value)
        if (!isOpenDialog.value) {
            WifiSyncServiceSettings.syncCustomFiles = true

            WifiSyncService.startSynchronisation(
                applicationContext,
                0,
                false,
                false
            )
        }
    }

    private class FileSelectedInfo(
        val filename: String,
        var checked: Boolean
    )

    companion object {
        @Volatile
        private var selectedPlaylists: ArrayList<FileSelectedInfo>? = null
    }
}
