package kim.tkland.musicbeewifisync

import android.Manifest
import android.R.color.white
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader


class DeletePlaylistActivity : WifiSyncStartSyncBaseActivity() {
    private var sortOrder by mutableStateOf(SortOrder.NAME_ASC)

    enum class SortOrder {
        NAME_ASC, NAME_DESC, PATH_ASC, PATH_DESC
    }

    private var checkCount = mutableStateOf("")
    private val isListChecked = mutableStateListOf<Boolean>()
    private val listFilename = mutableStateListOf<String>()
    private val listFullPath = mutableStateListOf<String>()
    private var selectedPlaylists = mutableStateListOf<FileSelectedInfo>()

    lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val DeletePlaylistActivity.viewModel: WifiSyncViewModel
        get() = this.viewModels<WifiSyncViewModel>().value
    private var syncPreview = false
    private var playlistLoaderThread: Thread? = null

    private var isLoading by mutableStateOf(true)
    private var isDeleting by mutableStateOf(false)
    
    // リスト処理のオーバーヘッドを避けるため、事前にチャンク化されたリストを保持する
    private var pendingChunksToDelete = mutableListOf<List<Uri>>()

    suspend fun doAsyncWork() {
        val job = CoroutineScope(Dispatchers.Default).launch {
            playlistLoaderThread?.start()
            playlistLoaderThread?.join()
        }
        job.join()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra("deleteplaylist", false)) {
            syncPreview = true
        }

