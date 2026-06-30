package kim.tkland.musicbeewifisync

import android.Manifest
import android.R.color.white
import android.app.Application
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CheckboxDefaults.colors
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
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults.verticalArrangement
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
import androidx.compose.ui.res.colorResource
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
import java.io.File
import java.io.InputStreamReader
import java.io.BufferedReader


class DeletePlaylistActivity : WifiSyncStartSyncBaseActivity() {
    private var checkCount = mutableStateOf("")
    private val isListChecked = mutableStateListOf<Boolean>(false)
    private val listFilename = mutableStateListOf<String>("")
    private val listFullPath = mutableStateListOf<String>("")

    lateinit var resultLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val DeletePlaylistActivity.viewModel: WifiSyncViewModel
        get() = this.viewModels<WifiSyncViewModel>().value
    private var syncPreview = false
    private var playlistLoaderThread: Thread? = null

    private var deleteConfirmationDialogShow = mutableStateOf(false)
    var count = mutableIntStateOf(0)
    private var deleteConfirmationDialogMessage: String = ""

    private var isLoading by mutableStateOf(true)
    private var pendingUrisToDelete = mutableListOf<Uri>()

    suspend fun doAsyncWork() {
        val job = CoroutineScope(Dispatchers.Default).launch {
            // バックグラウンドで時間のかかる処理
            playlistLoaderThread?.start()
            Log.d("PlaylistSyncActivity.loadPlaylist", "Thread started")
            playlistLoaderThread?.join()
            Log.d("PlaylistSyncActivity.loadPlaylist", "Thread finished")
        }

        // スレッドをブロックせずにコルーチンの完了を待つ
        job.join()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra("deleteplaylist", false)) {
            syncPreview = true
        }

