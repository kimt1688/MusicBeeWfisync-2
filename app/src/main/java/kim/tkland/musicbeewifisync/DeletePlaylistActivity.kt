package kim.tkland.musicbeewifisync

import android.R.color.white
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.res.painterResource
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


class DeletePlaylistActivity : WifiSyncStartSyncBaseActivity() {
    private var checkCount by mutableStateOf("")
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
            // ここに結果の処理コードを記述
            if (result.resultCode == RESULT_OK) {
                Log.d("DeletePlaylistActivity", "Result OK")
            } else {
                Log.d("DeletePlaylistActivity", "Result NOT OK")
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
                            deletePlaylists()
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
                ) { Text(text = checkCount, color = Color.Red, fontSize = 16.sp) }
                showPlaylistsSelectedCount(
                    checkCount = "",
                    onCheckCountChange = { newCheckCountValue: String ->
                        checkCount = newCheckCountValue
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

     private fun loadPlaylists(
            isListChecked: MutableList<Boolean>,
            onIsListCheckChange: (Boolean) -> Unit,
            listFilename: MutableList<String>,
            onListFilenameChange: (String) -> Unit,
            listFullPath: MutableList<String>,
            onListFullPathChange: (String) -> Unit
    ) {
        playlistLoaderThread = Thread(Runnable {
            val playListCollection = MediaStore.Audio.Playlists.getContentUri(
                MediaStore.getExternalVolumeNames(applicationContext)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
            )
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
                //progressDialog!!.dismiss()
                //interrupt()
                return@Runnable
            }
            selectedPlaylists = ArrayList()
            if (cursor != null) {
                try {
                    cursor.moveToFirst()
                    if (cursor.count == 0) {
                        return@Runnable
                    }
                    do {
                        selectedPlaylists?.add(
                            FileSelectedInfo(
                                cursor.getString(2),
                                false
                            )
                        )
                        onListFilenameChange(cursor.getString(2))
                        onListFullPathChange(cursor.getString(1) + cursor.getString(2))
                        onIsListCheckChange(false)
                    } while (cursor.moveToNext())
                    cursor.close()
                    return@Runnable
                } catch (e: InterruptedException) {
                    Log.d("onDeleteAllPlaylistsClick", e.toString())
                    Log.d("onDeleteAllPlaylistsClick", e.stackTraceToString())
                    return@Runnable
                }
            }
        })
    }

    private fun deletePlaylists() {
        count.intValue = 0
        var deleteTrackCount = 0
        val loopStartIndex = selectedPlaylists!!.size - 1

        for(i in loopStartIndex downTo 0) {
            if (selectedPlaylists!![i].checked) {
                val id = filePathToId(listFullPath[i])
                val uri = filePathToUri(listFullPath[i])

                val file = File(getAbsolutePathFromId(id))
                // Log.d("Delete target Playlist File", file.toString())
                // TODO プレイリストの内容を消す（MediaStoreからのクエリからの何かしらでできたはず ）
                if (file.extension.equals("m3u", false) || file.extension.equals("m3u8", false)) {
                    deleteM3UPlaylistTracks(file)
                } else {
                    deletePlaylistTracks(applicationContext, id)
                }
                val uriList = ArrayList<Uri>()
                uriList.add(uri)
                deleteUri(uriList)

                selectedPlaylists!!.removeAt(i)
                listFilename.removeAt(i)
                listFullPath.removeAt(i)
                isListChecked.removeAt(i)

                setContent { CustomView() }
                //if (file.exists()) {
                //    file.delete()
                //}
                //MediaScannerConnection.scanFile(
                //    applicationContext,
                //    arrayOf(file.path),
                //    null
                //) { path, uri ->}

                count.intValue++
            }
        }
        /*
        deleteConfirmationDialogMessage = when(count.intValue) {
            0 -> getString(R.string.deletePlaylists0)
            1 -> getString(R.string.deletePlaylists1)
            else -> getString(R.string.deletePlaylistsN, count.intValue)
        }
        deleteConfirmationDialogShow.value = true

         */
    }

    fun deleteUri(uriList:List<Uri>) {
        val pendingIntent =
            MediaStore.createDeleteRequest(contentResolver, uriList)

        val intentSenderRequest = IntentSenderRequest.Builder(pendingIntent).build()
        resultLauncher.launch(intentSenderRequest)
    }

    fun deleteM3UPlaylistTracks(playlist: File) {
        val uriList = ArrayList<Uri>()

        playlist.forEachLine {
            if (it.startsWith("#")) {
                return@forEachLine
            }
            val uri = filePathToSongUri(it.substring(1))
            uriList.add(uri)
        }
        deleteUri(uriList)
    }

    fun deletePlaylistTracks(
        context: Context,
        playlistId: Long//,
        //audioId: Long
    ): Int {
        val resolver = context.contentResolver
        var countDel = 0
        val uriList = ArrayList<Uri>()

        try {
            val uri = MediaStore.Audio.Playlists.Members.getContentUri(
                MediaStore.getExternalVolumeNames(context)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1], playlistId)

            val cursor = resolver.query(
                uri,
                arrayOf(
                    MediaStore.Audio.Playlists.Members.AUDIO_ID,
                    MediaStore.Audio.Playlists.Members.PLAY_ORDER,
                    MediaStore.Audio.Playlists.Members.DISPLAY_NAME
                ),
                null,
                null,
                MediaStore.Audio.Playlists.Members.PLAY_ORDER + " ASC",
                null
            )
            if (cursor != null) {
                cursor.moveToFirst()
                if (cursor.count == 0) {
                    return countDel
                }
                do {
                    val deleteUri = ContentUris.withAppendedId(MediaStore.Audio.Media.getContentUri(
                        MediaStore.getExternalVolumeNames(context)
                            .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]), cursor.getLong(0))

                    uriList.add(deleteUri)
                    countDel += 1
               } while (cursor.moveToNext())
               deleteUri(uriList)
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return countDel
    }

    fun getAbsolutePathFromId(ID: Long): String {
        var result: String = ""
        val projection = arrayOf(MediaStore.Audio.Playlists.DATA, MediaStore.Audio.Playlists._ID)
        val cursor = applicationContext.contentResolver.query(
            MediaStore.Audio.Playlists.getContentUri(
                MediaStore.getExternalVolumeNames(applicationContext)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]),
                projection, "${MediaStore.Audio.Playlists._ID} = ?", arrayOf("${ID}"), null)
        if (cursor != null && cursor.moveToFirst()) {
            val cindex = cursor.getColumnIndexOrThrow(projection[0])
            result = cursor.getString(cindex)
            cursor.close()
        }
        return result
    }