        super.onCreate(savedInstanceState)

        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("DeletePlaylistActivity", "Result OK")
                processNextDeleteChunk()
            } else {
                Log.d("DeletePlaylistActivity", "Result NOT OK or Cancelled")
                pendingChunksToDelete.clear()
                finishDeletion()
            }
        }

        loadPlaylistsAsync()
        
        setContent {
            CustomView()
        }
    }

    private fun loadPlaylistsAsync() {
        isLoading = true
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val volumeName = WifiSyncService.getVolumeName(applicationContext)
                val playListCollection = MediaStore.Audio.Playlists.getContentUri(volumeName)
                
                val results = mutableListOf<FileSelectedInfo>()
                
                try {
                    applicationContext.contentResolver.query(
                        playListCollection,
                        arrayOf(
                            MediaStore.Audio.Playlists._ID,
                            MediaStore.Audio.Playlists.RELATIVE_PATH,
                            MediaStore.Audio.Playlists.DISPLAY_NAME
                        ),
                        null,
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            do {
                                val id = cursor.getLong(0)
                                val relativePath = cursor.getString(1) ?: ""
                                val displayName = cursor.getString(2) ?: ""
                                val fullPath = relativePath + displayName
                                
                                results.add(FileSelectedInfo(id, displayName, fullPath, false))
                            } while (cursor.moveToNext())
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DeletePlaylistActivity", "Error loading playlists", e)
                }

                when (sortOrder) {
                    SortOrder.NAME_ASC -> results.sortBy { it.filename.lowercase() }
                    SortOrder.NAME_DESC -> results.sortByDescending { it.filename.lowercase() }
                    SortOrder.PATH_ASC -> results.sortBy { it.fullPath.lowercase() }
                    SortOrder.PATH_DESC -> results.sortByDescending { it.fullPath.lowercase() }
                }

                withContext(Dispatchers.Main) {
                    isListChecked.clear()
                    listFilename.clear()
                    listFullPath.clear()
                    selectedPlaylists.clear()
                    
                    results.forEach { info ->
                        selectedPlaylists.add(info)
                        listFilename.add(info.filename)
                        listFullPath.add(info.fullPath)
                        isListChecked.add(false)
                    }
                    
                    showPlaylistsSelectedCount()
                    isLoading = false
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun CustomView() {
        val showDialogFromViewModel by viewModel.showDialog.collectAsStateWithLifecycle()
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
        var expanded by remember { mutableStateOf(false) }
        val context = LocalContext.current

        isFullSync.value = false
        appBarTitle.value = getString(R.string.title_activity_delete_playlists)

        super.CustomView()
        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
                    title = {
                        Text(
                            text = appBarTitle.value,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        var sortMenuExpanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { sortMenuExpanded = !sortMenuExpanded }) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort...")
                        }
                        DropdownMenu(
                            modifier = Modifier.background(Color(ContextCompat.getColor(context, white))),
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Name (A-Z)") },
                                onClick = {
                                    sortOrder = SortOrder.NAME_ASC
                                    sortMenuExpanded = false
                                    loadPlaylistsAsync()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Name (Z-A)") },
                                onClick = {
                                    sortOrder = SortOrder.NAME_DESC
                                    sortMenuExpanded = false
                                    loadPlaylistsAsync()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Path (A-Z)") },
                                onClick = {
                                    sortOrder = SortOrder.PATH_ASC
                                    sortMenuExpanded = false
                                    loadPlaylistsAsync()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Path (Z-A)") },
                                onClick = {
                                    sortOrder = SortOrder.PATH_DESC
                                    sortMenuExpanded = false
                                    loadPlaylistsAsync()
                                }
                            )
                        }

                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                        }
                        DropdownMenu(
                            modifier = Modifier.background(Color(ContextCompat.getColor(context, white))),
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menuWifiFullSync)) },
                                onClick = {
                                    val intent = Intent(context, MainActivity::class.java).apply { putExtra("fullSync", true) }
                                    expanded = false
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menuWifiPlaylistSync)) },
                                onClick = {
                                    val intent = Intent(context, PlaylistSyncActivity::class.java).apply { putExtra("playlistSync", true) }
                                    expanded = false
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menuSyncSettings)) },
                                onClick = {
                                    expanded = false
                                    context.startActivity(Intent(context, SettingsActivity::class.java))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menuWifiSyncLog)) },
                                onClick = {
                                    expanded = false
                                    context.startActivity(Intent(context, ViewErrorLogActivity::class.java))
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding)
                ) {
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .border(width = 2.dp, color = Color(getColor(white))),
                        enabled = !isDeleting && !isLoading,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = { isDeleting = true }
                    ) {
                        Text("Delete", fontSize = 20.sp)
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else {
                    Column(
                        modifier = Modifier
                            .background(Color(getColor(white)))
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.Start,
                    ) {
                        Text(text = getString(R.string.deletePlaylistsPrompt), fontSize = 20.sp, modifier = Modifier.padding(8.dp))
                        Text(text = checkCount.value, color = Color.Red, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        
                        LazyColumn(
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .fillMaxWidth()
                        ) {
                            items(selectedPlaylists.size) { i ->
                                val info = selectedPlaylists[i]
                                Row(
                                    modifier = Modifier
                                        .toggleable(
                                            value = info.checked,
                                            enabled = true,
                                            role = Role.Checkbox,
                                            onValueChange = { checked ->
                                                selectedPlaylists[i] = info.copy(checked = checked)
                                                isListChecked[i] = checked
                                                showPlaylistsSelectedCount()
                                            }
                                        )
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start,
                                ) {
                                    Checkbox(
                                        checked = info.checked,
                                        onCheckedChange = { checked ->
                                            selectedPlaylists[i] = info.copy(checked = checked)
                                            isListChecked[i] = checked
                                            showPlaylistsSelectedCount()
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkmarkColor = Color(getColor(white)),
                                            uncheckedColor = Color.Black,
                                            checkedColor = Color(getColor(R.color.colorAccent)),
                                        )
                                    )
                                    Text(info.filename, fontSize = 16.sp)
                                }
                                HorizontalDivider(thickness = 1.dp)
                            }
                        }
                    }
                }

                if (isDeleting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {} // クリックイベントを消費して背後に通さない
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .background(Color.White, shape = RectangleShape)
                                .padding(24.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(getColor(R.color.colorAccent)),
                                trackColor = Color(getColor(R.color.colorButtonTextEnabled))
                            )
                            Text(text = "Deleting Playlists...", modifier = Modifier.padding(top = 16.dp), color = Color.Black)
                        }
                    }

                    LaunchedEffect(Unit) {
                        performDeletion(context)
                    }
                }
            }
        }
    }

    private suspend fun performDeletion(context: Context) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            isDeleting = false
            return
        }

        val allUrisToDelete = mutableListOf<Uri>()

        withContext(Dispatchers.IO) {
            val selected = selectedPlaylists
            for (info in selected) {
                if (info.checked) {
                    try {
                        if (info.id > 0) {
                            val volumeName = WifiSyncService.getVolumeName(context)
                            val uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.getContentUri(volumeName), info.id)

                            if (info.filename.endsWith(".m3u", true) || info.filename.endsWith(".m3u8", true)) {
                                allUrisToDelete.addAll(getM3UPlaylistTrackUris(context, uri))
                            } else {
                                allUrisToDelete.addAll(getPlaylistTrackUris(context, info.id))
                            }
                            allUrisToDelete.add(uri)
                        }
                    } catch (e: Exception) {
                        Log.e("DeletePlaylistActivity", "Error gathering URIs", e)
                    }
                }
            }

            // 重複排除と無効なURIの除去
            val distinctUris = allUrisToDelete.distinct().filter { !it.toString().endsWith("/") }
            
            if (distinctUris.isNotEmpty()) {
                // UIスレッドを固まらせないよう、IOスレッドで事前にボリュームごとにチャンク化（最大2000件）する
                val chunks = mutableListOf<List<Uri>>()
                val groupedByVolume = distinctUris.groupBy { uri ->
                    try { MediaStore.getVolumeName(uri) } catch (e: Exception) { "external" }
                }
                
                groupedByVolume.forEach { (_, uris) ->
                    uris.chunked(2000).forEach { rawChunk ->
                        // MediaStore.createDeleteRequest に渡せる有効なメディアアイテムのみにフィルタリング
                        val mediaItems = rawChunk.filter { uri ->
                            val path = uri.path ?: return@filter false
                            path.contains("/audio/") || path.contains("/video/") ||
                            path.contains("/images/") || path.contains("/playlists/") ||
                            !path.contains("/file/")
                        }
                        if (mediaItems.isNotEmpty()) {
                            chunks.add(mediaItems)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    pendingChunksToDelete = chunks.toMutableList()
                    processNextDeleteChunk()
                }
            } else {
                withContext(Dispatchers.Main) {
                    finishDeletion()
                }
            }
        }
    }

    private fun processNextDeleteChunk() {
        if (pendingChunksToDelete.isEmpty()) {
            finishDeletion()
            return
        }

        // 最初のチャンクを取り出す。リスト操作は最小限なのでUIは固まらない。
        val chunk = pendingChunksToDelete.removeAt(0)

        try {
            Log.d("DeletePlaylistActivity", "Requesting delete for ${chunk.size} items. Chunks remaining: ${pendingChunksToDelete.size}")
            val pendingIntent = MediaStore.createDeleteRequest(contentResolver, chunk)
            resultLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        } catch (e: Exception) {
            Log.e("DeletePlaylistActivity", "Error creating delete request", e)
            finishDeletion()
        }
    }

    private fun finishDeletion() {
        isDeleting = false
        loadPlaylistsAsync()
    }

    private fun getM3UPlaylistTrackUris(context: Context, playlistUri: Uri): List<Uri> {
        val uriList = mutableListOf<Uri>()
        try {
            context.contentResolver.openInputStream(playlistUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        if (!line.startsWith("#") && line.isNotBlank()) {
                            val path = if (line.startsWith("/")) line.substring(1) else line
                            filePathToSongUri(path)?.let { uriList.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeletePlaylistActivity", "Error reading M3U", e)
        }
        return uriList
    }

    private fun getPlaylistTrackUris(context: Context, playlistId: Long): List<Uri> {
        val uriList = mutableListOf<Uri>()
        try {
            val volumeName = WifiSyncService.getVolumeName(context)
            val uri = MediaStore.Audio.Playlists.Members.getContentUri(volumeName, playlistId)
            contentResolver.query(uri, arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID), null, null, null)?.use { cursor ->
                val audioIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)
                val collection = MediaStore.Audio.Media.getContentUri(volumeName)
                while (cursor.moveToNext()) {
                    val audioId = cursor.getLong(audioIdIndex)
                    if (audioId > 0) uriList.add(ContentUris.withAppendedId(collection, audioId))
                }
            }
        } catch (e: Exception) {
            Log.e("DeletePlaylistActivity", "Error querying playlist tracks", e)
        }
        return uriList
    }

    fun filePathToSongUri(filePath: String): Uri? {
        val volumeName = WifiSyncService.getVolumeName(this)
        val collection = MediaStore.Audio.Media.getContentUri(volumeName)
        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val separatorIndex = filePath.lastIndexOf('/') + 1
        val displayName = filePath.substring(separatorIndex)
        val relativePath = filePath.substring(0, separatorIndex)

        val selection = "UPPER(${MediaStore.Audio.Media.DISPLAY_NAME}) = ? AND UPPER(${MediaStore.Audio.Media.RELATIVE_PATH}) = ?"
        val selectionArgs = arrayOf(displayName.uppercase(), relativePath.uppercase())

        try {
            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return ContentUris.withAppendedId(collection, cursor.getLong(0))
                }
            }
        } catch (e: Exception) {}

        val fileCollection = MediaStore.Files.getContentUri(volumeName)
        try {
            contentResolver.query(fileCollection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return ContentUris.withAppendedId(fileCollection, cursor.getLong(0))
                }
            }
        } catch (e: Exception) {}

        return null
    }

    private fun showPlaylistsSelectedCount() {
        val count = selectedPlaylists.count { it.checked }
        checkCount.value = when (count) {
            0 -> getString(R.string.syncPlaylists0)
            1 -> getString(R.string.syncPlaylists1)
            else -> String.format(getString(R.string.syncPlaylistsN), count)
        }
    }

    override fun onPreviewButtonClick() {}
    override fun onSyncNowButtonClick() {}

    data class FileSelectedInfo(
        val id: Long,
        val filename: String,
        val fullPath: String,
        val checked: Boolean
    )
}