        //enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        resultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d("DeletePlaylistActivity", "Result OK")
                processNextDeleteChunk()
            } else {
                Log.d("DeletePlaylistActivity", "Result NOT OK or Cancelled")
                pendingUrisToDelete.clear()
                finishDeletion()
            }
        }
        // Playlistのロード
        isListChecked.clear()
        listFilename.clear()
        listFullPath.clear()
        selectedPlaylists = null
        loadPlaylists(
                isListChecked,
                onIsListCheckChange = { isChecked -> isListChecked.add(isChecked) },
                listFilename,
                onListFilenameChange = { newFilename: String -> listFilename.add(newFilename) },
                listFullPath,
                onListFullPathChange = { newFullPath: String -> listFullPath.add(newFullPath) }
            )
        lifecycleScope.launch {
            doAsyncWork()
        }
        setContent {
            CustomView()
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
        var context = LocalContext.current

        // You'll need to observe changes from the ViewModel and update the local state
        LaunchedEffect(showDialogFromViewModel) {
            showProgressDialogShow = showDialogFromViewModel
        }

        isFullSync.value = false
        appBarTitle.value = getString(R.string.title_activity_delete_playlists)
        val buttons = WifiSyncSyncButtons(::onPreviewButtonClick, ::onSyncNowButtonClick)

        super.CustomView()
        Scaffold(
            topBar = {
                MusicBeeWifiSyncTopBar(
                    title = {
                        Box(
                        ) {
                            Text(
                                text = appBarTitle.value,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu...")
                        }
                        DropdownMenu(
                            modifier = Modifier.background(Color(ContextCompat.getColor(context,white))),
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.fillMaxWidth(),
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(resources.getString(R.string.menuWifiFullSync))
                                        }
                                    }
                                },
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        MainActivity::class.java
                                    )
                                    intent.putExtra("fullSync", true)
                                    expanded = false
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        PlaylistSyncActivity::class.java
                                    )
                                    intent.putExtra("playlistSync", true)
                                    expanded = false
                                    context.startActivity(intent)
                                },
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(resources.getString(R.string.menuWifiPlaylistSync))
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menuSyncSettings)) },
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        SettingsActivity::class.java
                                    )
                                    expanded = false
                                    context.startActivity(intent)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menuWifiSyncLog)) },
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        ViewErrorLogActivity::class.java
                                    )
                                    expanded = false
                                    context.startActivity(intent)
                                }
                            )
                            // for Test(delete playlists)
                            DropdownMenuItem(
                                text = { Text(resources.getString(R.string.menudeletePlaylists)) },
                                onClick = {
                                    val intent = Intent(
                                        context,
                                        DeletePlaylistActivity::class.java
                                    )
                                    expanded = false
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                )
            },
            bottomBar = {
                Row( // Or a Compose Row
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(navigationBarPadding)
                ) {
                    Button(modifier = Modifier
                        .weight(1f)
                        .height(80.dp)
                        .border(
                            width = 2.dp, // 枠線の幅
                            color = Color(getColor(white)), // 枠線の色
                        ),
                        enabled = true,
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(getColor(R.color.colorButtonBackground)),
                            contentColor = Color(getColor(R.color.colorButtonTextEnabled))
                        ),
                        onClick = {
                            setContent {
                                DeletePlaylists()
                            }
                        }) {
                        Modifier.weight(1f)

                        //Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Delete", fontSize = 20.sp)
                    }
                }
            }
        )
        { innerPadding ->
            /*
            if (deleteConfirmationDialogShow.value) {
                AlertDialog(
                    onDismissRequest = {
                        deleteConfirmationDialogShow.value = false
                    }, // ダイアログの外側をクリックしたときの処理
                    icon = { // Use the dedicated 'icon' parameter
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_dialog_info),
                            contentDescription = "Info Icon"
                        )  /* Provide a content description)*/
                    },
                    title = { Text(getString(R.string.title_activity_delete_playlists)) },
                    text = { Text(deleteConfirmationDialogMessage) },
                    confirmButton = {
                        Button(
                            onClick = {
                                isListChecked.clear()
                                listFilename.clear()
                                selectedPlaylists = null
                                loadPlaylists(
                                    isListChecked,
                                    onIsListCheckChange = { isChecked -> isListChecked.add(isChecked) },
                                    listFilename,
                                    onListFilenameChange = { newFilename: String -> listFilename.add(newFilename) },
                                    listFullPath,
                                    onListFullPathChange = { newFullPath: String -> listFullPath.add(newFullPath) }
                                )
                                lifecycleScope.launch {
                                    doAsyncWork()
                                }
                                setContent {CustomView()}

                                deleteConfirmationDialogShow.value = false // ダイアログを閉じる
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(getColor(R.color.colorButtonBackground)),
                                contentColor = Color(getColor(R.color.colorButtonTextEnabled)),
                            )
                        ) { /* Handle confirm action */
                            Text(getString(android.R.string.ok)) // Or use a string resource
                        }
                    },
                    dismissButton = null
                )
            }

             */
            Column(
                modifier = Modifier
                    .background(Color(getColor(white)))
                    .padding(innerPadding),
                    //.padding(statusBarPadding),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.Start,
            ) {
                Text(text = getString(R.string.deletePlaylistsPrompt), fontSize = 20.sp)
                Row(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                ) { Text(text = checkCount.value, color = Color.Red, fontSize = 16.sp) }
                showPlaylistsSelectedCount(
                    checkCount = "",
                    onCheckCountChange = { newCheckCountValue: String ->
                        checkCount.value = newCheckCountValue
                    }
                )
                LazyColumn(
                    modifier = Modifier
                        .padding(start = 20.dp)
                        .fillMaxWidth()
                ) {
                    if (selectedPlaylists != null) {
                        if (!selectedPlaylists!!.isNotEmpty() || !isListChecked.isNotEmpty() || !listFilename.isNotEmpty()) {
                            return@LazyColumn
                        }
                        for (i in 0 until selectedPlaylists!!.size) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .toggleable(
                                            value = isListChecked[i],
                                            enabled = true,
                                            role = Role.Checkbox,
                                            onValueChange = { it ->
                                                isListChecked[i] = it
                                                selectedPlaylists!![i].checked = it
                                                showPlaylistsSelectedCount(
                                                    checkCount = "",
                                                    onCheckCountChange = { newCheckCountValue: String ->
                                                        checkCount.value = newCheckCountValue
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
                                            checkmarkColor = Color(getColor(white)),
                                            uncheckedColor = Color(getColor(android.R.color.black)),
                                            checkedColor = Color(getColor(R.color.colorAccent)),
                                        ),
                                        onCheckedChange = {
                                            isListChecked[i] = it
                                            selectedPlaylists!![i].checked = it
                                            showPlaylistsSelectedCount(
                                                "",
                                                { newCheckCountValue: String ->
                                                    checkCount.value =
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

    private fun getVolumeName(context: Context): String {
        return WifiSyncService.getVolumeName(context)
    }

    private fun loadPlaylists(
        isListChecked: MutableList<Boolean>,
        onIsListCheckChange: (Boolean) -> Unit,
        listFilename: MutableList<String>,
        onListFilenameChange: (String) -> Unit,
        listFullPath: MutableList<String>,
        onListFullPathChange: (String) -> Unit
    ) {
        playlistLoaderThread = Thread(Runnable {
            val volumeName = getVolumeName(applicationContext)
            val playListCollection = MediaStore.Audio.Playlists.getContentUri(volumeName)
            var cursor: Cursor?
            try {
                cursor = applicationContext.contentResolver.query(
                    playListCollection,
                    arrayOf(
                        MediaStore.Audio.Playlists._ID,
                        MediaStore.Audio.Playlists.RELATIVE_PATH,
                        MediaStore.Audio.Playlists.DISPLAY_NAME
                    ),
                    null,
                    null,
                    MediaStore.Audio.Playlists.DISPLAY_NAME + " ASC",
                    null
                )
            } catch (e: Exception) {
                Log.d("SQLite Error", e.stackTraceToString())
                return@Runnable
            }
            selectedPlaylists = ArrayList()
            if (cursor != null) {
                try {
                    cursor.moveToFirst()
                    if (cursor.count == 0) {
                        cursor.close()
                        return@Runnable
                    }
                    do {
                        val id = cursor.getLong(0)
                        val relativePath = cursor.getString(1)
                        val displayName = cursor.getString(2)
                        val fullPath = relativePath + displayName
                        
                        selectedPlaylists?.add(
                            FileSelectedInfo(
                                id,
                                displayName,
                                fullPath,
                                false
                            )
                        )
                        onListFilenameChange(displayName)
                        onListFullPathChange(fullPath)
                        onIsListCheckChange(false)
                    } while (cursor.moveToNext())
                    cursor.close()
                    return@Runnable
                } catch (e: Exception) {
                    Log.d("onDeleteAllPlaylistsClick", e.toString())
                    Log.d("onDeleteAllPlaylistsClick", e.stackTraceToString())
                    cursor.close()
                    return@Runnable
                }
            }
        })
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun DeletePlaylists() {
        val context = LocalContext.current
        
        // ローカルステートでローディングを管理
        var localLoading by remember { mutableStateOf(true) }

        if (localLoading) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Box(
                            ) {
                                Text(
                                    text = appBarTitle.value,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colorResource(R.color.colorPrimary),
                            titleContentColor = colorResource(white),
                            navigationIconContentColor = colorResource(white),
                            actionIconContentColor = colorResource(white),
                            scrolledContainerColor = colorResource(white)
                        ),
                    )
                }
            )
            { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.Red,
                        strokeWidth = 4.dp
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.w("DeletePlaylistActivity", "Permission not granted: $permission")
                localLoading = false
                finishDeletion()
                return@LaunchedEffect
            }

            val allUrisToDelete = mutableListOf<Uri>()
            
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val selected = selectedPlaylists ?: return@withContext
                val loopStartIndex = selected.size - 1

                for (i in loopStartIndex downTo 0) {
                    if (selected[i].checked) {
                        try {
                            val id = selected[i].id
                            if (id > 0) {
                                val volumeName = getVolumeName(context)
                                val uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.getContentUri(volumeName), id)
                                
                                val fileName = selected[i].filename
                                if (fileName.endsWith(".m3u", true) || fileName.endsWith(".m3u8", true)) {
                                    allUrisToDelete.addAll(getM3UPlaylistTrackUris(context, uri))
                                } else {
                                    allUrisToDelete.addAll(getPlaylistTrackUris(context, id))
                                }
                                allUrisToDelete.add(uri)
                            }
                        } catch (e: Exception) {
                            Log.e("DeletePlaylistActivity", "Error gathering URIs for index $i", e)
                        }
                    }
                }
            }

            if (allUrisToDelete.isNotEmpty()) {
                // 重複を削除して無効なUri（IDが0のものなど）を除外
                val distinctUris = allUrisToDelete.distinct().filter { !it.toString().endsWith("/") }

                if (distinctUris.isNotEmpty()) {
                    pendingUrisToDelete = distinctUris.toMutableList()
                    processNextDeleteChunk()
                } else {
                    finishDeletion()
                }
            } else {
                Log.d("DeletePlaylistActivity", "No URIs to delete")
                finishDeletion()
            }
        }
    }

    private fun processNextDeleteChunk() {
        if (pendingUrisToDelete.isEmpty()) {
            finishDeletion()
            return
        }

        // Group by volume to avoid SecurityException/IllegalArgumentException for multi-volume requests
        val firstUri = pendingUrisToDelete.first()
        val volumeName = try { 
            MediaStore.getVolumeName(firstUri) 
        } catch (e: Exception) { 
            "external" 
        }
        
        val sameVolumeUris = pendingUrisToDelete.filter { uri ->
            try { MediaStore.getVolumeName(uri) == volumeName } catch (e: Exception) { true } 
        }
        
        val chunkSize = 2000
        val chunk = if (sameVolumeUris.size > chunkSize) {
            sameVolumeUris.take(chunkSize)
        } else {
            sameVolumeUris
        }

        // Remove the processed chunk from the global pending list
        pendingUrisToDelete.removeAll { chunk.contains(it) }

        try {
            Log.d("DeletePlaylistActivity", "Requesting delete for ${chunk.size} URIs on volume $volumeName. Remaining: ${pendingUrisToDelete.size}")
            
            // On Android 11+ createDeleteRequest requires media items (audio, video, images, playlists).
            // Files collection items might throw IllegalArgumentException: All requested items must be Media items
            val mediaUris = chunk.filter { uri ->
                val path = uri.path ?: return@filter false
                path.contains("/audio/") || path.contains("/video/") ||
                path.contains("/images/") || path.contains("/playlists/") ||
                !path.contains("/file/")
            }

            if (mediaUris.isEmpty()) {
                Log.w("DeletePlaylistActivity", "No valid media URIs in chunk")
                processNextDeleteChunk() // Skip this chunk if no valid media URIs
                return
            }

            val pendingIntent = MediaStore.createDeleteRequest(contentResolver, mediaUris)
            val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            resultLauncher.launch(intentSenderRequest)
        } catch (e: Exception) {
            Log.e("DeletePlaylistActivity", "Error creating delete request", e)
            pendingUrisToDelete.clear()
            finishDeletion()
        }
    }

    private fun finishDeletion() {
        isLoading = false
        // 削除成功時にリストをリロード
        isListChecked.clear()
        listFilename.clear()
        listFullPath.clear()
        selectedPlaylists = null
        loadPlaylists(
            isListChecked,
            { isChecked -> isListChecked.add(isChecked) },
            listFilename,
            { newFilename -> listFilename.add(newFilename) },
            listFullPath,
            { newFullPath -> listFullPath.add(newFullPath) }
        )
        lifecycleScope.launch {
            doAsyncWork()
        }
        setContent { CustomView() }
    }

    // トラックのURIをリストアップするだけのヘルパーメソッド
    private fun getM3UPlaylistTrackUris(context: Context, playlistUri: Uri): List<Uri> {
        val uriList = mutableListOf<Uri>()
        try {
            context.contentResolver.openInputStream(playlistUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.forEachLine { line ->
                        if (!line.startsWith("#") && line.isNotBlank()) {
                            // M3Uファイル内のパスから曲のURIを取得
                            // MusicBeeのM3Uは通常絶対パス（/Music/Artist/...）
                            val path = if (line.startsWith("/")) line.substring(1) else line
                            filePathToSongUri(path)?.let { uriList.add(it) }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeletePlaylistActivity", "Error reading M3U from Uri", e)
        }
        return uriList
    }

    private fun getPlaylistTrackUris(context: Context, playlistId: Long): List<Uri> {
        val uriList = mutableListOf<Uri>()
        try {
            val volumeName = getVolumeName(context)
            val uri = MediaStore.Audio.Playlists.Members.getContentUri(volumeName, playlistId)
            contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Playlists.Members.AUDIO_ID),
                null, null, null
            )?.use { cursor ->
                val audioIdIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)
                val collection = MediaStore.Audio.Media.getContentUri(volumeName)
                while (cursor.moveToNext()) {
                    val audioId = cursor.getLong(audioIdIndex)
                    if (audioId > 0) {
                        uriList.add(ContentUris.withAppendedId(collection, audioId))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DeletePlaylistActivity", "Error querying playlist tracks", e)
        }
        return uriList
    }

    fun deleteUri(uriList: List<Uri>) {
        (application as WifiSyncApp).deleteUrisImmediate(uriList)
    }


    fun filePathToSongUri(filePath: String): Uri? {
        val volumeName = getVolumeName(this)
        
        // Use Audio collection instead of Files to ensure createDeleteRequest treats it as a Media item
        val collection = MediaStore.Audio.Media.getContentUri(volumeName)

        val projection = arrayOf(MediaStore.Audio.Media._ID)
        val separatorIndex = filePath.lastIndexOf('/') + 1
        val displayName = filePath.substring(separatorIndex)
        val relativePath = filePath.substring(0, separatorIndex)

        val selection = "UPPER(${MediaStore.Audio.Media.DISPLAY_NAME}) = ? AND UPPER(${MediaStore.Audio.Media.RELATIVE_PATH}) = ?"
        val selectionArgs = arrayOf(displayName.uppercase(), relativePath.uppercase())

        var id: Long = 0
        try {
            contentResolver.query(collection, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                }
            }
        } catch (e: Exception) {
            Log.e("filePathToSongUri", "Error querying Audio collection", e)
        }

        // Fallback to Files collection if not found in Audio, but be aware it might cause issues with createDeleteRequest
        if (id == 0L) {
            val fileCollection = MediaStore.Files.getContentUri(volumeName)
            try {
                contentResolver.query(fileCollection, projection, selection, selectionArgs, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))
                        Log.i("filePathToSongUri", "Found in Files collection instead of Audio: $filePath")
                        return ContentUris.withAppendedId(fileCollection, id)
                    }
                }
            } catch (e: Exception) {
                Log.e("filePathToSongUri", "Error querying Files collection", e)
            }
        }

        Log.i("filePathToSongUri", "filePath: $filePath, id: $id, volume: $volumeName")

        if (id == 0L) return null
        return ContentUris.withAppendedId(collection, id)
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
    }

    override fun onSyncNowButtonClick() {
    }

    private inner class FileSelectedInfo internal constructor(
        val id: Long,
        val filename: String,
        val fullPath: String,
        var checked: Boolean
    )

    companion object {
        @Volatile
        private var selectedPlaylists: ArrayList<FileSelectedInfo>? = null
    }
}