    fun filePathToUri(filePath: String): Uri {
        var id: Long = 0
        val cr = applicationContext.contentResolver

        val uri = MediaStore.Audio.Playlists.getContentUri(
            MediaStore.getExternalVolumeNames(applicationContext)
                .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
        )
        val projection =
            arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.DISPLAY_NAME, MediaStore.Audio.Playlists.RELATIVE_PATH)
        val selectionArgs = arrayOf(
            filePath.substring(filePath.lastIndexOf('/') + 1),
            filePath.substring(0, filePath.lastIndexOf('/') + 1),
        )

        Log.d("filePathToUri()", "selectionArgs:${selectionArgs[0]}, ${selectionArgs[1]}")

        val cursor = cr.query(
            uri, projection,
            "${MediaStore.Audio.Playlists.DISPLAY_NAME} = ? and ${MediaStore.Audio.Playlists.RELATIVE_PATH} = ?", selectionArgs, null
        )

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val idIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)
                    id = cursor.getString(idIndex).toLong()
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        val return_uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(
                MediaStore.getExternalVolumeNames(applicationContext)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
            ),
            id
        )
        return return_uri
    }

    fun filePathToSongUri(filePath: String): Uri {
        var id: Long = 0
        val cr = applicationContext.contentResolver

        val uri = MediaStore.Audio.Media.getContentUri(
            MediaStore.getExternalVolumeNames(applicationContext)
                .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
        )
        val projection =
            arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.RELATIVE_PATH)
        val selectionArgs = arrayOf(
            filePath.substring(filePath.lastIndexOf('/') + 1),
            filePath.substring(0, filePath.lastIndexOf('/') + 1),
        )

        Log.d("filePathToUri()", "selectionArgs:${selectionArgs[0]}, ${selectionArgs[1]}")

        val cursor = cr.query(
            uri, projection,
            "${MediaStore.Audio.Media.DISPLAY_NAME} = ? and ${MediaStore.Audio.Media.RELATIVE_PATH} = ?", selectionArgs, null
        )

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val idIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
                    id = cursor.getString(idIndex).toLong()
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        val return_uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(
                MediaStore.getExternalVolumeNames(applicationContext)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
            ),
            id
        )
        return return_uri
    }
    fun filePathToId(filePath: String): Long {
        var id: Long = 0
        val cr = applicationContext.contentResolver

        val uri = MediaStore.Audio.Playlists.getContentUri(
            MediaStore.getExternalVolumeNames(applicationContext)
                .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
        )
        val projection =
            arrayOf(MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.DISPLAY_NAME, MediaStore.Audio.Playlists.RELATIVE_PATH)
        val selectionArgs = arrayOf(
            filePath.substring(filePath.lastIndexOf('/') + 1),
            filePath.substring(0, filePath.lastIndexOf('/') + 1),
        )

        Log.d("filePathToUri()", "selectionArgs:${selectionArgs[0]}, ${selectionArgs[1]}")

        val cursor = cr.query(
            uri, projection,
            "${MediaStore.Audio.Playlists.DISPLAY_NAME} = ? and ${MediaStore.Audio.Playlists.RELATIVE_PATH} = ?", selectionArgs, null
        )

        if (cursor != null) {
            if (cursor.count > 0) {
                cursor.moveToFirst()
                do {
                    val idIndex = cursor.getColumnIndex(MediaStore.Audio.Playlists._ID)
                    id = cursor.getString(idIndex).toLong()
                } while (cursor.moveToNext())
            }
            cursor.close()
        }

        return id
        /*
        val return_uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri(
                MediaStore.getExternalVolumeNames(applicationContext)
                    .toTypedArray()[WifiSyncServiceSettings.deviceStorageIndex - 1]
            ),
            id
        )
        return return_uri

         */
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
        val filename: String,
        var checked: Boolean
    )

    companion object {
        @Volatile
        private var selectedPlaylists: ArrayList<FileSelectedInfo>? = null
    }
}